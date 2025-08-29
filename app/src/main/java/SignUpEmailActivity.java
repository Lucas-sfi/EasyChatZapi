package com.example.easychat;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SignUpEmailActivity extends AppCompatActivity {

    EditText emailInput;
    EditText passwordInput;
    EditText confirmPasswordInput;
    Button createAccountBtn;
    ProgressBar progressBar;
    ImageButton backBtn;
    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_email);

        emailInput = findViewById(R.id.signup_email);
        passwordInput = findViewById(R.id.signup_password);
        confirmPasswordInput = findViewById(R.id.signup_confirm_password);
        createAccountBtn = findViewById(R.id.signup_create_account_btn);
        progressBar = findViewById(R.id.signup_progress_bar);
        backBtn = findViewById(R.id.back_btn);

        firebaseAuth = FirebaseAuth.getInstance();

        backBtn.setOnClickListener(v -> onBackPressed());
        createAccountBtn.setOnClickListener(v -> handleSignUp());
    }

    void handleSignUp() {
        String email = emailInput.getText().toString();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        if (!validateData(email, password, confirmPassword)) {
            return;
        }

        setInProgress(true);
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    setInProgress(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Cadastro realizado com sucesso! Faça o login.", Toast.LENGTH_LONG).show();
                        firebaseAuth.signOut();
                        finish(); // Fecha a tela de cadastro e volta para a de login
                    } else {
                        Toast.makeText(this, task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    boolean validateData(String email, String password, String confirmPassword) {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("E-mail inválido");
            return false;
        }
        if (password.length() < 6) {
            passwordInput.setError("A senha deve ter pelo menos 6 caracteres");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("As senhas não coincidem");
            return false;
        }
        return true;
    }

    void setInProgress(boolean inProgress) {
        if (inProgress) {
            progressBar.setVisibility(View.VISIBLE);
            createAccountBtn.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            createAccountBtn.setVisibility(View.VISIBLE);
        }
    }
}