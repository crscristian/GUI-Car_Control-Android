package com.example.interface_car_control;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class TCPAuthResponse implements Runnable {
    private String serverIp;
    private int serverPort;
    private ResponseCallback callback;

    public TCPAuthResponse(String serverIp, int serverPort, ResponseCallback callback) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.callback = callback;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(serverIp, serverPort);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line).append("\n");
            }
            if (callback != null) {
                callback.onResponseReceived(responseBuilder.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface ResponseCallback {
        void onResponseReceived(String response);
    }
}
