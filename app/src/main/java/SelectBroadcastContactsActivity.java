package com.example.easychat;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easychat.adapter.SelectUserRecyclerAdapter;
import com.example.easychat.model.ChatroomModel;
import com.example.easychat.model.UserModel;
import com.example.easychat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class SelectBroadcastContactsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SelectUserRecyclerAdapter adapter;
    private ImageButton backButton;
    private FloatingActionButton nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_broadcast_contacts);

        recyclerView = findViewById(R.id.recycler_view);
        backButton = findViewById(R.id.back_btn);
        nextButton = findViewById(R.id.next_btn);

        setupRecyclerView();

        backButton.setOnClickListener(v -> onBackPressed());

        nextButton.setOnClickListener(v -> {
            if (adapter == null) {
                Toast.makeText(this, "Nenhum contato selecionado.", Toast.LENGTH_SHORT).show();
                return;
            }
            ArrayList<String> selectedIds = adapter.getSelectedUserIds();
            if (selectedIds.size() < 2) {
                Toast.makeText(this, "Selecione pelo menos 2 contatos", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, ComposeBroadcastActivity.class);
            intent.putStringArrayListExtra("userIds", selectedIds);
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        FirebaseUtil.allChatroomCollectionReference()
                .whereArrayContains("userIds", FirebaseUtil.currentUserId())
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> contactIds = new ArrayList<>();
                        for (ChatroomModel chatroom : task.getResult().toObjects(ChatroomModel.class)) {
                            // Garante que estamos pegando contatos apenas de conversas individuais
                            if (!chatroom.isGroupChat()) {
                                for (String userId : chatroom.getUserIds()) {
                                    if (!userId.equals(FirebaseUtil.currentUserId())) {
                                        contactIds.add(userId);
                                    }
                                }
                            }
                        }

                        if (!contactIds.isEmpty()) {
                            Query query = FirebaseUtil.allUserCollectionReference()
                                    .whereIn("userId", contactIds);

                            FirestoreRecyclerOptions<UserModel> options = new FirestoreRecyclerOptions.Builder<UserModel>()
                                    .setQuery(query, UserModel.class).build();

                            // Passamos um new ArrayList<>() porque na lista de transmissão não há "membros atuais" a serem desabilitados
                            adapter = new SelectUserRecyclerAdapter(options, getApplicationContext(), new ArrayList<>());
                            recyclerView.setLayoutManager(new LinearLayoutManager(this));
                            recyclerView.setAdapter(adapter);
                            adapter.startListening();
                        } else {
                            Toast.makeText(this, "Você não tem contatos para criar uma lista de transmissão.", Toast.LENGTH_LONG).show();
                        }
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