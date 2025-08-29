package com.example.easychat;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    Button loginPhoneButton;
    Button loginEmailButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginPhoneButton = findViewById(R.id.login_phone_button);
        loginEmailButton = findViewById(R.id.login_email_button);

        loginPhoneButton.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, LoginPhoneNumberActivity.class));
        });

        loginEmailButton.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, LoginEmailActivity.class));
        });
    }
}