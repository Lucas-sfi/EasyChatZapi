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
import com.example.easychat.model.ChatroomModel;
import com.example.easychat.model.UserModel;
import com.example.easychat.utils.AndroidUtil;
import com.example.easychat.utils.FirebaseUtil;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.Map;

public class UserSettingsActivity extends AppCompatActivity {

    private String chatroomId;
    private UserModel otherUser;
    private ChatroomModel chatroomModel;

    private ImageButton backButton;
    private ImageView profilePicView;
    private TextView usernameView;
    private Button removeContactBtn;
    private SwitchMaterial notificationSwitch; // Novo Switch

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_settings);

        backButton = findViewById(R.id.back_btn);
        profilePicView = findViewById(R.id.profile_pic_view);
        usernameView = findViewById(R.id.username_view);
        removeContactBtn = findViewById(R.id.remove_contact_btn);
        notificationSwitch = findViewById(R.id.notification_switch); // Referência

        chatroomId = getIntent().getStringExtra("chatroomId");
        otherUser = AndroidUtil.getUserModelFromIntent(getIntent());

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

        getChatroomDetails();
    }

    private void getChatroomDetails() {
        FirebaseUtil.getChatroomReference(chatroomId).get().addOnSuccessListener(documentSnapshot -> {
            chatroomModel = documentSnapshot.toObject(ChatroomModel.class);
            if (chatroomModel != null) {
                // Configurar o estado inicial do Switch
                boolean isEnabled = chatroomModel.getCustomNotificationStatus()
                        .getOrDefault(FirebaseUtil.currentUserId(), false);
                notificationSwitch.setChecked(isEnabled);

                // Configurar o listener para salvar as alterações
                notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    updateNotificationStatus(isChecked);
                });
            }
        });
    }

    private void updateNotificationStatus(boolean isEnabled) {
        if (chatroomModel != null) {
            Map<String, Boolean> statusMap = chatroomModel.getCustomNotificationStatus();
            statusMap.put(FirebaseUtil.currentUserId(), isEnabled);
            chatroomModel.setCustomNotificationStatus(statusMap);

            FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);
        }
    }

    private void showRemoveContactDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Remove Contact")
                .setMessage("Are you sure you want to remove this contact? This will delete the entire conversation.")
                .setPositiveButton("Remove", (dialog, which) -> deleteChatroom())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteChatroom() {
        FirebaseUtil.getChatroomReference(chatroomId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Contact removed successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to remove contact", Toast.LENGTH_SHORT).show();
                });
    }
}