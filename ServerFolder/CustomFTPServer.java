import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.File;

public class CustomFTPServer {

    private static ServerSocket serverSocket;

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
                new Thread(new ClientHandler(clientConnection, addr)).start();;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    protected void finalize() throws IOException {
        serverSocket.close();
    }
}

class ClientHandler implements Runnable {
        
    private final static String SUCCESS = "200\r\n";
    private final static String FAILURE = "400\r\n";
    private final static int MAX_DATA_PORT = 65535;
    private final static int MIN_DATA_PORT = 1;
    private final static String ROOT_DIRECTORY = "./";

    private String currentDirectory = ROOT_DIRECTORY;
    private Socket dataPort;
    private String addr;
    private int dataPortNo = -1;

    private Socket clientConnection;
    BufferedReader inFromClient;
    DataOutputStream outToClient;
    
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
        inFromClient = null;
        try {
            inFromClient = new BufferedReader(new InputStreamReader((clientConnection.getInputStream())));
        } catch (IOException e) {
            e.printStackTrace();
        }

        outToClient = null;
        try {
            outToClient = new DataOutputStream(clientConnection.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        boolean isConnected = true;
        while(isConnected){
            String requestMessage = null;
            try {
                requestMessage = inFromClient.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //Print Request Msg
            System.out.println(requestMessage);

            String receivedCommand[] = requestMessage.split(" ");
            isConnected = handleRequest(receivedCommand);
        }
    }

    public boolean handleRequest(String[] receivedCommand) {
        
        if (receivedCommand[0].equals("PORT")) {
            boolean isSuccess = true;
            if(dataPortNo < MIN_DATA_PORT || dataPortNo > MAX_DATA_PORT){
                isSuccess = false;
            }else{
                dataPortNo = Integer.parseInt(receivedCommand[1]);
            }
            return sendResponse(isSuccess);
        }
        
        else if (receivedCommand[0].equals("GPRT")) {
            //Check if Port is opened
            boolean isSuccess = false;
            if(dataPortNo != -1){
                // TODO
            }else{
                isSuccess = false;
            }
            return sendResponse(isSuccess);
        }
        
        else if (receivedCommand[0].equals("NLST")) {
            boolean isSuccess = false;
            // TODO
            return sendResponse(isSuccess);
        }
        
        else if (receivedCommand[0].equals("CWD")) {
            String childName = receivedCommand[1];
            //Get Children
            File root = new File(currentDirectory);
            File[] fileList = root.listFiles();
            boolean isSuccess = false;
                               
            for (File file: fileList) {
                if(file.getName().equals(childName) && file.isDirectory()){
                    isSuccess = true;
                    break;
                }
            }
            
            //Change directory if directory is found
            if(isSuccess){
                currentDirectory += childName + "/";
            }

            return sendResponse(isSuccess);
        }
        
        else if (receivedCommand[0].equals("CDUP")) {
            boolean isSuccess = false;
            if (currentDirectory.equals(ROOT_DIRECTORY)) {
                isSuccess = true;
            }else {
                currentDirectory = currentDirectory.substring(0, currentDirectory.length()-1);
                int index = currentDirectory.lastIndexOf("/");
                currentDirectory = currentDirectory.substring(0, index+1);
                isSuccess = true;
            }
            return sendResponse(isSuccess);
        }
        
        else if (receivedCommand[0].equals("PUT")) {
            // TODO
            boolean isSuccess = false;
            return sendResponse(isSuccess);
        }
        
        else if (receivedCommand[0].equals("MKDIR")) {
            boolean isSucccess = (new File(currentDirectory + receivedCommand[1])).mkdirs();
            return sendResponse(isSucccess);
        }
        
        else if (receivedCommand[0].equals("RETR")) {
            // TODO
            boolean isSuccess = false;
            return sendResponse(isSuccess);
        }
        
        else if (receivedCommand[0].equals("DELE")) {
            String fileName = receivedCommand[1];
            File file = new File(currentDirectory + fileName);
            boolean isSuccess = false;
            if(file.exists() && !file.isDirectory()){
                isSuccess = file.delete();
            }
            return sendResponse(isSuccess);
        }
        
        else if (receivedCommand[0].equals("DDIR")) {
            String directoryName = receivedCommand[1];
            File directory = new File(currentDirectory + "/" + directoryName);
            boolean isSuccess = true;
            if (directory.exists() && directory.isDirectory()) {
                try{
                    deleteDirectoryRecursion(directory);
                }catch(IOException e){
                    isSuccess = false;
                    e.printStackTrace();
                }                    
            }
            else {
                isSuccess = false;
            }
            return sendResponse(isSuccess);
        }
        
        else if (receivedCommand[0].equals("QUIT")) {
            try {
                sendResponse(true);
                clientConnection.close();
                if (dataPort != null){
                    dataPort.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return false;
        }
        return false;
    }

    boolean sendResponse(boolean isSuccess){
        String responseMessage = isSuccess ? SUCCESS : FAILURE;
        
        if(responseMessage.equals(FAILURE)){
            try {
                clientConnection.close();
                if (dataPort != null){
                    dataPort.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        try {
            outToClient.writeBytes(responseMessage);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
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