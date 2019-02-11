package client;
import com.sun.deploy.util.ArrayUtil;

import java.net.*;
import java.io.*;
import java.lang.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class yaChatClient implements Runnable {

    private static String userName = null;
    private static List<String[]> chatters;
    private static DatagramSocket clientUDPSocket = null;

    public static void main(String [] arg) throws IOException {
        if(arg.length != 3) {
            System.out.println("3 arguments are required");
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
        PrintWriter TCPOut = new PrintWriter(clientTCPSocket.getOutputStream(), true);

        TCPOut.println("HELO " + userName + " " + ClientHostAddress + " " + UDPPort);
        System.out.println("My port is: " + UDPPort);

//        DataOutputStream outToServer = new DataOutputStream(clientTCPSocket.getOutputStream());
//        outToServer.writeBytes("HELO " + userName + " " + ClientHostAddress + " " + udpPort + '\n');

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
                break;

            case "RJCT":
                System.out.println("Screen name already exists: " + userName);
                clientTCPSocket.close();
                return;
        }

        /*
         * Start UDP connection
         */

        // strat a thread to read incoming UDP message
        Thread UDPIn = new Thread(new yaChatClient());
        UDPIn.start();

        // reading user input and send mesg
        String inputLine;
        byte [] UDPOut = new byte [1024];
        while((inputLine = userInput.readLine()) != null) {

            String inputs = "MESG Ann: " + inputLine + '\n';
            UDPOut = inputs.getBytes();

            Iterator chattersItr = chatters.iterator();
            while (chattersItr.hasNext()) {
                String [] chatter = (String [])chattersItr.next();
                InetAddress chatterAddress = InetAddress.getByName(chatter[1]);
                int chatterPort = Integer.parseInt(chatter[2]);

                DatagramPacket UDPOutPacket = new DatagramPacket(UDPOut, inputs.length(), chatterAddress,  chatterPort );
                clientUDPSocket.send(UDPOutPacket);
            }

            System.out.print(userName + ": ");
        }

        // exit the chat
        TCPOut.println("EXIT");
        clientTCPSocket.close();
        System.out.println("Good Bye!");
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
                        System.out.println(inMesg[0] + ":" + inMesg[1]);
                        break;

                    case "JOIN":
                        if (!inChatter[0].equals(userName)) chatters.add(inChatter);
//                        for (String [] chatter : chatters) {
//                            System.out.println(Arrays.deepToString(chatter));
//                        }
                        break;

                    case "EXIT":
                        if (!inChatter[0].equals(userName)) {
                            Iterator itr = chatters.iterator();
                            while(itr.hasNext()) {
                                String [] nextUser = (String []) itr.next();
                                if (nextUser[0].equals(inChatter[0])) itr.remove();
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

    public List getAcceptedList(String userName) {

        List <String[]> processedList = new ArrayList<String[]>();
        String [] acceptedList = getContentByColonNSpace();

        acceptedList = Arrays.stream(acceptedList).filter(user -> !user.split(" ")[0].equals(userName)).toArray(String[]::new);

        for (String acceptedUser : acceptedList) {
            processedList.add(acceptedUser.split(" "));
        }

        return processedList;
    }
}
