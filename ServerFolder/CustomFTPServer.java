package ServerFolder;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class CustomFTPServer {

    final static String SUCCESS = "400";
    final static String FAILURE = "200";

    public static void main(String[] args){

        if(args.length != 2){
            System.out.println("Exactly two parameters are needed: <Addr> <ControlPort> ");
            return;
        }
        String responseMessage = "";

        System.out.println("Server is running...");

        //Get parameters from arguments
        String addr = args[0];
        int controlPort = Integer.parseInt(args[1]);

        //Create Server socket
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(controlPort);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Server could not be established at port: " + controlPort);
            System.out.println("Closing the ServerFolder.CustomFtpServer");
            return;
        }

        System.out.println("Listening port " + controlPort + ".");
        Socket clientPort;
        while(true){
            //Listen the port and accept incoming request
            Socket clientConnection = null;
            try {
                clientConnection = serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Received new client request");


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

            String requestMessage = null;
            try {
                requestMessage = inFromClient.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(requestMessage);

            String inData[] = requestMessage.split(" ");
            if (inData[0].equals("PORT")){
                try {
                    clientPort = new Socket(addr, Integer.parseInt(inData[1]));
                    responseMessage = SUCCESS + "\r\n";
                } catch (IOException e) {
                    responseMessage = FAILURE + "\r\n";
                    e.printStackTrace();
                }finally {
                    try {
                        outToClient.writeBytes(responseMessage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            System.out.println("Request has been handled");
        }

    }

    private static void handleClientRequest(Socket clientConnection, String addr) throws IOException {



    }

}