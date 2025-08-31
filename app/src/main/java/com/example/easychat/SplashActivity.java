package com.example.easychat;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.example.easychat.model.UserModel;
import com.example.easychat.utils.AndroidUtil;
import com.example.easychat.utils.FirebaseUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (getIntent().getExtras() != null && getIntent().getExtras().getString("userId") != null) {
            String userId = getIntent().getExtras().getString("userId");
            FirebaseUtil.allUserCollectionReference().document(userId).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            UserModel model = task.getResult().toObject(UserModel.class);
                            if (model != null) {
                                Intent mainIntent = new Intent(this, MainActivity.class);
                                mainIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                startActivity(mainIntent);

                                Intent chatIntent = new Intent(this, ChatActivity.class);
                                AndroidUtil.passUserModelAsIntent(chatIntent, model);
                                chatIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(chatIntent);
                                finish();
                            }
                        }
                    });
        } else {
            new Handler().postDelayed(() -> {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                } else {
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                }
                finish();
            }, 1500); // Aumentado para 1.5 segundos para garantir a inicialização
        }
    }
}