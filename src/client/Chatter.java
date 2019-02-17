package client;

import java.net.*;
import java.io.*;
import java.lang.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class Chatter implements Runnable {

    private static String userName = null;
    private static ArrayList<String[]> chatters;
    private static DatagramSocket clientUDPSocket = null;

    public static void main(String [] arg) throws IOException {
        if(arg.length != 3) {
            System.out.println("Usage: java Chatter <screen Name> <Membership_server_addr> <Membership_server_tcp_port>");
            return;
        }

        // assign variables with arg
        userName = arg[0];
        String serverHostName = arg[1];
        int TCPPort = Integer.parseInt(arg[2]);

        Socket clientTCPSocket = null;
        BufferedReader userInput = null;

        // getting local host address
        String ClientHostAddress = InetAddress.getLocalHost().getHostAddress();

        try {
            clientTCPSocket = new Socket(serverHostName, TCPPort); // TCP socket
            clientUDPSocket = new DatagramSocket(); // UDP socket
            userInput = new BufferedReader(new InputStreamReader(System.in)); //user inputs
        } catch (Exception e ) {
            System.out.println(e);
        }

        /*
         * Start TCP connection
         */

        // send HELO message
        int UDPPort = clientUDPSocket.getLocalPort();
        System.out.println("====================================");
        System.out.println("My port is: " + UDPPort);

        // not using writeBytes, so the message would not break into packets
        DataOutputStream outToServer = new DataOutputStream(clientTCPSocket.getOutputStream());
        outToServer.writeBytes("HELO " + userName + " " + ClientHostAddress + " " + UDPPort + '\n');
        outToServer.flush();

        // check if it connects successfully
        BufferedReader TPCIn = new BufferedReader((new InputStreamReader(clientTCPSocket.getInputStream())));

        Message TCPConnectionRes = new Message(TPCIn.readLine());

        switch (TCPConnectionRes.getType()) {
            case "ACPT":
                chatters = TCPConnectionRes.getAcceptedList(userName);
                for(String[] chatter : chatters) {
                    System.out.println(chatter[0] + " is in the chatroom");
                }
                System.out.println(userName + " accepted to the chatroom");
                System.out.println("====================================");
                System.out.print(userName + ": ");
                break;

            case "RJCT":
                System.out.println("Screen name already exists: " + userName);
                System.out.println("====================================");
                clientTCPSocket.close();
                return;
        }

        /*
         * Start UDP connection
         */

        // strat a thread to read incoming UDP message
        Thread UDPIn = new Thread(new Chatter());
        UDPIn.start();

        // reading user input and send mesg
        String inputLine;
        byte [] UDPOut = new byte [1024];
        while((inputLine = userInput.readLine()) != null) {
            String inputs = "MESG " + userName + ": " + inputLine + '\n';
            UDPOut = inputs.getBytes();

            synchronized (chatters) {
                Iterator chattersItr = chatters.iterator();
                while (chattersItr.hasNext()) {
                    String [] chatter = (String [])chattersItr.next();
                    InetAddress chatterAddress = InetAddress.getByName(chatter[1]);
                    int chatterPort = Integer.parseInt(chatter[2]);

                    DatagramPacket UDPOutPacket = new DatagramPacket(UDPOut, inputs.length(), chatterAddress,  chatterPort );
                    clientUDPSocket.send(UDPOutPacket);
                }
            }

            System.out.print(userName + ": ");
        }

        // exit the chat
        userInput.close();
        outToServer.writeBytes("EXIT \n");
        outToServer.flush();
        clientTCPSocket.close();
        System.out.println("\nGood Bye!");
        return;
    }

    public void run() {
        /*
         * thread reading UDP socket
         */
        try {
            byte [] UDPIn = new byte [1024];
            DatagramPacket UDPInPacket = new DatagramPacket(UDPIn, UDPIn.length);
            UDPLoop: while (true) {
                clientUDPSocket.receive(UDPInPacket);
                String UDPInPacketString = new String(UDPInPacket.getData());
                Message UDPInMsg = new Message(UDPInPacketString);
                String [] inChatter = UDPInMsg.getContentBySpace();
                String [] inMesg = UDPInMsg.getContentByColonNSpace();

                switch (UDPInMsg.getType()) {
                    case "MESG":
                        System.out.println();
                        System.out.println(inMesg[0] + ":" + inMesg[1]);
                        break;

                    case "JOIN":
                        if (!inChatter[0].equals(userName)) {
                            synchronized (chatters) {
                                chatters.add(inChatter);
                                System.out.println();
                                System.out.println("====================================");
                                System.out.println(inChatter[0] + " has joined the chatroom");
                                System.out.println("====================================");
                            }
                        }
                        break;

                    case "EXIT":
                        if (!inChatter[0].equals(userName)) {
                            synchronized (chatters) {
                                chatters.removeIf(chatter -> (chatter[0].equals(inChatter[0])));
                                System.out.println();
                                System.out.println("====================================");
                                System.out.println(inChatter[0] + " has left the chatroom");
                                System.out.println("====================================");
                            }
                            break;
                        }
                        clientUDPSocket.close();
                        break UDPLoop;
                }
            }
        }catch (IOException e) {
            System.out.println(e);
        }
    }
}

class Message {
    String msg;

    public Message(String msg) {
        this.msg = msg.split("\n")[0];
    }

    public String getType() {
        return this.msg.split(" ")[0];
    }

    public String [] getContentBySpace() {
        return this.msg.split("^[^\\s]*\\s")[1].split(" ");
    }

    public String [] getContentByColonNSpace() {
        String [] content = this.msg.split("^[^\\s]*\\s|:");
        return Arrays.stream(content).filter(data -> data.length() > 0).toArray(String[]::new);
    }

    public ArrayList getAcceptedList(String userName) {

        ArrayList <String[]> processedList = new ArrayList<String[]>();
        String [] acceptedList = getContentByColonNSpace();

        acceptedList = Arrays.stream(acceptedList).filter(user -> !user.split(" ")[0].equals(userName)).toArray(String[]::new);

        for (String acceptedUser : acceptedList) {
            processedList.add(acceptedUser.split(" "));
        }

        return processedList;
    }
}

