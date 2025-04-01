package com.example.interface_car_control;

public class AdressIP {
    // store IP address
    private static String adresa;

    // set IP address
    public static void setAdresa(String adresa) {
        AdressIP.adresa = adresa;
    }

    // get IP address
    public static String getAdresa() {
        return adresa;
    }
}
