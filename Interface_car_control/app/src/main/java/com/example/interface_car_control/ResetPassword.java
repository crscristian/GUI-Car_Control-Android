package com.example.interface_car_control;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import com.example.interface_car_control.databinding.ActivityResetPasswordBinding;

public class ResetPassword extends AppCompatActivity {

    private  ActivityResetPasswordBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}