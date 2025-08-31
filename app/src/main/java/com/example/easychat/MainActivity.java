package com.example.easychat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageButton;

import com.example.easychat.model.ChatMessageModel;
import com.example.easychat.model.ChatroomModel;
import com.example.easychat.utils.FirebaseUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    ImageButton addUserBtn;
    ImageButton searchMessageBtn;
    ImageButton broadcastBtn; // Novo Botão
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
        broadcastBtn = findViewById(R.id.main_broadcast_btn); // Referência
        addNewChatBtn = findViewById(R.id.main_add_new_chat_btn);

        addUserBtn.setOnClickListener((v)->{
            startActivity(new Intent(MainActivity.this, SearchUserActivity.class));
        });

        searchMessageBtn.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SearchMessageActivity.class));
        });

        // Lógica para o novo botão de transmissão
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

        migrateMessagesData();
    }

    private List<String> generateKeywordsForMigration(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
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
                                        if (message != null) {
                                            String messageText = message.getMessage();
                                            List<String> newKeywords = generateKeywordsForMigration(messageText);
                                            if (!newKeywords.equals(message.getSearchKeywords())) {
                                                messageDoc.getReference().update("searchKeywords", newKeywords)
                                                        .addOnSuccessListener(aVoid -> Log.d("MsgMigration", "Message prefixes updated: " + messageDoc.getId()))
                                                        .addOnFailureListener(e -> Log.e("MsgMigration", "Error updating message prefixes: " + messageDoc.getId(), e));
                                            }
                                        }
                                    }
                                });
                    }
                    Log.d("MsgMigration", "Message prefix migration check completed.");
                });
    }


    void getFCMToken(){
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                String token = task.getResult();
                FirebaseUtil.currentUserDetails().update("fcmToken",token);
            }
        });
    }
}