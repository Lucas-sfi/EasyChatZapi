package com.example.easychat;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.easychat.adapter.MessageSearchAdapter;
import com.example.easychat.model.ChatMessageModel;
import com.example.easychat.model.ChatroomModel;
import com.example.easychat.utils.FirebaseUtil;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

public class SearchMessageActivity extends AppCompatActivity {

    private EditText searchInput;
    private ImageButton searchButton;
    private ImageButton backButton;
    private RecyclerView recyclerView;
    private MessageSearchAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_message);

        searchInput = findViewById(R.id.search_message_input);
        searchButton = findViewById(R.id.search_message_btn);
        backButton = findViewById(R.id.back_btn);
        recyclerView = findViewById(R.id.search_message_recycler_view);

        searchInput.requestFocus();
        backButton.setOnClickListener(v -> onBackPressed());

        searchButton.setOnClickListener(v -> {
            String searchTerm = searchInput.getText().toString().trim();
            if (searchTerm.isEmpty()) return;
            searchMessages(searchTerm.toLowerCase());
        });
    }

    private void searchMessages(String searchTerm) {
        FirebaseUtil.allChatroomCollectionReference()
                .whereArrayContains("userIds", FirebaseUtil.currentUserId())
                .get()
                .addOnSuccessListener(chatroomSnapshots -> {
                    List<Task<QuerySnapshot>> tasks = new ArrayList<>();
                    List<ChatroomModel> chatroomModels = chatroomSnapshots.toObjects(ChatroomModel.class);

                    Log.d("SearchMessage", "Searching in " + chatroomModels.size() + " chatrooms.");

                    for (ChatroomModel chatroom : chatroomModels) {
                        Task<QuerySnapshot> task = FirebaseUtil.getChatroomMessageReference(chatroom.getChatroomId())
                                .whereArrayContains("searchKeywords", searchTerm)
                                .get();
                        tasks.add(task);
                    }

                    Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                        List<ChatMessageModel> foundMessages = new ArrayList<>();
                        List<ChatroomModel> correspondingChatrooms = new ArrayList<>();

                        for (int i = 0; i < results.size(); i++) {
                            QuerySnapshot messageSnapshots = (QuerySnapshot) results.get(i);
                            for (ChatMessageModel message : messageSnapshots.toObjects(ChatMessageModel.class)) {
                                foundMessages.add(message);
                                correspondingChatrooms.add(chatroomModels.get(i));
                            }
                        }

                        Log.d("SearchMessage", "Found " + foundMessages.size() + " messages.");

                        // ADICIONAR FEEDBACK SE NENHUM RESULTADO FOR ENCONTRADO
                        if (foundMessages.isEmpty()) {
                            Toast.makeText(this, "No messages found for '" + searchTerm + "'", Toast.LENGTH_LONG).show();
                        }

                        adapter = new MessageSearchAdapter(getApplicationContext(), foundMessages, correspondingChatrooms);
                        recyclerView.setLayoutManager(new LinearLayoutManager(this));
                        recyclerView.setAdapter(adapter);
                    });
                });
    }
}