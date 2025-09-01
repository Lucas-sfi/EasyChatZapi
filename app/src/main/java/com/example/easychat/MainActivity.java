package com.example.easychat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;

import com.example.easychat.model.ChatMessageModel;
import com.example.easychat.model.ChatroomModel;
import com.example.easychat.model.UserModel;
import com.example.easychat.utils.AndroidUtil;
import com.example.easychat.utils.FirebaseUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    ImageButton addUserBtn;
    ImageButton searchMessageBtn;
    ImageButton broadcastBtn;
    FloatingActionButton addNewChatBtn;

    ChatFragment chatFragment;
    ProfileFragment profileFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chatFragment = new ChatFragment();
        profileFragment = new ProfileFragment();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        addUserBtn = findViewById(R.id.main_add_user_btn);
        searchMessageBtn = findViewById(R.id.main_search_message_btn);
        broadcastBtn = findViewById(R.id.main_broadcast_btn);
        addNewChatBtn = findViewById(R.id.main_add_new_chat_btn);

        addUserBtn.setOnClickListener((v)->{
            startActivity(new Intent(MainActivity.this, SearchUserActivity.class));
        });

        searchMessageBtn.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SearchMessageActivity.class));
        });

        broadcastBtn.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SelectBroadcastContactsActivity.class));
        });

        addNewChatBtn.setOnClickListener((v) -> {
            startActivity(new Intent(MainActivity.this, SelectGroupMembersActivity.class));
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if(item.getItemId()==R.id.menu_chat){
                getSupportFragmentManager().beginTransaction().replace(R.id.main_frame_layout,chatFragment).commit();
            }
            if(item.getItemId()==R.id.menu_profile){
                getSupportFragmentManager().beginTransaction().replace(R.id.main_frame_layout,profileFragment).commit();
            }
            return true;
        });
        bottomNavigationView.setSelectedItemId(R.id.menu_chat);

        getFCMToken();

        // Ponto chave: Lógica para redirecionar se a MainActivity for aberta por uma notificação
        handleNotificationIntent();
    }

    private void handleNotificationIntent() {
        if (getIntent().getExtras() != null && getIntent().hasExtra("chatroomId")) {
            String chatroomId = getIntent().getStringExtra("chatroomId");
            String userId = getIntent().getStringExtra("userId");
            String username = getIntent().getStringExtra("username");

            // Recria o UserModel a partir dos dados da Intent para passar para a ChatActivity
            UserModel userModel = new UserModel();
            userModel.setUserId(userId);
            userModel.setUsername(username);

            // Precisamos buscar o token FCM e o telefone, pois eles não vêm na notificação
            FirebaseUtil.allUserCollectionReference().document(userId).get().addOnSuccessListener(documentSnapshot -> {
                UserModel fullUserModel = documentSnapshot.toObject(UserModel.class);
                if (fullUserModel != null) {
                    Intent chatIntent = new Intent(this, ChatActivity.class);
                    AndroidUtil.passUserModelAsIntent(chatIntent, fullUserModel);
                    chatIntent.putExtra("chatroomId", chatroomId);
                    chatIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(chatIntent);
                }
            });
        }
    }

    void getFCMToken(){
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                String token = task.getResult();
                FirebaseUtil.currentUserDetails().update("fcmToken",token);
            }
        });
    }

    // A função de migração pode ser removida se já foi executada uma vez
    void migrateMessagesData() {
        FirebaseUtil.allChatroomCollectionReference()
                .whereArrayContains("userIds", FirebaseUtil.currentUserId())
                .get()
                .addOnSuccessListener(chatroomSnapshots -> {
                    for (ChatroomModel chatroom : chatroomSnapshots.toObjects(ChatroomModel.class)) {
                        FirebaseUtil.getChatroomMessageReference(chatroom.getChatroomId()).get()
                                .addOnSuccessListener(messageSnapshots -> {
                                    for (DocumentSnapshot messageDoc : messageSnapshots.getDocuments()) {
                                        ChatMessageModel message = messageDoc.toObject(ChatMessageModel.class);
                                        if (message != null && message.getSearchKeywords() == null) { // Migra apenas se for nulo
                                            String messageText = message.getMessage();
                                            List<String> newKeywords = generateKeywordsForMigration(messageText);
                                            messageDoc.getReference().update("searchKeywords", newKeywords);
                                        }
                                    }
                                });
                    }
                });
    }

    private List<String> generateKeywordsForMigration(String text) {
        if (text == null || text.isEmpty()) return new ArrayList<>();
        String searchableString = text.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "");
        List<String> keywords = new ArrayList<>();
        for (String word : searchableString.split("\\s+")) {
            if (word.length() > 0) {
                for (int i = 1; i <= word.length(); i++) {
                    keywords.add(word.substring(0, i));
                }
            }
        }
        return keywords;
    }
}