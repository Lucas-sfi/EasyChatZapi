package com.example.easychat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import com.example.easychat.adapter.SelectUserRecyclerAdapter;
import com.example.easychat.model.UserModel;
import com.example.easychat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;

public class SelectGroupMembersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SelectUserRecyclerAdapter adapter;
    private ImageButton backButton;
    private FloatingActionButton nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_group_members);

        recyclerView = findViewById(R.id.recycler_view);
        backButton = findViewById(R.id.back_btn);
        nextButton = findViewById(R.id.next_btn);

        setupRecyclerView();

        backButton.setOnClickListener(v -> onBackPressed());

        nextButton.setOnClickListener(v -> {
            ArrayList<String> selectedIds = adapter.getSelectedUserIds();
            // Para um grupo, precisamos de pelo menos 1 outro membro.
            if (selectedIds.isEmpty()) {
                Toast.makeText(this, "Select at least 1 member", Toast.LENGTH_SHORT).show();
                return;
            }
            // A lógica agora inicia a CreateGroupActivity
            Intent intent = new Intent(this, CreateGroupActivity.class);
            intent.putStringArrayListExtra("userIds", selectedIds);
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        Query query = FirebaseUtil.allUserCollectionReference()
                .whereNotEqualTo("userId", FirebaseUtil.currentUserId());

        FirestoreRecyclerOptions<UserModel> options = new FirestoreRecyclerOptions.Builder<UserModel>()
                .setQuery(query, UserModel.class).build();

        adapter = new SelectUserRecyclerAdapter(options, getApplicationContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
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