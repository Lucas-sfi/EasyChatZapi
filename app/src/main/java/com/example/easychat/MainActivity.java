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

        handleNotificationIntent();
    }

    private void handleNotificationIntent() {
        if (getIntent().getExtras() != null && getIntent().hasExtra("chatroomId")) {
            String chatroomId = getIntent().getStringExtra("chatroomId");
            boolean isGroupChat = getIntent().getBooleanExtra("isGroupChat", false);

            Intent chatIntent = new Intent(this, ChatActivity.class);
            chatIntent.putExtra("chatroomId", chatroomId);
            chatIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (isGroupChat) {
                chatIntent.putExtra("isGroupChat", true);
                chatIntent.putExtra("groupName", getIntent().getStringExtra("groupName"));
                startActivity(chatIntent);
            } else {
                String userId = getIntent().getStringExtra("userId");
                FirebaseUtil.allUserCollectionReference().document(userId).get().addOnSuccessListener(documentSnapshot -> {
                    UserModel userModel = documentSnapshot.toObject(UserModel.class);
                    if (userModel != null) {
                        AndroidUtil.passUserModelAsIntent(chatIntent, userModel);
                        startActivity(chatIntent);
                    }
                });
            }
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
}