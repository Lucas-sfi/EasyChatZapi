package com.example.easychat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageButton;

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

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    ImageButton searchButton;
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
        searchButton = findViewById(R.id.main_search_btn);
        addNewChatBtn = findViewById(R.id.main_add_new_chat_btn);

        searchButton.setOnClickListener((v)->{
            startActivity(new Intent(MainActivity.this,SearchUserActivity.class));
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

        // CHAME A FUNÇÃO DE MIGRAÇÃO AQUI
        migrateUserData();
    }

    // FUNÇÃO DE MIGRAÇÃO TEMPORÁRIA
    void migrateUserData() {
        FirebaseUtil.allUserCollectionReference().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DocumentSnapshot document : task.getResult()) {
                    UserModel user = document.toObject(UserModel.class);
                    // Verifica se o campo de pesquisa está em falta ou nulo
                    if (user != null && user.getSearchUsername() == null) {
                        String username = user.getUsername();
                        if (username != null) {
                            document.getReference().update("searchUsername", username.toLowerCase())
                                    .addOnSuccessListener(aVoid -> Log.d("Migration", "User updated: " + user.getUserId()))
                                    .addOnFailureListener(e -> Log.e("Migration", "Error updating user: " + user.getUserId(), e));
                        }
                    }
                }
                Log.d("Migration", "Data migration check completed.");
            } else {
                Log.e("Migration", "Error getting documents: ", task.getException());
            }
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