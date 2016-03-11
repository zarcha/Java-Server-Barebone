package com.altimit_server; /**
 * Created by Zach on 10/19/15.
 */

import com.altimit_server.util.AltimitConverter;
import javassist.bytecode.ByteArray;

import java.io.*;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.*;

public class main {

    private static HashSet<ObjectOutputStream> writers = new HashSet<ObjectOutputStream>(); //this is the list of streams the server uses to write to the clients

    //client map
    public static Map<UUID, DataOutputStream> userMap = new HashMap<UUID, DataOutputStream>();

    public static void main(String[] args) throws Exception{
        //Start server
        StartServer(1024);
    }

    public static void StartServer(int port){
        //lets just do something fancy to show its ready
        System.out.println("==================================== \n" +
                            "========    ALTIMIT SERVER  ======== \n" +
                            "==================================== \n");

        ServerSocket listener = null;

        //Compile a list of the methods that will be used when compiling
        AltimitMethod.AltimitMethodCompile();

        try {
            //Start listening for clients
            listener = new ServerSocket(port);

            //Let the admin know that we can accept users now
            System.out.println("Ready for clients...");

            while (true){
                //once a client connects do things with them in their own thread
                new Handler(listener.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                //this is used to shut off the server when its not accepting anymore clients
                listener.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }


    private static class Handler extends Thread {
        private Socket socket;
        private DataInputStream in;
        private DataOutputStream out;
        private static Commander commander = new Commander();

        //This is the key needed for a message to be valid
        byte[] key = {5, 9, 0, 4};

        //message to be converted
        int messageSize = 0;
        int messageOffset;
        byte[] currentMessage;

        //message that can be old and new
        int fullMessageSize;
        byte[] fullMessage = new byte[0];

        //This is to seee if the client has registered its UUID with the server
        public static boolean uuidSet = false;

        public  Handler(Socket socket){
            this.socket = socket;
        }

        public void run() {
            System.out.println("Client has connected from " + socket.getLocalSocketAddress().toString());

            try {
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());

                while (true) {
                    if (socket.isConnected()) { //we want to make sure we arent dealing with a client thats not there
                        Integer bufSize = in.available();
                        if (bufSize != 0 || fullMessage.length != 0) {

                            //Lets see if this is an old message, new message, and if it needs to combine with an old message
                            if (fullMessage.length != 0 && bufSize != 0) {
                                fullMessage = Arrays.copyOf(fullMessage, fullMessage.length + bufSize);
                                byte[] newMessage = new byte[bufSize];
                                in.read(newMessage, 0, bufSize);
                                System.arraycopy(newMessage, 0, fullMessage, fullMessageSize, newMessage.length);
                                messageSize = 0;
                            } else if (bufSize != 0) {
                                fullMessage = new byte[bufSize];
                                in.read(fullMessage, 0, bufSize);
                                messageSize = 0;
                            } else if (fullMessage.length != 0) {
                                messageSize = 0;
                            }

                            //If the message is not empty now then lets continue
                            if (messageSize == 0) {
                                //Lets get the size of the next message
                                messageSize= ByteBuffer.wrap(fullMessage, 0, 4).getInt();

                                //Lets make sure this message is long enough to even check for a key
                                if(messageSize <= fullMessage.length) {

                                    //Get the last 4 bytes (should be the key)
                                    byte[] messageKey = Arrays.copyOfRange(fullMessage, messageSize - 4, messageSize);

                                    //Make sure the full message is here
                                    if (Arrays.equals(messageKey, key)) {
                                        currentMessage = new byte[messageSize - 8];
                                        messageOffset = 4;
                                        System.arraycopy(fullMessage, messageOffset, currentMessage, 0, currentMessage.length);
                                        messageOffset = messageSize;

                                        //Lets get the convert the byte array message to a list of real variables
                                        List<Object> sentMessage = AltimitConverter.ReceiveConversion(currentMessage);

                                        //Make sure this client has an identifier and if it doesnt then see if it is trying to set it. If not then do nothing!
                                        if (uuidSet) {
                                            //Lets make a new thread and have it do what is needs to do with the message
                                            Thread t = new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    InvokeMessage(sentMessage);
                                                }
                                            });
                                            t.start();
                                        } else if (sentMessage.get(0).equals("SetClientUUID")){
                                            SetClientUUID((String)sentMessage.get(1));
                                        } else {
                                            System.out.println("UUID has not been set...");
                                        }

                                        //if there is more of a message then delete the stuff we just used and if not then reset data
                                        if (messageOffset != fullMessage.length) {
                                            fullMessage = Arrays.copyOfRange(fullMessage, messageOffset, fullMessage.length);
                                            fullMessageSize = fullMessage.length;
                                        } else {
                                            fullMessage = new byte[0];
                                            bufSize = 0;
                                        }
                                    } else {
                                        System.out.println("Key was not found. Message will try to be completed in next read!");
                                    }
                                }
                            }
                        }
                    } else {
                        break;
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                //If they crashed the party then kick them out
                DisconnectUser(socket);
            }
        }


        //this is used to set the UUID of the client so we can identify it and send its socket messages or delete it
        public void SetClientUUID(String sentUUID){
            UUID uuid = UUID.fromString(sentUUID);

            if(userMap.containsKey(uuid)){
                System.out.println("UUID has already been registed! Dropping client!...");
                DisconnectUser(socket);
            } else {
                uuidSet = true;
                userMap.put(UUID.fromString(sentUUID), out);
                System.out.println("Clients UUID has been set...");
            }
        }

        //This will call start calling the method its meant to call on the server
        public void InvokeMessage(List<Object> sentMessage){
            String methodName = (String)sentMessage.get(0);
            sentMessage.remove(0);

            AltimitMethod.CallAltimitMethod(methodName, sentMessage.toArray());
        }

        //If the client is not needed anymore this is used to diconnect the client
        public void DisconnectUser(Socket soc){
            if(out != null){
                writers.remove(out);
            }
            try {
                socket.close();
                System.out.println("User has been disconnected!");
            }catch (IOException e){
                System.out.println("Error: " + e.toString());
            }
        }
    }
}
