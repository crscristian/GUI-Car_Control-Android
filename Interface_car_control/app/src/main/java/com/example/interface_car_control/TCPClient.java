package com.example.interface_car_control;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPClient {
    private String serverIp;
    private int serverPort;
    private ExecutorService executorService;
    private Socket socket;
    private DataOutputStream dataOutputStream;

    // Constructor to initialize server IP, port, and executor service
    public TCPClient(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.executorService = Executors.newSingleThreadExecutor();
        initializeConnection(); // Initialize connection upon creation
    }

    // Method to initialize the connection to the server
    private void initializeConnection() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    InetAddress inetAddress = InetAddress.getByName(serverIp);
                    socket = new Socket(inetAddress, serverPort);
                    dataOutputStream = new DataOutputStream(socket.getOutputStream());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // Method to send a command to the server
    public void sendCommand(String command) {
        executorService.submit(new SendCommandTask(command));
    }

    // Runnable task to send a command
    private class SendCommandTask implements Runnable {
        private String command;

        public SendCommandTask(String command) {
            this.command = command;
        }

        @Override
        public void run() {
            try {
                if (dataOutputStream != null) {
                    dataOutputStream.writeBytes(command);
                    dataOutputStream.flush();  // Ensure the command is sent immediately
                } else {
                    System.out.println("DataOutputStream is null");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Method to close the connection and shutdown the executor service
    public void shutdown() {
        try {
            if (dataOutputStream != null) {
                dataOutputStream.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        executorService.shutdown();
    }
}
