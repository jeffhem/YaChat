package server;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

public class MemD implements Runnable {
    private static ArrayList<String[]> memberList = new ArrayList<>();
    Socket socketToClient;

    public MemD(Socket socketParam) {
        socketToClient = socketParam;
    }

    public void broadcastUDP(ArrayList memembers, String msg) {

        try {
            DatagramSocket serverUDPSOcket = new DatagramSocket();
            Iterator newMemberItr = memembers.iterator();

            while (newMemberItr.hasNext()) {
                String [] member = (String [])newMemberItr.next();
                InetAddress chatterAddress = InetAddress.getByName(member[1]);
                int chatterPort = Integer.parseInt(member[2]);
                byte [] UDPOut = new byte [1024];
                UDPOut = msg.getBytes();

                DatagramPacket UDPOutPacket = new DatagramPacket(UDPOut, msg.length(), chatterAddress,  chatterPort );
                serverUDPSOcket.send(UDPOutPacket);
            }

        } catch (IOException e){
            System.out.println(e);
        }
    }

    public void run() {

        try {
            // DatagramSocket serverUDPSOcket = new DatagramSocket();
            BufferedReader infromClientBuf = new BufferedReader(new InputStreamReader(socketToClient.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(socketToClient.getOutputStream());
            String[] newMember = null;

            TCPLoop: while(true) {
                String inFromClient = infromClientBuf.readLine();
                String[] action = inFromClient.split("\\s|\\n");

                switch(action[0]) {
                    case "HELO":
                        newMember = inFromClient.split("^[^\\s]*\\s")[1].split(" ");
                        String acceptMsg = "ACPT ";
                        String joinMsg = "JOIN ";

                        synchronized (memberList) {
                            Iterator memberItr = memberList.iterator();
                            while (memberItr.hasNext()) {
                                String [] member = (String [])memberItr.next();
                                acceptMsg = acceptMsg + member[0] + " " + member[1] + " " + member[2] + ":";

                                // reject incoming user if the name in use
                                if (member[0].equals(newMember[0])) {
                                    outToClient.writeBytes("RJCT " + newMember[0] + "\n");
                                    outToClient.flush();
                                    break TCPLoop;
                                }
                            }

                            acceptMsg = acceptMsg + newMember[0] + " " + newMember[1] + " " + newMember[2] + "\n";
                            outToClient.writeBytes(acceptMsg);
                            outToClient.flush();

                            memberList.add(newMember);
                            joinMsg = joinMsg + newMember[0] + " " + newMember[1] + " " + newMember[2] + "\n";

                            broadcastUDP(memberList, joinMsg);

//                            Iterator newMemberItr = memberList.iterator();
//                            while (newMemberItr.hasNext()) {
//                                String [] member = (String [])newMemberItr.next();
//                                InetAddress chatterAddress = InetAddress.getByName(member[1]);
//                                int chatterPort = Integer.parseInt(member[2]);
//
//                                byte [] UDPOut = new byte [1024];
//                                String joinMsg = "JOIN " + newMember[0] + " " + newMember[1] + " " + newMember[2] + "\n";
//                                UDPOut = joinMsg.getBytes();
//
//                                DatagramPacket UDPOutPacket = new DatagramPacket(UDPOut, joinMsg.length(), chatterAddress,  chatterPort );
//                                serverUDPSOcket.send(UDPOutPacket);
//                            }

                        }

                        break;
                    case "EXIT":
                        String exitMsg = "EXIT " + newMember[0] + "\n";

                        synchronized (memberList) {
                            broadcastUDP(memberList, exitMsg);
                            memberList.remove(newMember);

//                        Iterator newMemberItr = memberList.iterator();
//                        while (newMemberItr.hasNext()) {
//                            String [] member = (String [])newMemberItr.next();
//                            InetAddress chatterAddress = InetAddress.getByName(member[1]);
//                            int chatterPort = Integer.parseInt(member[2]);
//
//                            byte [] UDPOut = new byte [1024];
//                            String joinMsg = "EXIT " + newMember[0] + "\n";
//                            UDPOut = joinMsg.getBytes();
//
//                            DatagramPacket UDPOutPacket = new DatagramPacket(UDPOut, joinMsg.length(), chatterAddress,  chatterPort );
//                            serverUDPSOcket.send(UDPOutPacket);
//                        }
                        }

                        break TCPLoop;
                }
            }


        } catch (IOException e){
            System.out.println(e);
        }
    }

    public static void main(String [] arg) throws IOException {

        if (arg.length != 1) {
            System.out.println("Usage: java MemD <server port>");
            System.exit(-1);
        }

        int serverPort = java.lang.Integer.parseInt(arg[0]);
        ServerSocket welcomeSocket = new ServerSocket(serverPort);

        while(true) {
            Socket clientConnectionSocket = welcomeSocket.accept();
            Runnable MemDThread = new MemD(clientConnectionSocket);
            new Thread(MemDThread).start();
        }

    }
}
