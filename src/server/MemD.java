package server;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

public class MemD implements Runnable {
    private static Socket socketToClient;
    private static ArrayList<String[]> memberList;
    private static DatagramSocket serverUDPSOcket = null;

    public MemD(Socket socketParam) {
        socketToClient = socketParam;
    }

    public void run() {

        try {
            BufferedReader infromClientBuf = new BufferedReader(new InputStreamReader(socketToClient.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(socketToClient.getOutputStream());


            TCPLoop: while(true) {
                String inFromClient = infromClientBuf.readLine();
                String[] action = inFromClient.split("\\s|\\n");

                switch(action[0]) {
                    case "HELO":
                        String[] newMember = inFromClient.split("^[^\\s]*\\s")[1].split(" ");
                        String acceptMsg = "ACPT ";

                        synchronized (memberList) {
                            Iterator memberItr = memberList.iterator();
                            while (memberItr.hasNext()) {
                                String [] member = (String [])memberItr.next();
                                acceptMsg = acceptMsg + member[0] + " " + member[1] + " " + member[2] + ":";
                                if (member[0].equals(newMember[0])) {
                                    outToClient.writeBytes("RJCT " + newMember[0] + "\n");
                                    break TCPLoop;
                                }
                            }
                            acceptMsg = acceptMsg + newMember[0] + " " + newMember[1] + " " + newMember[2] + "\n";
                            memberList.add(newMember);

                            Iterator newMemberItr = memberList.iterator();
                            while (newMemberItr.hasNext()) {
                                String [] chatter = (String [])newMemberItr.next();
                                InetAddress chatterAddress = InetAddress.getByName(chatter[1]);
                                int chatterPort = Integer.parseInt(chatter[2]);

                                DatagramPacket UDPOutPacket = new DatagramPacket(UDPOut, inputs.length(), chatterAddress,  chatterPort );
                                clientUDPSocket.send(UDPOutPacket);
                            }


                        }

                        String

                        break;
                    case "EXIT":

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
        serverUDPSOcket = new DatagramSocket(); // UDP socket

        while(true) {
            Socket clientConnectionSocket = welcomeSocket.accept();
            Runnable MemDThread = new MemD(clientConnectionSocket);
            new Thread(MemDThread).start();
        }

    }
}
