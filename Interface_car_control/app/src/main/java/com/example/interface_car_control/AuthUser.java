package com.example.interface_car_control;

import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AuthUser {
    private static AuthUser instance;
    private String serverIp;
    private int serverPort;
    private int clientPort;
    private ExecutorService executorService;
    private Socket socket;
    private DataOutputStream dataOutputStream;
    private BufferedReader bufferedReader;
    private boolean isInitialized = false;  //verify if the connection is initialized
    private ConnectionStatusListener connectionStatusListener;

    // interface for connection status
    public interface ConnectionStatusListener {
        void onConnectionError(String error);
        void onConnectionEstablished();
    }


    public void setConnectionStatusListener(ConnectionStatusListener listener) {
        this.connectionStatusListener = listener;
    }

    private AuthUser(String serverIp, int serverPort, int clientPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.clientPort = clientPort;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    package com.example.interface_car_control;

    import java.io.DataOutputStream;
    import java.io.BufferedReader;
    import java.io.IOException;
    import java.io.InputStreamReader;
    import java.net.InetAddress;
    import java.net.Socket;
    import java.net.SocketAddress;
    import java.net.InetSocketAddress;
    import java.net.UnknownHostException;
    import java.util.concurrent.Callable;
    import java.util.concurrent.ExecutorService;
    import java.util.concurrent.Executors;
    import java.util.concurrent.Future;
    import java.util.concurrent.TimeUnit;
    import java.util.concurrent.TimeoutException;

    public class AuthUser {
        private static AuthUser instance;
        private String serverIp;
        private int serverPort;
        private int clientPort;
        private ExecutorService executorService;
        private Socket socket;
        private DataOutputStream dataOutputStream;
        private BufferedReader bufferedReader;
        private boolean isInitialized = false;  // Verify if the connection is initialized
        private ConnectionStatusListener connectionStatusListener;

        // Interface for connection status
        public interface ConnectionStatusListener {
            void onConnectionError(String error);
            void onConnectionEstablished();
        }

        // Set the connection status listener
        public void setConnectionStatusListener(ConnectionStatusListener listener) {
            this.connectionStatusListener = listener;
        }

        // Private constructor to initialize the AuthUser instance
        private AuthUser(String serverIp, int serverPort, int clientPort) {
            this.serverIp = serverIp;
            this.serverPort = serverPort;
            this.clientPort = clientPort;
            this.executorService = Executors.newSingleThreadExecutor();
        }

        // Get the singleton instance of AuthUser with parameters
        public static synchronized AuthUser getInstance(String serverIp, int serverPort, int clientPort) {
            if (instance == null) {
                instance = new AuthUser(serverIp, serverPort, clientPort);
            }
            return instance;
        }

        // Get the singleton instance of AuthUser without parameters
        public static synchronized AuthUser getInstance() {
            if (instance == null) {
                throw new IllegalStateException("AuthUser is not initialized, call getInstance(serverIp, serverPort, clientPort) first.");
            }
            return instance;
        }

        // Reset the singleton instance of AuthUser
        public static void resetInstance() {
            if (instance != null) {
                instance.shutdown();
                instance = null;
            }
        }

        // Initialize the connection to the server
        public void initializeConnection() {
            executorService.submit(() -> {
                try {
                    InetAddress inetAddress = InetAddress.getByName(serverIp);
                    SocketAddress socketAddress = new InetSocketAddress(inetAddress, serverPort);
                    socket = new Socket();
                    socket.bind(new InetSocketAddress(clientPort));
                    socket.connect(socketAddress);
                    dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    isInitialized = true;  // Set flag to true after successful initialization
                } catch (UnknownHostException e) {
                    System.out.println("Failed to resolve host: " + e.getMessage());
                    resetInstance();  // Reset instance on error
                } catch (IOException e) {
                    System.out.println("IO error during connection setup: " + e.getMessage());
                    resetInstance();  // Reset instance on error
                }
            });
        }

        // Send a command to the server
        public void sendCommand(String command) {
            executorService.submit(new SendCommandTask(command + "\n"));  // Add \n to delimit the command
        }

        // Read the response from the server
        public String readResponse() {
            Future<String> futureResponse = executorService.submit(() -> {
                StringBuilder response = new StringBuilder();
                if (isInitialized && bufferedReader != null) {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        response.append(line).append("\n");
                        break; // Read only the first response for authentication
                    }
                } else {
                    System.out.println("Connection not initialized");
                }
                return response.toString();
            });

            try {
                return futureResponse.get(5, TimeUnit.SECONDS); // Wait for 5 seconds
            } catch (TimeoutException e) {
                System.out.println("Response read timeout.");
                shutdown();
                return null; // Or appropriate error handling
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        // Task to send a command to the server
        private class SendCommandTask implements Runnable {
            private String command;

            public SendCommandTask(String command) {
                this.command = command;
            }

            @Override
            public void run() {
                try {
                    if (isInitialized && dataOutputStream != null) {  // Verify if the connection is initialized
                        dataOutputStream.writeBytes(command);
                        dataOutputStream.flush();  // Ensure the command is sent immediately
                    } else {
                        System.out.println("Connection not initialized");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Shutdown the connection and release resources
        public void shutdown() {
            try {
                if (dataOutputStream != null) {
                    dataOutputStream.close();
                }
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                System.out.println("Error closing resources: " + e.getMessage());
            }
            executorService.shutdownNow();  // Attempt to stop all actively executing tasks
        }
    }

    public void initializeConnection() {
        executorService.submit(() -> {
            try {
                InetAddress inetAddress = InetAddress.getByName(serverIp);
                SocketAddress socketAddress = new InetSocketAddress(inetAddress, serverPort);
                socket = new Socket();
                socket.bind(new InetSocketAddress(clientPort));
                socket.connect(socketAddress);
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                isInitialized = true;  // Set flag to true after successful initialization
            } catch (UnknownHostException e) {
                System.out.println("Failed to resolve host: " + e.getMessage());
                resetInstance();  // Reset instance on error
            } catch (IOException e) {
                System.out.println("IO error during connection setup: " + e.getMessage());
                resetInstance();  // Reset instance on error
            }
        });
    }


    public void sendCommand(String command) {
        executorService.submit(new SendCommandTask(command + "\n"));  // Adăugăm \n pentru a delimita comanda
    }

    public String readResponse() {
        Future<String> futureResponse = executorService.submit(() -> {
            StringBuilder response = new StringBuilder();
            if (isInitialized && bufferedReader != null) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    response.append(line).append("\n");
                    break; // Citește doar primul răspuns pentru autentificare
                }
            } else {
                System.out.println("Connection not initialized");
            }
            return response.toString();
        });

        try {
            return futureResponse.get(5, TimeUnit.SECONDS); // Wait for 10 seconds
        } catch (TimeoutException e) {
            System.out.println("Response read timeout.");
            shutdown();
            return null; // Or appropriate error handling
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private class SendCommandTask implements Runnable {
        private String command;

        public SendCommandTask(String command) {
            this.command = command;
        }

        @Override
        public void run() {
            try {
                if (isInitialized && dataOutputStream != null) {  // Verifică dacă conexiunea este inițializată
                    dataOutputStream.writeBytes(command);
                    dataOutputStream.flush();  // Ensure the command is sent immediately
                } else {
                    System.out.println("Connection not initialized");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void shutdown() {
        try {
            if (dataOutputStream != null) {
                dataOutputStream.close();
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing resources: " + e.getMessage());
        }
        executorService.shutdownNow();  // Attempt to stop all actively executing tasks
    }

}
