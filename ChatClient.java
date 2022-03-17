package chatapplication;

import java.io.*;
import java.util.*;
import java.net.*;

final class ChatClient {
    private ObjectInputStream sInput;
    private ObjectOutputStream sOutput;
    private Socket socket;

    private final String server;
    private final String username;
    private final int port;

    public static String msg;
    private static Scanner s = new Scanner(System.in);

    private ChatClient(String server, int port, String username) {
        this.server = server;
        this.port = port;
        this.username = username;
    }

    /*
     * This starts the Chat Client
     */
    private boolean start() {

        try {
            socket = new Socket(server, port);
        } catch (IOException e) {
            System.out.println("Could not open connection to server");
            e.printStackTrace();
            return false;
        }

        // Create your input and output streams
        try {
            sInput = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.out.println("Failed to open output streams");
            return false;
        }

        // This thread will listen from the server for incoming messages
        Runnable r = new ListenFromServer();
        Thread t = new Thread(r);

        t.start();

        // After starting, send the clients username to the server.
        try {
            sOutput.writeObject(username);
        } catch (IOException e) {
            System.out.println("Failed to send username");
            return false;
        }

        return true;
    }


    /*
     * This method is used to send a ChatMessage Objects to the server
     */
    private void sendMessage(ChatMessage msg) {
        try {
            sOutput.writeObject(msg);
        } catch (IOException e) {
            System.out.println("Server has closed the connection");
            System.exit(0);
        }
    }


    /*
     * To start the Client use one of the following command
     * > java ChatClient
     * > java ChatClient username
     * > java ChatClient username portNumber
     * > java ChatClient username portNumber serverAddress
     *
     * If the portNumber is not specified 1500 should be used
     * If the serverAddress is not specified "localHost" should be used
     * If the username is not specified "Anonymous" should be used
     */
    public static void main(String[] args) {
        // Get proper arguments and override defaults
        String username = "";
        int port = 0;
        String address = "";

        try {
            username = args[0];
        }
        catch(Exception e) {
            username = "Anonymous";
        }

        try {
            port = Integer.parseInt(args[1]);
        }
        catch(Exception e) {
            port = 1500;
        }

        try {
            address = args[2];
        }
        catch(Exception e) {
            address = "localhost";
        }

        // Create your client and start it
        ChatClient client = new ChatClient(address, port, username);


        if(client.start()) {
            while(true) {
                msg = s.nextLine();

                int type = 0;

                if(msg.indexOf("/logout") == 0) {
                    type = 1;
                }
                else if(msg.indexOf("/msg") == 0) {
                    type = 2;
                }
                else if(msg.indexOf("/list") == 0) {
                    type = 3;
                }

                client.sendMessage(new ChatMessage(type, msg));
            }
        }
        else {
            System.out.println("Failed to start, shutting down");
            s.close();
        }
    }


    /*
     * This is a private class inside of the ChatClient
     * It will be responsible for listening for messages from the ChatServer.
     * ie: When other clients send messages, the server will relay it to the client.
     */
    private final class ListenFromServer implements Runnable {
        public void run() {
            System.out.println("Connection accepted " + socket.getRemoteSocketAddress().toString());

            while (true) {
                try {
                    System.out.println("> " + ((ChatMessage) sInput.readObject()).getMessage());
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("Server has closed the connection");
                    // So i'm not positive if system.exit is allowed because @1623 says it is in the follow up, but says no elsewhere
                    // I'm no sure how to stop the scanner from reading input besides system.exit, so I'm going to use that
                    System.exit(0);
                }
            }
        }
    }
}