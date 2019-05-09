package ServerFolder;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.File;

public class CustomFTPServer {

    private static ServerSocket serverSocket;
    private static ClientHandler clientHandler;
    private static Thread thread;

    public static void main(String[] args){

        // Check the arguments and read the host & port
        if(args.length != 2){
            System.out.println("Exactly two parameters are needed: <Addr> <ControlPort> ");
            return;
        }

        //Get parameters from arguments
        String addr = args[0];
        int controlPort = Integer.parseInt(args[1]);

        System.out.println("Server is running...");

        //Create Server socket
        serverSocket = null;
        try {
            serverSocket = new ServerSocket(controlPort);
            System.out.println("Listening port " + controlPort + ".");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Server could not be established at port: " + controlPort);
            System.out.println("Closing the CustomFTPServer");
            return;
        }
        
        while(true){
            Socket clientConnection = null;
            try {
                clientConnection = serverSocket.accept();
                clientHandler = new ClientHandler(clientConnection, addr);
                thread = new Thread(clientHandler);
                thread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    protected void finalize() throws IOException {
        serverSocket.close();
    }
    
    static class ClientHandler implements Runnable {
        
        private final static String SUCCESS = "200\r\n";
        private final static String FAILURE = "400\r\n";
        private final static int MAX_DATA_PORT = 65535;
        private final static int MIN_DATA_PORT = 1;
        private final static String ROOT_DIRECTORY = "./";

        private String currentDirectory = ROOT_DIRECTORY;
        private Socket clientConnection;
        private Socket dataPort;
        private String addr;
        private String responseMessage = "";
        private int dataPortNo = -1;
        
        ClientHandler(Socket clientConnection, String addr) {
            this.clientConnection = clientConnection;
            this.addr = addr;
        } 
        
        public void run() {
            // create input buffer and output buffer
            // wait for input from client and send response back to client
            // close all streams and sockets

            System.out.println("New thread started for client...");
            
            //Handle Client Request
            BufferedReader inFromClient = null;
            try {
                inFromClient = new BufferedReader(new InputStreamReader((clientConnection.getInputStream())));
            } catch (IOException e) {
                e.printStackTrace();
            }

            DataOutputStream outToClient = null;
            try {
                outToClient = new DataOutputStream(clientConnection.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            while(true){
                String requestMessage = null;
                try {
                    requestMessage = inFromClient.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
    
                //Print Request Msg
                System.out.println(requestMessage);

                String receivedCommand[] = requestMessage.split(" ");
                handleRequest(receivedCommand, outToClient);
            }
        }

        public void handleRequest(String[] receivedCommand, DataOutputStream outToClient) {
            
            if (receivedCommand[0].equals("PORT")) {
                if(dataPortNo < MIN_DATA_PORT || dataPortNo > MAX_DATA_PORT){
                    responseMessage = FAILURE;
                }else{
                    dataPortNo = Integer.parseInt(receivedCommand[1]);
                    responseMessage = SUCCESS;
                }
                try {
                    outToClient.writeBytes(responseMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            else if (receivedCommand[0].equals("GPRT")) {
                responseMessage = SUCCESS;
                try {
                    // TODO
                    outToClient.writeBytes(responseMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            else if (receivedCommand[0].equals("NLST")) {
                responseMessage = SUCCESS;
                try {
                    // TODO
                    outToClient.writeBytes(responseMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            else if (receivedCommand[0].equals("CWD")) {
                String childName = receivedCommand[1];
                //Get Children
                File root = new File(currentDirectory);
                File[] fileList = root.listFiles();
                boolean isChildFound = false;
                                   
                for (File file: fileList) {
                    if(file.getName().equals(childName) && file.isDirectory()){
                        isChildFound = true;
                        break;
                    }
                }
                //Change directory if directory is found
                if(isChildFound){
                    currentDirectory += childName + "/";
                    responseMessage = SUCCESS;
                }else{
                    responseMessage = FAILURE;
                }

                try {
                    outToClient.writeBytes(responseMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            else if (receivedCommand[0].equals("CDUP")) {
                boolean isCDUP = false;
                if (currentDirectory.equals(ROOT_DIRECTORY)) {
                    responseMessage = FAILURE;
                }else {
                    currentDirectory = currentDirectory.substring(0, currentDirectory.length()-1);
                    int index = currentDirectory.lastIndexOf("/");
                    currentDirectory = currentDirectory.substring(0, index+1);
                    isCDUP = true;
                }

                responseMessage = isCDUP ? SUCCESS : FAILURE;
                try {
                    outToClient.writeBytes(responseMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            else if (receivedCommand[0].equals("PUT")) {
                responseMessage = SUCCESS;
                try {
                    // TODO
                    outToClient.writeBytes(responseMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            else if (receivedCommand[0].equals("MKDIR")) {
                boolean success = (new File(currentDirectory + receivedCommand[1])).mkdirs();
                responseMessage = success ? SUCCESS : FAILURE;
                try {
                    outToClient.writeBytes(responseMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            else if (receivedCommand[0].equals("RETR")) {
                responseMessage = SUCCESS;
                try {
                    outToClient.writeBytes(responseMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            else if (receivedCommand[0].equals("DELE")) {
                String fileName = receivedCommand[1];
                File file = new File(currentDirectory + fileName);
                boolean isDeleted = false;
                if(file.exists() && !file.isDirectory()){
                    isDeleted = file.delete();
                }

                responseMessage = isDeleted ? SUCCESS : FAILURE;
                try {
                    outToClient.writeBytes(responseMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            else if (receivedCommand[0].equals("DDIR")) {
                String directoryName = receivedCommand[1];
                File directory = new File(currentDirectory + "/" + directoryName);
                boolean isDeleted = true;
                if (directory.exists() && directory.isDirectory()) {
                    try{
                        deleteDirectoryRecursion(directory);
                    }catch(IOException e){
                        isDeleted = false;
                        e.printStackTrace();
                    }                    
                }
                else {
                    isDeleted = false;
                }

                responseMessage = isDeleted ? SUCCESS : FAILURE;
                try {
                    outToClient.writeBytes(responseMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            else if (receivedCommand[0].equals("QUIT")) {
                responseMessage = SUCCESS;
                try {
                    outToClient.writeBytes(responseMessage);
                    clientConnection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(responseMessage.equals(FAILURE)){
                try {
                    clientConnection.close();
                    if (dataPort != null){
                        dataPort.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
        }

        void deleteDirectoryRecursion(File file) throws IOException {
            if (file.isDirectory()) {
                File[] entries = file.listFiles();
                if (entries != null) {
                    for (File entry : entries) {
                        deleteDirectoryRecursion(entry);
                    }
                }
            }
            if (!file.delete()) {
                throw new IOException("Failed to delete " + file);
            }
        }
    
    }
}