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
import com.example.easychat.model.UserModel;
import com.example.easychat.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    ImageButton addUserBtn;
    ImageButton searchMessageBtn;
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
        addNewChatBtn = findViewById(R.id.main_add_new_chat_btn);

        addUserBtn.setOnClickListener((v)->{
            startActivity(new Intent(MainActivity.this, SearchUserActivity.class));
        });

        searchMessageBtn.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SearchMessageActivity.class));
        });

        addNewChatBtn.setOnClickListener((v) -> {
            startActivity(new Intent(MainActivity.this, SelectGroupMembersActivity.class));
        });

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(item.getItemId()==R.id.menu_chat){
                    getSupportFragmentManager().beginTransaction().replace(R.id.main_frame_layout,chatFragment).commit();
                }
                if(item.getItemId()==R.id.menu_profile){
                    getSupportFragmentManager().beginTransaction().replace(R.id.main_frame_layout,profileFragment).commit();
                }
                return true;
            }
        });
        bottomNavigationView.setSelectedItemId(R.id.menu_chat);

        getFCMToken();

        // CHAME A NOVA FUNÇÃO DE MIGRAÇÃO AQUI
        migrateMessagesData();
    }

    // FUNÇÃO DE MIGRAÇÃO TEMPORÁRIA PARA MENSAGENS
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
                                        if (message != null && message.getSearchKeywords() == null) {
                                            String messageText = message.getMessage();
                                            if (messageText != null && !messageText.isEmpty()) {
                                                String searchableString = messageText.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "");
                                                List<String> keywords = Arrays.asList(searchableString.split("\\s+"));
                                                messageDoc.getReference().update("searchKeywords", keywords)
                                                        .addOnSuccessListener(aVoid -> Log.d("MsgMigration", "Message updated: " + messageDoc.getId()))
                                                        .addOnFailureListener(e -> Log.e("MsgMigration", "Error updating message: " + messageDoc.getId(), e));
                                            }
                                        }
                                    }
                                });
                    }
                    Log.d("MsgMigration", "Message migration check completed.");
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