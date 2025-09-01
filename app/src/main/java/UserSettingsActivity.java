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
import com.google.firebase.firestore.DocumentSnapshot;
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
        // Passo 1 (CRÍTICO): Deletar o documento do chatroom.
        // Isso garante que ele desapareça imediatamente da lista de chats do usuário.
        FirebaseUtil.getChatroomReference(chatroomId).delete()
                .addOnSuccessListener(aVoid -> {
                    // Se a exclusão principal for bem-sucedida, realize as tarefas de limpeza.

                    // Limpeza 1: Deletar a subcoleção de mensagens.
                    deleteChatMessages();

                    // Limpeza 2: Remover os usuários das listas de contatos um do outro.
                    removeContactFromUserList(FirebaseUtil.currentUserId(), otherUser.getUserId());
                    removeContactFromUserList(otherUser.getUserId(), FirebaseUtil.currentUserId());

                    Toast.makeText(UserSettingsActivity.this, "Contact removed successfully", Toast.LENGTH_SHORT).show();
                    goToMainActivity();
                })
                .addOnFailureListener(e -> {
                    // Se a exclusão principal falhar, informe o usuário e não faça mais nada.
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
                    // Não precisamos esperar por isso para continuar, pois o chat já sumiu da lista.
                    batch.commit();
                });
    }

    private void removeContactFromUserList(String userId, String contactIdToRemove) {
        if (userId == null || contactIdToRemove == null) return;
        DocumentReference userDocRef = FirebaseUtil.allUserCollectionReference().document(userId);

        // Executa a atualização da lista de contatos em segundo plano.
        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                UserModel user = documentSnapshot.toObject(UserModel.class);
                if (user != null && user.getContacts() != null && user.getContacts().contains(contactIdToRemove)) {
                    user.getContacts().remove(contactIdToRemove);
                    userDocRef.set(user); // Atualiza o documento do usuário
                }
            }
        });
    }

    private void goToMainActivity(){
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}