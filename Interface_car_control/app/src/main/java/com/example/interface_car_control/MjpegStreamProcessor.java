package com.example.interface_car_control;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import androidx.lifecycle.MutableLiveData;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MjpegStreamProcessor {
    private ExecutorService executorService = Executors.newFixedThreadPool(1); // Create a pool with a single thread
    private MutableLiveData<Bitmap> bitmapLiveData;
    private boolean isStreaming = false; // Flag to check if streaming is active

    public MjpegStreamProcessor(MutableLiveData<Bitmap> bitmapLiveData) {
        this.bitmapLiveData = bitmapLiveData;
    }

    public void startStreaming(String url, String username, String password) {
        isStreaming = true;
        executorService.submit(() -> {
            // Logic to connect to the URL and retrieve the stream
            try {
                URL streamUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) streamUrl.openConnection();
                String auth = "Basic " + Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP);
                connection.setRequestProperty("Authorization", auth);
                connection.connect();

                InputStream inputStream = connection.getInputStream();
                ByteArrayOutputStream frameBuffer = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                boolean insideFrame = false;

                while (isStreaming && (bytesRead = inputStream.read(buffer)) != -1) {
                    for (int i = 0; i < bytesRead - 1; i++) {
                        if ((buffer[i] & 0xFF) == 0xFF && (buffer[i + 1] & 0xFF) == 0xD8) {
                            if (insideFrame) {
                                // End of the previous frame
                                frameBuffer.write(buffer, 0, i);
                                byte[] frameData = frameBuffer.toByteArray();
                                Bitmap bitmap = BitmapFactory.decodeByteArray(frameData, 0, frameData.length);
                                if (bitmap != null) {
                                    bitmapLiveData.postValue(bitmap);
                                }
                                frameBuffer.reset();
                            }
                            insideFrame = true;
                        }
                        if (insideFrame) {
                            frameBuffer.write(buffer[i]);
                        }
                    }
                    if (insideFrame) {
                        frameBuffer.write(buffer[bytesRead - 1]);
                    }
                }

                inputStream.close();
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void stopStreaming() {
        isStreaming = false;
        if (!executorService.isShutdown()) {
            executorService.shutdownNow(); // Try to stop all active tasks
            try {
                if (executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                    System.out.println("Executor service stopped successfully.");
                }
            } catch (InterruptedException e) {
                System.err.println("Interrupted while waiting for executor service to terminate.");
                Thread.currentThread().interrupt();
            }
        }
    }

}
