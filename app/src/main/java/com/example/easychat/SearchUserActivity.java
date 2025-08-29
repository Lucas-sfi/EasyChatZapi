package com.example.easychat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;

import com.example.easychat.adapter.SearchUserRecyclerAdapter;
import com.example.easychat.model.ChatroomModel;
import com.example.easychat.model.UserModel;
import com.example.easychat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class SearchUserActivity extends AppCompatActivity {

    EditText searchInput;
    ImageButton searchButton;
    ImageButton backButton;
    RecyclerView recyclerView;

    SearchUserRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_user);

        searchInput = findViewById(R.id.seach_username_input);
        searchButton = findViewById(R.id.search_user_btn);
        backButton = findViewById(R.id.back_btn);
        recyclerView = findViewById(R.id.search_user_recycler_view);

        searchInput.requestFocus();

        backButton.setOnClickListener(v -> onBackPressed());

        searchButton.setOnClickListener(v -> {
            String searchTerm = searchInput.getText().toString().trim();
            if(searchTerm.isEmpty() || searchTerm.length() < 3){
                searchInput.setError("Invalid Username");
                return;
            }
            getContactIdsAndSetupRecyclerView(searchTerm.toLowerCase());
        });
    }

    void getContactIdsAndSetupRecyclerView(String searchTerm) {
        FirebaseUtil.allChatroomCollectionReference()
                .whereArrayContains("userIds", FirebaseUtil.currentUserId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> contactIds = new ArrayList<>();
                    for (ChatroomModel chatroom : queryDocumentSnapshots.toObjects(ChatroomModel.class)) {
                        for (String userId : chatroom.getUserIds()) {
                            if (!userId.equals(FirebaseUtil.currentUserId())) {
                                contactIds.add(userId);
                            }
                        }
                    }
                    setupSearchRecyclerView(searchTerm, contactIds);
                });
    }

    void setupSearchRecyclerView(String searchTerm, List<String> contactIds){
        // QUERY CORRIGIDA para usar o novo campo
        Query query = FirebaseUtil.allUserCollectionReference()
                .whereGreaterThanOrEqualTo("searchUsername", searchTerm)
                .whereLessThanOrEqualTo("searchUsername", searchTerm + '\uf8ff');

        FirestoreRecyclerOptions<UserModel> options = new FirestoreRecyclerOptions.Builder<UserModel>()
                .setQuery(query, UserModel.class).build();

        adapter = new SearchUserRecyclerAdapter(options, getApplicationContext(), contactIds);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(adapter != null)
            adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(adapter != null)
            adapter.stopListening();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(adapter != null)
            adapter.startListening();
    }
}