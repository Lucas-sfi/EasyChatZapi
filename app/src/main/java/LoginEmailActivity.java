package com.example.easychat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.easychat.utils.FirebaseUtil;
import com.google.firebase.auth.FirebaseAuth;

public class LoginEmailActivity extends AppCompatActivity {

    EditText emailInput;
    EditText passwordInput;
    Button loginBtn;
    ProgressBar progressBar;
    ImageButton backBtn;
    TextView goToSignUpText;
    TextView forgotPasswordText; // Novo campo
    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_email);

        emailInput = findViewById(R.id.login_email);
        passwordInput = findViewById(R.id.login_password);
        loginBtn = findViewById(R.id.login_email_next_btn);
        progressBar = findViewById(R.id.login_progress_bar);
        backBtn = findViewById(R.id.back_btn);
        goToSignUpText = findViewById(R.id.go_to_signup_text);
        forgotPasswordText = findViewById(R.id.forgot_password_text); // Referência

        firebaseAuth = FirebaseAuth.getInstance();

        backBtn.setOnClickListener(v -> onBackPressed());
        loginBtn.setOnClickListener(v -> handleLogin());
        goToSignUpText.setOnClickListener(v ->
                startActivity(new Intent(LoginEmailActivity.this, SignUpEmailActivity.class))
        );

        // Lógica para o "Esqueceu a senha?"
        forgotPasswordText.setOnClickListener(v -> showRecoveryDialog());
    }

    void showRecoveryDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Recuperação de Conta")
                .setMessage("Para redefinir sua senha, você precisará fazer login com o número de telefone associado a esta conta. Deseja continuar?")
                .setPositiveButton("Continuar", (dialog, which) -> {
                    // Redireciona para o login com telefone
                    Intent intent = new Intent(LoginEmailActivity.this, LoginPhoneNumberActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    void handleLogin() {
        String email = emailInput.getText().toString();
        String password = passwordInput.getText().toString();

        if (!validateData(email, password)) {
            return;
        }

        setInProgress(true);
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    setInProgress(false);
                    if (task.isSuccessful()) {
                        FirebaseUtil.currentUserDetails().get().addOnCompleteListener(docTask -> {
                            if (docTask.isSuccessful() && docTask.getResult().exists()) {
                                Intent intent = new Intent(LoginEmailActivity.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            } else {
                                Intent intent = new Intent(LoginEmailActivity.this, LoginUsernameActivity.class);
                                intent.putExtra("email", email);
                                startActivity(intent);
                            }
                            finish();
                        });
                    } else {
                        Toast.makeText(this, task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    boolean validateData(String email, String password) {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("E-mail inválido");
            return false;
        }
        if (password.length() < 6) {
            passwordInput.setError("A senha deve ter pelo menos 6 caracteres");
            return false;
        }
        return true;
    }

    void setInProgress(boolean inProgress) {
        if (inProgress) {
            progressBar.setVisibility(View.VISIBLE);
            loginBtn.setVisibility(View.GONE);
            goToSignUpText.setVisibility(View.GONE);
            forgotPasswordText.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            loginBtn.setVisibility(View.VISIBLE);
            goToSignUpText.setVisibility(View.VISIBLE);
            forgotPasswordText.setVisibility(View.VISIBLE);
        }
    }
}