package com.example.interface_car_control;

import android.widget.Toast;

import java.net.InetSocketAddress;
import java.util.regex.Pattern;

public class IPAddressUtils {

    private static final Pattern IP_PORT_PATTERN = Pattern.compile(
            "^(([0-9]{1,3})\\.){3}([0-9]{1,3}):(\\d{1,5})$"
    );



    public static InetSocketAddress parseAddress(String address) {
        if (!isValidIPPort(address)) {
            return new InetSocketAddress("0.0.0.0", 0);
        }

        String[] parts = address.split(":");
        String ip = parts[0];
        int port = Integer.parseInt(parts[1]);

        return new InetSocketAddress(ip, port);
    }

    private static boolean isValidIPPort(String address) {
        if (!IP_PORT_PATTERN.matcher(address).matches()) {
            return false;
        }

        String[] parts = address.split(":");
        String ip = parts[0];
        int port = Integer.parseInt(parts[1]);

        if (!isValidIP(ip) || !isValidPort(port)) {
            return false;
        }
        System.out.println("Port:" + ip);
        System.out.println("Port:" + port);

        return true;
    }

    private static boolean isValidIP(String ip) {
        String[] parts = ip.split("\\.");
        for (String part : parts) {
            int num = Integer.parseInt(part);
            if (num < 0 || num > 255) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidPort(int port) {
        return port >= 0 && port <= 65535;
    }

}
