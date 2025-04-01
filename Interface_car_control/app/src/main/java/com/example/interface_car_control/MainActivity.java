package com.example.interface_car_control;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.example.interface_car_control.databinding.ActivityMainBinding;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity implements TCPAuthResponse.ResponseCallback {

    private ActivityMainBinding binding;
    public String ip_address, ipAddress;
    int port;
    public String user_name;
    public String pass_word;
    private AuthUser authUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Handle login button click
        binding.login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ip_address = binding.ipaddress.getText().toString();
                user_name = binding.userName.getText().toString();
                pass_word = binding.passWord.getText().toString();
                if (ip_address.isEmpty() || user_name.isEmpty() || pass_word.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Invalid input. Unfilled fields", Toast.LENGTH_SHORT).show();
                } else {
                    InetSocketAddress address = IPAddressUtils.parseAddress(ip_address);
                    if (address != null) {
                        ipAddress = address.getAddress().getHostAddress();
                        port = address.getPort();
                        if ("0.0.0.0".equals(ipAddress) || port == 0) {
                            Toast.makeText(MainActivity.this, "Incorrect IP. ex:0.0.0.0:0", Toast.LENGTH_SHORT).show();
                        } else {
                            System.out.println("Adresa: " + ip_address);
                            System.out.println("Numar port: " + port);
                            System.out.println("User name: " + user_name);
                            System.out.println("Password hash: " + HashUtils.sha256(pass_word));

                            // Utilizează un port client fix (de exemplu, 5050)
                            int clientPort = 5050;
                            authUser = AuthUser.getInstance(ipAddress, port, clientPort);
                            authUser.initializeConnection();

                            // Setează adresa IP în clasa AdressIP
                            AdressIP.setAdresa(ipAddress);

                            String authentication = user_name + "," + HashUtils.sha256(pass_word) + "\n";
                            System.out.println("User name+password: " + authentication);
                            authUser.sendCommand(authentication);
                            // Wait for the connection to initialize
                            ExecutorService executorService = Executors.newSingleThreadExecutor();
                            Future<String> futureResponse = executorService.submit(() -> {
                                try {
                                    Thread.sleep(1000); // Wait for a second to ensure the connection is established
                                    return authUser.readResponse();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    return null;
                                }
                            });

                            executorService.submit(() -> {
                                try {
                                    String response = futureResponse.get(10, TimeUnit.SECONDS); // Wait for 10 seconds for the response
                                    runOnUiThread(() -> {
                                        System.out.println("Validare server: " + response);
                                        if (response != null && response.contains("Authentication successful")) {
                                            Toast.makeText(MainActivity.this, "Authentication successful", Toast.LENGTH_SHORT).show();
                                            // Start new activity or handle success
                                            startActivity(new Intent(MainActivity.this, CarControl.class));
                                            finish(); // Close MainActivity
                                        } else {
                                            Toast.makeText(MainActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } catch (TimeoutException e) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(MainActivity.this, "Connection failed", Toast.LENGTH_SHORT).show();
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Invalid IP address format. Expected format: IP:port", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

       /* binding.forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ResetPassword.class));
            }
        });*/
    }

    @Override
    public void onResponseReceived(String response) {
        //runOnUiThread(() -> responseView.setText(response));
    }
}
package com.example.interface_car_control;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.example.interface_car_control.databinding.ActivityMainBinding;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity implements TCPAuthResponse.ResponseCallback {

    private ActivityMainBinding binding;
    public String ip_address, ipAddress;
    int port;
    public String user_name;
    public String pass_word;
    private AuthUser authUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Handle login button click
        binding.login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ip_address = binding.ipaddress.getText().toString();
                user_name = binding.userName.getText().toString();
                pass_word = binding.passWord.getText().toString();
                if (ip_address.isEmpty() || user_name.isEmpty() || pass_word.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Invalid input. Unfilled fields", Toast.LENGTH_SHORT).show();
                } else {
                    InetSocketAddress address = IPAddressUtils.parseAddress(ip_address);
                    if (address != null) {
                        ipAddress = address.getAddress().getHostAddress();
                        port = address.getPort();
                        if ("0.0.0.0".equals(ipAddress) || port == 0) {
                            Toast.makeText(MainActivity.this, "Incorrect IP. ex:0.0.0.0:0", Toast.LENGTH_SHORT).show();
                        } else {
                            System.out.println("Adresa: " + ip_address);
                            System.out.println("Numar port: " + port);
                            System.out.println("User name: " + user_name);
                            System.out.println("Password hash: " + HashUtils.sha256(pass_word));

                            // Use a fixed client port (e.g., 5050)
                            int clientPort = 5050;
                            authUser = AuthUser.getInstance(ipAddress, port, clientPort);
                            authUser.initializeConnection();

                            // Set the IP address in the AdressIP class
                            AdressIP.setAdresa(ipAddress);

                            String authentication = user_name + "," + HashUtils.sha256(pass_word) + "\n";
                            System.out.println("User name+password: " + authentication);
                            authUser.sendCommand(authentication);
                            // Wait for the connection to initialize
                            ExecutorService executorService = Executors.newSingleThreadExecutor();
                            Future<String> futureResponse = executorService.submit(() -> {
                                try {
                                    Thread.sleep(1000); // Wait for a second to ensure the connection is established
                                    return authUser.readResponse();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    return null;
                                }
                            });

                            executorService.submit(() -> {
                                try {
                                    String response = futureResponse.get(10, TimeUnit.SECONDS); // Wait for 10 seconds for the response
                                    runOnUiThread(() -> {
                                        System.out.println("Validare server: " + response);
                                        if (response != null && response.contains("Authentication successful")) {
                                            Toast.makeText(MainActivity.this, "Authentication successful", Toast.LENGTH_SHORT).show();
                                            // Start new activity or handle success
                                            startActivity(new Intent(MainActivity.this, CarControl.class));
                                            finish(); // Close MainActivity
                                        } else {
                                            Toast.makeText(MainActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } catch (TimeoutException e) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(MainActivity.this, "Connection failed", Toast.LENGTH_SHORT).show();
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Invalid IP address format. Expected format: IP:port", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

       /* binding.forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ResetPassword.class));
            }
        });*/
    }

    @Override
    public void onResponseReceived(String response) {
        //runOnUiThread(() -> responseView.setText(response));
    }
}
