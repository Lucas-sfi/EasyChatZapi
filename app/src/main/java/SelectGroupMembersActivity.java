package com.example.easychat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import com.example.easychat.adapter.SelectUserRecyclerAdapter;
import com.example.easychat.model.ChatroomModel;
import com.example.easychat.model.UserModel;
import com.example.easychat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class SelectGroupMembersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SelectUserRecyclerAdapter adapter;
    private ImageButton backButton;
    private FloatingActionButton nextButton;
    private ArrayList<String> currentMembers;
    private String chatroomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_group_members);

        recyclerView = findViewById(R.id.recycler_view);
        backButton = findViewById(R.id.back_btn);
        nextButton = findViewById(R.id.next_btn);

        currentMembers = getIntent().getStringArrayListExtra("currentMembers");
        chatroomId = getIntent().getStringExtra("chatroomId");

        if (currentMembers == null) {
            currentMembers = new ArrayList<>();
        }

        setupRecyclerView();

        backButton.setOnClickListener(v -> onBackPressed());

        nextButton.setOnClickListener(v -> {
            ArrayList<String> selectedIds = adapter.getSelectedUserIds();
            if (selectedIds.isEmpty()) {
                Toast.makeText(this, "Select at least 1 member to add", Toast.LENGTH_SHORT).show();
                return;
            }

            if (chatroomId != null) {
                addMembersToGroup(selectedIds);
            } else {
                Intent intent = new Intent(this, CreateGroupActivity.class);
                intent.putStringArrayListExtra("userIds", selectedIds);
                startActivity(intent);
            }
        });
    }

    private void addMembersToGroup(ArrayList<String> newMemberIds) {
        FirebaseUtil.getChatroomReference(chatroomId).get().addOnSuccessListener(documentSnapshot -> {
            ChatroomModel chatroomModel = documentSnapshot.toObject(ChatroomModel.class);
            if (chatroomModel != null) {
                List<String> updatedUserIds = chatroomModel.getUserIds();
                updatedUserIds.addAll(newMemberIds);
                chatroomModel.setUserIds(updatedUserIds);

                FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel).addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Members added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void setupRecyclerView() {
        FirebaseUtil.currentUserDetails().get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                UserModel currentUser = task.getResult().toObject(UserModel.class);
                if (currentUser != null && currentUser.getContacts() != null && !currentUser.getContacts().isEmpty()) {
                    List<String> contactIds = currentUser.getContacts();

                    Query query = FirebaseUtil.allUserCollectionReference()
                            .whereIn("userId", contactIds);

                    FirestoreRecyclerOptions<UserModel> options = new FirestoreRecyclerOptions.Builder<UserModel>()
                            .setQuery(query, UserModel.class).build();

                    adapter = new SelectUserRecyclerAdapter(options, getApplicationContext(), currentMembers);
                    recyclerView.setLayoutManager(new LinearLayoutManager(this));
                    recyclerView.setAdapter(adapter);
                    adapter.startListening();
                } else {
                    Toast.makeText(this, "Você não tem contatos para adicionar.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Não foi possível carregar seus contatos.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}