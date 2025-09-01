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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.Map;

public class UserSettingsActivity extends AppCompatActivity {

    private String chatroomId;
    private UserModel otherUser;
    private ChatroomModel chatroomModel;

    private ImageButton backButton;
    private ImageView profilePicView;
    private TextView usernameView;
    private Button removeContactBtn;
    private SwitchMaterial notificationSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_settings);

        backButton = findViewById(R.id.back_btn);
        profilePicView = findViewById(R.id.profile_pic_view);
        usernameView = findViewById(R.id.username_view);
        removeContactBtn = findViewById(R.id.remove_contact_btn);
        notificationSwitch = findViewById(R.id.notification_switch);

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
            if (documentSnapshot.exists()) {
                chatroomModel = documentSnapshot.toObject(ChatroomModel.class);
                if (chatroomModel != null) {
                    boolean isEnabled = chatroomModel.getCustomNotificationStatus()
                            .getOrDefault(FirebaseUtil.currentUserId(), false);
                    notificationSwitch.setChecked(isEnabled);
                    notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        updateNotificationStatus(isChecked);
                    });
                }
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
                .setMessage("Are you sure you want to remove this contact? This will delete the entire conversation and remove them from your contacts.")
                .setPositiveButton("Remove", (dialog, which) -> deleteContactAndChatroom())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteContactAndChatroom() {
        // Usa um WriteBatch para garantir que todas as operações sejam atômicas
        WriteBatch batch = FirebaseUtil.getFirestore().batch();

        // 1. Adiciona a exclusão do chatroom ao lote
        DocumentReference chatroomRef = FirebaseUtil.getChatroomReference(chatroomId);
        batch.delete(chatroomRef);

        // 2. Adiciona a remoção do contato da lista do usuário atual ao lote
        DocumentReference currentUserRef = FirebaseUtil.allUserCollectionReference().document(FirebaseUtil.currentUserId());
        batch.update(currentUserRef, "contacts", FieldValue.arrayRemove(otherUser.getUserId()));

        // 3. Adiciona a remoção do usuário atual da lista do outro contato ao lote
        DocumentReference otherUserRef = FirebaseUtil.allUserCollectionReference().document(otherUser.getUserId());
        batch.update(otherUserRef, "contacts", FieldValue.arrayRemove(FirebaseUtil.currentUserId()));

        // Executa o lote de operações
        batch.commit().addOnSuccessListener(aVoid -> {
            // Se o lote for bem-sucedido, as operações principais foram concluídas
            deleteChatMessages(); // Executa a limpeza das mensagens em segundo plano

            Toast.makeText(UserSettingsActivity.this, "Contact removed successfully", Toast.LENGTH_SHORT).show();
            goToMainActivity();
        }).addOnFailureListener(e -> {
            Toast.makeText(UserSettingsActivity.this, "Failed to remove contact. Please try again.", Toast.LENGTH_SHORT).show();
        });
    }

    private void deleteChatMessages() {
        FirebaseUtil.getChatroomMessageReference(chatroomId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = FirebaseUtil.getFirestore().batch();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        batch.delete(doc.getReference());
                    }
                    // Executa a exclusão das mensagens em segundo plano.
                    // Não precisamos esperar por isso para continuar.
                    batch.commit();
                });
    }

    private void goToMainActivity(){
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}