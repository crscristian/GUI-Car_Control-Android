package com.example.interface_car_control;

import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServerResponse implements Runnable {
    private String serverIp;
    private int serverPort;
    private TextView speedTextView;
    private TextView distanceTextView;

    // Constructor to initialize server IP, port, and TextViews
    public TCPServerResponse(String serverIp, int serverPort, TextView speedTextView, TextView distanceTextView) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.speedTextView = speedTextView;
        this.distanceTextView = distanceTextView;
    }

    @Override
    public void run() {
        // Try-with-resources to ensure socket and reader are closed properly
        try (Socket socket = new Socket(serverIp, serverPort);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String line;
            // Read lines from the server
            while ((line = reader.readLine()) != null) {
                // Split the line into speed and distance
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    String speed = parts[0];
                    String distance = parts[1];

                    // Update UI on the main thread
                    speedTextView.post(() -> {
                        speedTextView.setText("Speed: " + speed + " cm/s");
                        distanceTextView.setText("Distance: " + distance + " cm");
                    });
                }
            }
        } catch (Exception e) {
            // Print stack trace in case of an exception
            e.printStackTrace();
        }
    }
}
