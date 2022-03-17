package chatapplication;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.Date;


final class ChatServer {
    private static int uniqueId = 0;
    private final List<ClientThread> clients = new ArrayList<>();
    private final ChatFilter chatFilter;
    private static Object lock = new Object();
    private final int port;


    private ChatServer(int port, String badWordsFile) {
        this.port = port;
        this.chatFilter = new ChatFilter(badWordsFile);
    }

    /*
     * This is what starts the ChatServer.
     * Right now it just creates the socketServer and adds a new ClientThread to a list to be handled
     */
    private void start() {
        ServerSocket serverSocket;

        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("Failed to open server");
            e.printStackTrace();
            return;
        }

        System.out.println();

        while(true) {
            try {
                System.out.println(new SimpleDateFormat("HH:mm:ss").format(new Date()) + " Server waiting for clients on port " + port);
                Socket socket = serverSocket.accept();
                Runnable r = new ClientThread(socket, uniqueId++);

                if(((ClientThread) r).isAlive) {
                    clients.add((ClientThread) r);
                    Thread t = new Thread(r);
                    t.start();
                }
                else {
                    ((ClientThread) r).writeMessage("Username taken, Please choose a different username");
                    ((ClientThread) r).close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void broadcast(String message) {
        synchronized(lock) {
            for(ClientThread c : clients) {
                c.writeMessage(message);
            }
        }
    }

    private void remove(int id) {
        synchronized(lock) {
            for (int i = 0; i < clients.size(); i++) {
                if(clients.get(i).id == id) {
                    clients.remove(i);
                }
            }
        }
    }

    private void directMessage(String message, String username, ClientThread ct) {
        synchronized(lock) {
            if (username.equals(ct.username)) {
                System.out.println(new SimpleDateFormat("HH:mm:ss").format(new Date()) + " " + ct.username + " tried to direct message themself");
                ct.writeMessage(new SimpleDateFormat("HH:mm:ss").format(new Date()) + " ERROR you cannot direct message yourself");
            }
            else {
                boolean sent = false;
                for (int i = 0; i < clients.size(); i++) {
                    if(clients.get(i).username.equals(username)) {
                        clients.get(i).writeMessage(message);
                        sent = true;
                        System.out.println(message);
                        ct.writeMessage(message);
                    }
                }
                if (!sent) {
                    System.out.println(new SimpleDateFormat("HH:mm:ss").format(new Date()) + " " + ct.username + " tried to send a direct message to a user that does not exist");
                    ct.writeMessage(new SimpleDateFormat("HH:mm:ss").format(new Date()) + " ERROR sending message to " + username + ", user does not exist");
                }
            }
        }
    }

    private boolean checkUsername(String username) {
        synchronized(lock) {
            for (ClientThread c : clients) {
                if (c.username.equals(username)) {
                    return false;
                }
            }
            return true;
        }
    }

    /*
     *  > java ChatServer
     *  > java ChatServer portNumber
     *  If the port number is not specified 1500 is used
     */
    public static void main(String[] args) {
        int port = 0;
        try {
            port = Integer.parseInt(args[0]);
        }
        catch(Exception e) {
            port = 1500;
        }

        String fileName = "";
        try {
            fileName = args[1];
        }
        catch(ArrayIndexOutOfBoundsException e) {
            // Do Nothing, it'll be caught when the ChatFilter is created
        }

        ChatServer server = new ChatServer(port, fileName);
        server.start();
    }


    /*
     * This is a private class inside of the ChatServer
     * A new thread will be created to run this every time a new client connects.
     */
    private final class ClientThread implements Runnable {
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        int id;
        String username;
        ChatMessage cm;
        boolean isAlive;

        private ClientThread(Socket socket, int id) {
            this.id = id;
            this.socket = socket;
            this.isAlive = true;

            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                username = (String) sInput.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            isAlive = checkUsername(username);

            if(isAlive) {
                System.out.println(new SimpleDateFormat("HH:mm:ss").format(new Date()) + " " + username + " just connected from " + socket.getRemoteSocketAddress().toString());
            }
            else {
                System.out.println(new SimpleDateFormat("HH:mm:ss").format(new Date()) + " " + username + " tried to connect from a username that is already logged in, disconnecting them.");
            }
        }

        /*
         * This is what the client thread actually runs.
         */
        @Override
        public void run() {
            while (isAlive) {
                ChatMessage cm;

                try {
                    cm = (ChatMessage) sInput.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    close();
                    System.out.println(new SimpleDateFormat("HH:mm:ss").format(new Date()) + " User " + username + " disconnected unexpectedly");
                    return;
                }

                if(cm.getType() == 0) {
                    String cleanMessage = chatFilter.filter(cm.getMessage());

                    cleanMessage = new SimpleDateFormat("HH:mm:ss").format(new Date()) + " " + username + ": " + cleanMessage;
                    System.out.println(cleanMessage);

                    broadcast(cleanMessage);
                }
                else if(cm.getType() == 1) {
                    // logout
                    System.out.println(new SimpleDateFormat("HH:mm:ss").format(new Date()) + " " + username + " disconnected with LOGOUT message.");
                    close();
                }
                else if(cm.getType() == 2) {
                    // Private message
                    // Parse out the username
                    try {
                        String[] words = cm.getMessage().split(" ");
                        String sendTo = words[1];
                        String newmsg = "";

                        for(int i=2; i<words.length; i++) {
                            newmsg += words[i] + " ";
                        }

                        // Create the new string and filter out bad words
                        String toSend = new SimpleDateFormat("HH:mm:ss").format(new Date()) + " " + username + " -> " + sendTo + ": " + chatFilter.filter(newmsg);

                        directMessage(toSend, sendTo, this);
                    }
                    catch(ArrayIndexOutOfBoundsException e) {
                        System.out.println(new SimpleDateFormat("HH:mm:ss").format(new Date()) + " malformed /msg command recieved, ignoring it");
                    }
                }
                else if(cm.getType() == 3) {
                    //list
                    String toSend = new SimpleDateFormat("HH:mm:ss").format(new Date()) + " list of users connected right now: ";

                    for(ClientThread c : clients) {
                        if(!c.username.equals(username))
                            toSend += c.username + ", ";
                               }

                    // Trim off the last comma because life is pain
                    toSend = toSend.substring(0, toSend.length()-2);

                    System.out.println(new SimpleDateFormat("HH:mm:ss").format(new Date()) + " User " + username + " sent /list command, response is: " + toSend);
                    writeMessage(toSend);
                }
            }
        }

        private synchronized boolean writeMessage(String message) {
            if(socket.isConnected() && isAlive) {
                try {
                    sOutput.writeObject(new ChatMessage(0, message));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return true;
            }
            else {
                return false;
            }
        }

        private synchronized void close() {
            try {
                socket.close();
            }
            catch (IOException e) {
                e.printStackTrace();
                return;
            }

            remove(id);
            isAlive = false;
        }
    }
}