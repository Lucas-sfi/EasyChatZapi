package com.example.easychat;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.easychat.model.UserModel;
import com.example.easychat.utils.AndroidUtil;
import com.example.easychat.utils.FirebaseUtil;

public class UserSettingsActivity extends AppCompatActivity {

    private String chatroomId;
    private UserModel otherUser;

    private ImageButton backButton;
    private ImageView profilePicView;
    private TextView usernameView;
    private Button removeContactBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_settings);

        // Inicializar Views
        backButton = findViewById(R.id.back_btn);
        profilePicView = findViewById(R.id.profile_pic_view);
        usernameView = findViewById(R.id.username_view);
        removeContactBtn = findViewById(R.id.remove_contact_btn);

        // Obter dados do Intent
        chatroomId = getIntent().getStringExtra("chatroomId");
        otherUser = AndroidUtil.getUserModelFromIntent(getIntent());

        // Configurar UI
        usernameView.setText(otherUser.getUsername());
        FirebaseUtil.getOtherProfilePicStorageRef(otherUser.getUserId()).getDownloadUrl()
                .addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        Uri uri = t.getResult();
                        AndroidUtil.setProfilePic(this, uri, profilePicView);
                    }
                });

        backButton.setOnClickListener(v -> onBackPressed());

        removeContactBtn.setOnClickListener(v -> showRemoveContactDialog());
    }

    private void showRemoveContactDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Remove Contact")
                .setMessage("Are you sure you want to remove this contact? This will delete the entire conversation.")
                .setPositiveButton("Remove", (dialog, which) -> {
                    deleteChatroom();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteChatroom() {
        FirebaseUtil.getChatroomReference(chatroomId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(UserSettingsActivity.this, "Contact removed successfully", Toast.LENGTH_SHORT).show();
                    // Voltar para a tela principal
                    Intent intent = new Intent(UserSettingsActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UserSettingsActivity.this, "Failed to remove contact", Toast.LENGTH_SHORT).show();
                });
    }
}