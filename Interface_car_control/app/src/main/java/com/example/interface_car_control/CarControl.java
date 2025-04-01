package com.example.interface_car_control;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import androidx.lifecycle.MutableLiveData;
import me.ibrahimsn.lib.Speedometer;
import com.example.interface_car_control.databinding.ActivityCarControlBinding;
import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CarControl extends AppCompatActivity implements GestureDetector.OnGestureListener {

    private GestureDetector gestureDetector;
    private Handler handler = new Handler();
    private Runnable processSwipe;
    private static final long SWIPE_DELAY = 500; // Delay de 500 ms între swipe-uri
    private long lastSwipeTime = 0;

    // define variabile for stream video
    private ImageView imageView;
    private String streamUrl;
    private String username = "admin";
    private String password = "password";
    private MutableLiveData<Bitmap> bitmapLiveData;

    private ActivityCarControlBinding binding;
    private AuthUser authUser;
    private CryptoData cryptoData;

    public Vector<String> commands = new Vector<>(Arrays.asList("LEFT", "RIGHT", "BACK", "FORWARD", "CX-", "CX+", "CZ-", "CZ+", "THROTTLE", "BRAKE"));

    private TextView speedTextView;
    private TextView distanceTextView;
    private Speedometer speedometer;
    private ExecutorService executorService, responseExecutorService;

    private MjpegStreamProcessor mjpegStreamProcessor;
    private Handler brakeHandler = new Handler();
    private Runnable brakeRunnable;
    private int brakeValue = 1;
    private long buttonPressStartTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCarControlBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obțineți parametrii din Intent
        Intent intent = getIntent();
        String serverIp = intent.getStringExtra("IP_ADDRESS");
        int serverPort = intent.getIntExtra("SERVER_PORT", 8080);
        int clientPort = intent.getIntExtra("CLIENT_PORT", 5050);

        authUser = AuthUser.getInstance(serverIp, serverPort, clientPort); // Folosește instanța `AuthUser` existentă
        // Initializare TextViews pentru viteza și distanță
        speedTextView = findViewById(R.id.textViewSpeed);
        distanceTextView = findViewById(R.id.textViewDistance);
        speedometer = findViewById(R.id.speedometer);

        imageView = findViewById(R.id.image_View);
        bitmapLiveData = new MutableLiveData<>();
        mjpegStreamProcessor = new MjpegStreamProcessor(bitmapLiveData);

        bitmapLiveData.observe(this, bitmap -> {
            imageView.setImageBitmap(bitmap);
        });

        // Start streaming
        String ipAddress = AdressIP.getAdresa();
        streamUrl = "http://" + ipAddress + ":5000/stream.mjpg";
        mjpegStreamProcessor.startStreaming(streamUrl, username, password);

        setOnTouchRepeatListener(binding.leftDirection, commands.get(0));
        setOnTouchRepeatListener(binding.rightDirection, commands.get(1));
        setOnTouchRepeatListener(binding.accelerate, commands.get(8));

        //swipe
        gestureDetector = new GestureDetector(this, this);
        View view = findViewById(R.id.image_View);
        view.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

        // BRAKE direction
        binding.back.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        binding.back.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.login), PorterDuff.Mode.SRC_ATOP);
                        buttonPressStartTime = System.currentTimeMillis();
                        brakeValue = 1; // Resetăm valoarea de frânare la 1
                        sendEncryptedCommand(commands.get(9) + brakeValue); // Trimitem valoarea inițială
                        startBrakeHandler(v);
                        return true;
                    case MotionEvent.ACTION_UP:
                        binding.back.clearColorFilter();
                        stopBrakeHandler();
                        return true;
                }
                return false;
            }
        });

        // Inițializare ExecutorService pentru citirea răspunsurilor în mod continuu
        responseExecutorService = Executors.newSingleThreadExecutor();
        responseExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    String response = authUser.readResponse();
                    if (response != null && !response.isEmpty()) {
                        response = response.trim();
                        String finalResponse = response;
                        runOnUiThread(() -> {
                            // Actualizează UI-ul cu datele primite
                            String[] parts = finalResponse.split(",");
                            if (parts.length == 2) {
                               // speedTextView.setText("Speed: " + parts[0] + " cm/s");
                                //distanceTextView.setText("Distance: " + parts[1] + " cm");
                                // Actualizează valoarea în Speedometer
                                try {
                                    float speed = Float.parseFloat(parts[0]);
                                    speedometer.setSpeed((int) speed, 1000L, () -> null);  // Setează viteza în Speedometer
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                    try {
                        Thread.sleep(1000); // Așteaptă 1 secundă înainte de a citi din nou
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        // Switch pentru direcție
        Switch directionSwitch = findViewById(R.id.direction);
        directionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Log.d("SwitchState", "Pornit");
                sendEncryptedCommand(commands.get(2));
            } else {
                Log.d("SwitchState", "Oprit");
                sendEncryptedCommand(commands.get(3));
            }
        });
    }

    private void startBrakeHandler(View v) {
        brakeRunnable = new Runnable() {
            @Override
            public void run() {
                long pressDuration = System.currentTimeMillis() - buttonPressStartTime;
                if (pressDuration >= 1000) {
                    brakeValue = Math.min(brakeValue * 2, 128); // Crește valoarea exponențial până la un maxim
                } else if (pressDuration >= 700) {
                    brakeValue = 10;
                } else if (pressDuration >= 300) {
                    brakeValue = 4;
                } else if (pressDuration >= 100) {
                    brakeValue = 2;
                }
                sendEncryptedCommand(commands.get(9) + brakeValue);
                brakeHandler.postDelayed(this, 100);
            }
        };
        brakeHandler.post(brakeRunnable);
    }

    private void stopBrakeHandler() {
        if (brakeRunnable != null) {
            brakeHandler.removeCallbacks(brakeRunnable);
        }
    }

    private void setOnTouchRepeatListener(View button, String command) {
        Handler handler = new Handler(Looper.getMainLooper());

        Runnable longClickAction = new Runnable() {
            @Override
            public void run() {
                sendEncryptedCommand(command);
                handler.postDelayed(this, 500); // Repetare la fiecare 500 ms
            }
        };

        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        ((ImageView) v).setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.login), PorterDuff.Mode.SRC_ATOP);
                        sendEncryptedCommand(command); // Trimite comanda inițială
                        handler.postDelayed(longClickAction, 500); // Start long click after 500 ms
                        return true;
                    case MotionEvent.ACTION_UP:
                        ((ImageView) v).clearColorFilter();
                        handler.removeCallbacks(longClickAction); // Stop the long click actions
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSwipeTime > SWIPE_DELAY) {
            lastSwipeTime = currentTime;
            handler.removeCallbacks(processSwipe);
            processSwipe = () -> processSwipe(e1, e2, distanceX, distanceY);
            handler.postDelayed(processSwipe, SWIPE_DELAY);
        }
        return true;
    }

    private void processSwipe(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        float deltaX = e2.getX() - e1.getX();
        float deltaY = e2.getY() - e1.getY();

        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            if (deltaX > 0) {
                Log.i("Gesture", "Slow Swipe Right");
                sendEncryptedCommand(commands.get(4));
            } else {
                Log.i("Gesture", "Slow Swipe Left");
                sendEncryptedCommand(commands.get(5));
            }
        } else {
            if (deltaY > 0) {
                Log.i("Gesture", "Slow Swipe Down");
                sendEncryptedCommand(commands.get(7));
            } else {
                Log.i("Gesture", "Slow Swipe Up");
                sendEncryptedCommand(commands.get(6));
            }
        }
    }

    @Override
    public void onShowPress(MotionEvent e) {}

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {}

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    private void closeResources() {
        try {
            if (authUser != null) {
                authUser.shutdown();
            }
            if (mjpegStreamProcessor != null) {
                mjpegStreamProcessor.stopStreaming();
            }
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdownNow();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Sigur vrei sa iesi din aplicatie?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    try {
                        authUser.shutdown();
                        if (mjpegStreamProcessor != null) {
                            mjpegStreamProcessor.stopStreaming();
                        }
                        closeResources();
                        this.finish();
                        this.finishAndRemoveTask();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                package com.example.interface_car_control;

                import androidx.appcompat.app.AlertDialog;
                import androidx.appcompat.app.AppCompatActivity;
                import androidx.core.content.ContextCompat;
                import android.annotation.SuppressLint;
                import android.content.Intent;
                import android.graphics.Bitmap;
                import android.graphics.PorterDuff;
                import android.os.Bundle;
                import android.os.Handler;
                import android.os.Looper;
                import android.util.Log;
                import android.view.GestureDetector;
                import android.view.MotionEvent;
                import android.view.View;
                import android.widget.ImageView;
                import android.widget.Switch;
                import android.widget.TextView;
                import androidx.lifecycle.MutableLiveData;
                import me.ibrahimsn.lib.Speedometer;
                import com.example.interface_car_control.databinding.ActivityCarControlBinding;
                import java.util.Arrays;
                import java.util.Vector;
                import java.util.concurrent.ExecutorService;
                import java.util.concurrent.Executors;

                public class CarControl extends AppCompatActivity implements GestureDetector.OnGestureListener {

                    private GestureDetector gestureDetector;
                    private Handler handler = new Handler();
                    private Runnable processSwipe;
                    private static final long SWIPE_DELAY = 500; // 500 ms delay between swipes
                    private long lastSwipeTime = 0;

                    // Define variables for video stream
                    private ImageView imageView;
                    private String streamUrl;
                    private String username = "admin";
                    private String password = "password";
                    private MutableLiveData<Bitmap> bitmapLiveData;

                    private ActivityCarControlBinding binding;
                    private AuthUser authUser;
                    private CryptoData cryptoData;

                    public Vector<String> commands = new Vector<>(Arrays.asList("LEFT", "RIGHT", "BACK", "FORWARD", "CX-", "CX+", "CZ-", "CZ+", "THROTTLE", "BRAKE"));

                    private TextView speedTextView;
                    private TextView distanceTextView;
                    private Speedometer speedometer;
                    private ExecutorService executorService, responseExecutorService;

                    private MjpegStreamProcessor mjpegStreamProcessor;
                    private Handler brakeHandler = new Handler();
                    private Runnable brakeRunnable;
                    private int brakeValue = 1;
                    private long buttonPressStartTime;

                    @Override
                    protected void onCreate(Bundle savedInstanceState) {
                        super.onCreate(savedInstanceState);
                        binding = ActivityCarControlBinding.inflate(getLayoutInflater());
                        setContentView(binding.getRoot());

                        // Get parameters from Intent
                        Intent intent = getIntent();
                        String serverIp = intent.getStringExtra("IP_ADDRESS");
                        int serverPort = intent.getIntExtra("SERVER_PORT", 8080);
                        int clientPort = intent.getIntExtra("CLIENT_PORT", 5050);

                        authUser = AuthUser.getInstance(serverIp, serverPort, clientPort); // Use existing `AuthUser` instance
                        // Initialize TextViews for speed and distance
                        speedTextView = findViewById(R.id.textViewSpeed);
                        distanceTextView = findViewById(R.id.textViewDistance);
                        speedometer = findViewById(R.id.speedometer);

                        imageView = findViewById(R.id.image_View);
                        bitmapLiveData = new MutableLiveData<>();
                        mjpegStreamProcessor = new MjpegStreamProcessor(bitmapLiveData);

                        bitmapLiveData.observe(this, bitmap -> {
                            imageView.setImageBitmap(bitmap);
                        });

                        // Start streaming
                        String ipAddress = AdressIP.getAdresa();
                        streamUrl = "http://" + ipAddress + ":5000/stream.mjpg";
                        mjpegStreamProcessor.startStreaming(streamUrl, username, password);

                        setOnTouchRepeatListener(binding.leftDirection, commands.get(0));
                        setOnTouchRepeatListener(binding.rightDirection, commands.get(1));
                        setOnTouchRepeatListener(binding.accelerate, commands.get(8));

                        // Swipe
                        gestureDetector = new GestureDetector(this, this);
                        View view = findViewById(R.id.image_View);
                        view.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

                        // BRAKE direction
                        binding.back.setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View v, MotionEvent event) {
                                switch (event.getAction()) {
                                    case MotionEvent.ACTION_DOWN:
                                        binding.back.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.login), PorterDuff.Mode.SRC_ATOP);
                                        buttonPressStartTime = System.currentTimeMillis();
                                        brakeValue = 1; // Reset brake value to 1
                                        sendEncryptedCommand(commands.get(9) + brakeValue); // Send initial value
                                        startBrakeHandler(v);
                                        return true;
                                    case MotionEvent.ACTION_UP:
                                        binding.back.clearColorFilter();
                                        stopBrakeHandler();
                                        return true;
                                }
                                return false;
                            }
                        });

                        // Initialize ExecutorService for continuously reading responses
                        responseExecutorService = Executors.newSingleThreadExecutor();
                        responseExecutorService.submit(new Runnable() {
                            @Override
                            public void run() {
                                while (true) {
                                    String response = authUser.readResponse();
                                    if (response != null && !response.isEmpty()) {
                                        response = response.trim();
                                        String finalResponse = response;
                                        runOnUiThread(() -> {
                                            // Update UI with received data
                                            String[] parts = finalResponse.split(",");
                                            if (parts.length == 2) {
                                               // speedTextView.setText("Speed: " + parts[0] + " cm/s");
                                                //distanceTextView.setText("Distance: " + parts[1] + " cm");
                                                // Update value in Speedometer
                                                try {
                                                    float speed = Float.parseFloat(parts[0]);
                                                    speedometer.setSpeed((int) speed, 1000L, () -> null);  // Set speed in Speedometer
                                                } catch (NumberFormatException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                                    }
                                    try {
                                        Thread.sleep(1000); // Wait 1 second before reading again
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });

                        // Switch for direction
                        Switch directionSwitch = findViewById(R.id.direction);
                        directionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            if (isChecked) {
                                Log.d("SwitchState", "On");
                                sendEncryptedCommand(commands.get(2));
                            } else {
                                Log.d("SwitchState", "Off");
                                sendEncryptedCommand(commands.get(3));
                            }
                        });
                    }

                    private void startBrakeHandler(View v) {
                        brakeRunnable = new Runnable() {
                            @Override
                            public void run() {
                                long pressDuration = System.currentTimeMillis() - buttonPressStartTime;
                                if (pressDuration >= 1000) {
                                    brakeValue = Math.min(brakeValue * 2, 128); // Increase value exponentially up to a maximum
                                } else if (pressDuration >= 700) {
                                    brakeValue = 10;
                                } else if (pressDuration >= 300) {
                                    brakeValue = 4;
                                } else if (pressDuration >= 100) {
                                    brakeValue = 2;
                                }
                                sendEncryptedCommand(commands.get(9) + brakeValue);
                                brakeHandler.postDelayed(this, 100);
                            }
                        };
                        brakeHandler.post(brakeRunnable);
                    }

                    private void stopBrakeHandler() {
                        if (brakeRunnable != null) {
                            brakeHandler.removeCallbacks(brakeRunnable);
                        }
                    }

                    private void setOnTouchRepeatListener(View button, String command) {
                        Handler handler = new Handler(Looper.getMainLooper());

                        Runnable longClickAction = new Runnable() {
                            @Override
                            public void run() {
                                sendEncryptedCommand(command);
                                handler.postDelayed(this, 500); // Repeat every 500 ms
                            }
                        };

                        button.setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View v, MotionEvent event) {
                                switch (event.getAction()) {
                                    case MotionEvent.ACTION_DOWN:
                                        ((ImageView) v).setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.login), PorterDuff.Mode.SRC_ATOP);
                                        sendEncryptedCommand(command); // Send initial command
                                        handler.postDelayed(longClickAction, 500); // Start long click after 500 ms
                                        return true;
                                    case MotionEvent.ACTION_UP:
                                        ((ImageView) v).clearColorFilter();
                                        handler.removeCallbacks(longClickAction); // Stop the long click actions
                                        return true;
                                }
                                return false;
                            }
                        });
                    }

                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastSwipeTime > SWIPE_DELAY) {
                            lastSwipeTime = currentTime;
                            handler.removeCallbacks(processSwipe);
                            processSwipe = () -> processSwipe(e1, e2, distanceX, distanceY);
                            handler.postDelayed(processSwipe, SWIPE_DELAY);
                        }
                        return true;
                    }

                    private void processSwipe(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                        float deltaX = e2.getX() - e1.getX();
                        float deltaY = e2.getY() - e1.getY();

                        if (Math.abs(deltaX) > Math.abs(deltaY)) {
                            if (deltaX > 0) {
                                Log.i("Gesture", "Slow Swipe Right");
                                sendEncryptedCommand(commands.get(4));
                            } else {
                                Log.i("Gesture", "Slow Swipe Left");
                                sendEncryptedCommand(commands.get(5));
                            }
                        } else {
                            if (deltaY > 0) {
                                Log.i("Gesture", "Slow Swipe Down");
                                sendEncryptedCommand(commands.get(7));
                            } else {
                                Log.i("Gesture", "Slow Swipe Up");
                                sendEncryptedCommand(commands.get(6));
                            }
                        }
                    }

                    @Override
                    public void onShowPress(MotionEvent e) {}

                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        return false;
                    }

                    @Override
                    public void onLongPress(MotionEvent e) {}

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                        return false;
                    }

                    private void closeResources() {
                        try {
                            if (authUser != null) {
                                authUser.shutdown();
                            }
                            if (mjpegStreamProcessor != null) {
                                mjpegStreamProcessor.stopStreaming();
                            }
                            if (executorService != null && !executorService.isShutdown()) {
                                executorService.shutdownNow();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @SuppressLint("MissingSuperCall")
                    @Override
                    public void onBackPressed() {
                        new AlertDialog.Builder(this)
                                .setMessage("Are you sure you want to exit the application?")
                                .setPositiveButton("Yes", (dialog, which) -> {
                                    try {
                                        authUser.shutdown();
                                        if (mjpegStreamProcessor != null) {
                                            mjpegStreamProcessor.stopStreaming();
                                        }
                                        closeResources();
                                        this.finish();
                                        this.finishAndRemoveTask();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                })
                                .setNegativeButton("No", (dialog, which) -> {
                                    dialog.dismiss();
                                })
                                .show();
                    }
                    private void sendEncryptedCommand(String command) {
                        String encryptedCommand;
                        try {
                            encryptedCommand = CryptoData.encrypt(command);
                            Log.i("Encrypted command: ", encryptedCommand);
                            authUser.sendCommand(encryptedCommand);  // Use existing `authUser` instance
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                })
                .show();
    }
    private void sendEncryptedCommand(String command) {
        String encryptedCommand;
        try {
            encryptedCommand = CryptoData.encrypt(command);
            Log.i("Encrypted command: ", encryptedCommand);
            authUser.sendCommand(encryptedCommand);  // Folosește instanța `authUser` existentă
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
package com.example.interface_car_control;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import androidx.lifecycle.MutableLiveData;
import me.ibrahimsn.lib.Speedometer;
import com.example.interface_car_control.databinding.ActivityCarControlBinding;
import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CarControl extends AppCompatActivity implements GestureDetector.OnGestureListener {

    private GestureDetector gestureDetector;
    private Handler handler = new Handler();
    private Runnable processSwipe;
    private static final long SWIPE_DELAY = 500; // 500 ms delay between swipes
    private long lastSwipeTime = 0;

    // Define variables for video stream
    private ImageView imageView;
    private String streamUrl;
    private String username = "admin";
    private String password = "password";
    private MutableLiveData<Bitmap> bitmapLiveData;

    private ActivityCarControlBinding binding;
    private AuthUser authUser;
    private CryptoData cryptoData;

    public Vector<String> commands = new Vector<>(Arrays.asList("LEFT", "RIGHT", "BACK", "FORWARD", "CX-", "CX+", "CZ-", "CZ+", "THROTTLE", "BRAKE"));

    private TextView speedTextView;
    private TextView distanceTextView;
    private Speedometer speedometer;
    private ExecutorService executorService, responseExecutorService;

    private MjpegStreamProcessor mjpegStreamProcessor;
    private Handler brakeHandler = new Handler();
    private Runnable brakeRunnable;
    private int brakeValue = 1;
    private long buttonPressStartTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCarControlBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get parameters from Intent
        Intent intent = getIntent();
        String serverIp = intent.getStringExtra("IP_ADDRESS");
        int serverPort = intent.getIntExtra("SERVER_PORT", 8080);
        int clientPort = intent.getIntExtra("CLIENT_PORT", 5050);

        authUser = AuthUser.getInstance(serverIp, serverPort, clientPort); // Use existing `AuthUser` instance
        // Initialize TextViews for speed and distance
        speedTextView = findViewById(R.id.textViewSpeed);
        distanceTextView = findViewById(R.id.textViewDistance);
        speedometer = findViewById(R.id.speedometer);

        imageView = findViewById(R.id.image_View);
        bitmapLiveData = new MutableLiveData<>();
        mjpegStreamProcessor = new MjpegStreamProcessor(bitmapLiveData);

        bitmapLiveData.observe(this, bitmap -> {
            imageView.setImageBitmap(bitmap);
        });

        // Start streaming
        String ipAddress = AdressIP.getAdresa();
        streamUrl = "http://" + ipAddress + ":5000/stream.mjpg";
        mjpegStreamProcessor.startStreaming(streamUrl, username, password);

        setOnTouchRepeatListener(binding.leftDirection, commands.get(0));
        setOnTouchRepeatListener(binding.rightDirection, commands.get(1));
        setOnTouchRepeatListener(binding.accelerate, commands.get(8));

        // Swipe
        gestureDetector = new GestureDetector(this, this);
        View view = findViewById(R.id.image_View);
        view.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

        // BRAKE direction
        binding.back.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        binding.back.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.login), PorterDuff.Mode.SRC_ATOP);
                        buttonPressStartTime = System.currentTimeMillis();
                        brakeValue = 1; // Reset brake value to 1
                        sendEncryptedCommand(commands.get(9) + brakeValue); // Send initial value
                        startBrakeHandler(v);
                        return true;
                    case MotionEvent.ACTION_UP:
                        binding.back.clearColorFilter();
                        stopBrakeHandler();
                        return true;
                }
                return false;
            }
        });

        // Initialize ExecutorService for continuously reading responses
        responseExecutorService = Executors.newSingleThreadExecutor();
        responseExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    String response = authUser.readResponse();
                    if (response != null && !response.isEmpty()) {
                        response = response.trim();
                        String finalResponse = response;
                        runOnUiThread(() -> {
                            // Update UI with received data
                            String[] parts = finalResponse.split(",");
                            if (parts.length == 2) {
                               // speedTextView.setText("Speed: " + parts[0] + " cm/s");
                                //distanceTextView.setText("Distance: " + parts[1] + " cm");
                                // Update value in Speedometer
                                try {
                                    float speed = Float.parseFloat(parts[0]);
                                    speedometer.setSpeed((int) speed, 1000L, () -> null);  // Set speed in Speedometer
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                    try {
                        Thread.sleep(1000); // Wait 1 second before reading again
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        // Switch for direction
        Switch directionSwitch = findViewById(R.id.direction);
        directionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Log.d("SwitchState", "On");
                sendEncryptedCommand(commands.get(2));
            } else {
                Log.d("SwitchState", "Off");
                sendEncryptedCommand(commands.get(3));
            }
        });
    }

    private void startBrakeHandler(View v) {
        brakeRunnable = new Runnable() {
            @Override
            public void run() {
                long pressDuration = System.currentTimeMillis() - buttonPressStartTime;
                if (pressDuration >= 1000) {
                    brakeValue = Math.min(brakeValue * 2, 128); // Increase value exponentially up to a maximum
                } else if (pressDuration >= 700) {
                    brakeValue = 10;
                } else if (pressDuration >= 300) {
                    brakeValue = 4;
                } else if (pressDuration >= 100) {
                    brakeValue = 2;
                }
                sendEncryptedCommand(commands.get(9) + brakeValue);
                brakeHandler.postDelayed(this, 100);
            }
        };
        brakeHandler.post(brakeRunnable);
    }

    private void stopBrakeHandler() {
        if (brakeRunnable != null) {
            brakeHandler.removeCallbacks(brakeRunnable);
        }
    }

    private void setOnTouchRepeatListener(View button, String command) {
        Handler handler = new Handler(Looper.getMainLooper());

        Runnable longClickAction = new Runnable() {
            @Override
            public void run() {
                sendEncryptedCommand(command);
                handler.postDelayed(this, 500); // Repeat every 500 ms
            }
        };

        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        ((ImageView) v).setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.login), PorterDuff.Mode.SRC_ATOP);
                        sendEncryptedCommand(command); // Send initial command
                        handler.postDelayed(longClickAction, 500); // Start long click after 500 ms
                        return true;
                    case MotionEvent.ACTION_UP:
                        ((ImageView) v).clearColorFilter();
                        handler.removeCallbacks(longClickAction); // Stop the long click actions
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSwipeTime > SWIPE_DELAY) {
            lastSwipeTime = currentTime;
            handler.removeCallbacks(processSwipe);
            processSwipe = () -> processSwipe(e1, e2, distanceX, distanceY);
            handler.postDelayed(processSwipe, SWIPE_DELAY);
        }
        return true;
    }

    private void processSwipe(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        float deltaX = e2.getX() - e1.getX();
        float deltaY = e2.getY() - e1.getY();

        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            if (deltaX > 0) {
                Log.i("Gesture", "Slow Swipe Right");
                sendEncryptedCommand(commands.get(4));
            } else {
                Log.i("Gesture", "Slow Swipe Left");
                sendEncryptedCommand(commands.get(5));
            }
        } else {
            if (deltaY > 0) {
                Log.i("Gesture", "Slow Swipe Down");
                sendEncryptedCommand(commands.get(7));
            } else {
                Log.i("Gesture", "Slow Swipe Up");
                sendEncryptedCommand(commands.get(6));
            }
        }
    }

    @Override
    public void onShowPress(MotionEvent e) {}

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {}

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    private void closeResources() {
        try {
            if (authUser != null) {
                authUser.shutdown();
            }
            if (mjpegStreamProcessor != null) {
                mjpegStreamProcessor.stopStreaming();
            }
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdownNow();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit the application?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    try {
                        authUser.shutdown();
                        if (mjpegStreamProcessor != null) {
                            mjpegStreamProcessor.stopStreaming();
                        }
                        closeResources();
                        this.finish();
                        this.finishAndRemoveTask();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }
    private void sendEncryptedCommand(String command) {
        String encryptedCommand;
        try {
            encryptedCommand = CryptoData.encrypt(command);
            Log.i("Encrypted command: ", encryptedCommand);
            authUser.sendCommand(encryptedCommand);  // Use existing `authUser` instance
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
