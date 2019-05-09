import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;

public class CustomFTPServer {

    private final static int MAX_DATA_PORT = 65535;
    private final static int MIN_DATA_PORT = 1;
    private static String serverDataPort;
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

        serverDataPort = (int) (Math.random()*(MAX_DATA_PORT-1)) + MIN_DATA_PORT + "";
        System.out.println("Server is running...");
        System.out.println("Server data port is chosen as: " + serverDataPort);

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
                new Thread(new ClientHandler(clientConnection, addr, serverDataPort)).start();;
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

    private String currentDirectory;
    private String addr;
    private int clientDataPort;
    private String serverDataPort;

    private Socket clientConnection;
    private Socket dataConnection;

    BufferedReader inFromClient;
    DataOutputStream outToClient;
    
    ClientHandler(Socket clientConnection, String addr, String serverDataPort) {
        this.clientConnection = clientConnection;
        this.addr = addr;
        this.serverDataPort = serverDataPort;

        this.clientDataPort = -1;
        this.currentDirectory = "./";
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
                if(requestMessage != null) {
                    System.out.println(requestMessage);

                    String receivedCommand[] = requestMessage.split(" ");
                    isConnected = handleRequest(receivedCommand);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("Client is closed...");
        return;
    }

    public boolean handleRequest(String[] receivedCommand) {
        
        if (receivedCommand[0].equals("PORT")) {
            boolean isSuccess = true;
            int port = Integer.parseInt(receivedCommand[1]);
            if(port < MIN_DATA_PORT || port > MAX_DATA_PORT){
                isSuccess = false;
            }else{
                clientDataPort = port;
            }
            return sendResponse(isSuccess);
        }
        
        else if (receivedCommand[0].equals("GPRT")) {
            byte data[] = new byte[0];
            try {
                data = serverDataPort.getBytes("US-ASCII");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return sendData(data);
        }
        
        else if (receivedCommand[0].equals("NLST")) {
            File root = new File(currentDirectory);
            File[] fileList = root.listFiles();
            String dataString = "";
            for (File file: fileList) {
                if(file.isDirectory()){
                    dataString += file.getName() + ":" + "d" + "\r\n";
                }else{
                    dataString += file.getName() + ":" + "f" + "\r\n";
                }
            }
            System.out.println("Current directory content: " + dataString);
            if (dataString.lastIndexOf("\r\n") != -1) {
                dataString = dataString.substring(0, dataString.lastIndexOf("\r\n"));
            }

            try {
                return sendData(dataString.getBytes("US-ASCII"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        
        else if (receivedCommand[0].equals("CWD")) {
            String childName = receivedCommand[1];
            File root = new File(currentDirectory);
            File[] fileList = root.listFiles();
            boolean isSuccess = false;
                               
            for (File file: fileList) {
                if(file.getName().equals(childName) && file.isDirectory()){
                    isSuccess = true;
                    break;
                }
            }
            
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
            sendResponse(true);
            String fileName = receivedCommand[1];      
        
            try{
                byte data[] = retrieveData();
                writeBytesToFile(data, currentDirectory + fileName);
            }catch(Exception e){
                e.printStackTrace();
            };

            return true;
        }
        
        else if (receivedCommand[0].equals("MKDR")) {
            boolean isSucccess = (new File(currentDirectory + receivedCommand[1])).mkdirs();
            return sendResponse(isSucccess);
        }
        
        else if (receivedCommand[0].equals("RETR")) {
            String fileName = receivedCommand[1];
            File file = new File(currentDirectory + fileName);
            if(file.exists() && !file.isDirectory()){
                byte data[] = null;
                try {
                    data = Files.readAllBytes(file.toPath());
                    return sendData(data);
                } catch(Exception e){
                    e.printStackTrace();
                    return sendResponse(false);
                }
            }
            return sendResponse(false);
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
                if (dataConnection != null){
                    dataConnection.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return false;
        }
        return sendResponse(false);
    }

    boolean sendResponse(boolean isSuccess){
        String responseMessage = isSuccess ? SUCCESS : FAILURE;

        try {
            outToClient.writeBytes(responseMessage);
            System.out.println("Sending response "  + responseMessage);

            if(!isSuccess) {
                clientConnection.close();
                if (dataConnection != null){
                    dataConnection.close();
                }
                return false;
            }

            return true;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    byte[] retrieveData() throws IOException {
        int port = Integer.parseInt(serverDataPort);
        ServerSocket dataSocket = new ServerSocket(port);
        Socket dataConnection = dataSocket.accept();
        InputStream inFromData = dataConnection.getInputStream();

        byte[] header = new byte[2];
        inFromData.read(header, 0, 2);

        short size = ByteBuffer.wrap(header).getShort();
        byte[] content = new byte[size];
        inFromData.read(content);
        dataSocket.close();
        dataConnection.close();

        return content;
    }

    boolean sendData(byte[] data) {
        if (clientDataPort == -1) {
            return false;
        }

        boolean isSuccess = false;
        try {
            Socket dataConnection = new Socket(addr, clientDataPort);
            OutputStream outputStream = dataConnection.getOutputStream();
            DataOutputStream dataOut = new DataOutputStream(outputStream);

            byte[] header = new byte[2];
            ByteBuffer headerBuffer = ByteBuffer.allocate(2);
            headerBuffer.putShort((short) data.length);
            header = headerBuffer.array();

            byte[] result = new byte[header.length + data.length];
            for (int i = 0; i < result.length; i++) {
                result[i] = i < header.length ? header[i] : data[i-2];
            }
        
            dataOut.write(result);
            isSuccess = true;
            dataOut.close();
            dataConnection.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sendResponse(isSuccess);
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

    void writeBytesToFile(byte[] bytes, String filepath) 
    { 
        try { 
            File file = new File(filepath);
            // Initialize a pointer 
            // in file using OutputStream 
            OutputStream os = new FileOutputStream(file); 
  
            // Starts writing the bytes in it 
            os.write(bytes); 
            System.out.println("Successfully written to file"); 
  
            // Close the file 
            os.close(); 
        } 
  
        catch (Exception e) { 
            System.out.println("Exception: " + e); 
        } 
    } 
}