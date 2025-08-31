package com.example.easychat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.easychat.model.ChatMessageModel;
import com.example.easychat.utils.FirebaseUtil;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class ComposeBroadcastActivity extends AppCompatActivity {

    private EditText messageInput;
    private ImageButton sendButton;
    private ImageButton backButton;
    private TextView subtitle;
    private ProgressBar progressBar;
    private ArrayList<String> memberIds;
    private int messagesSentCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose_broadcast);

        messageInput = findViewById(R.id.broadcast_message_input);
        sendButton = findViewById(R.id.broadcast_send_btn);
        backButton = findViewById(R.id.back_btn);
        subtitle = findViewById(R.id.broadcast_subtitle);
        progressBar = findViewById(R.id.progress_bar);

        memberIds = getIntent().getStringArrayListExtra("userIds");
        subtitle.setText("Para " + memberIds.size() + " destinatários");

        backButton.setOnClickListener(v -> onBackPressed());
        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (message.isEmpty()) {
            return;
        }
        setInProgress(true);

        for (String userId : memberIds) {
            String chatroomId = FirebaseUtil.getChatroomId(FirebaseUtil.currentUserId(), userId);

            // Criar a mensagem
            ChatMessageModel chatMessageModel = new ChatMessageModel(message, FirebaseUtil.currentUserId(), Timestamp.now(), ChatMessageModel.STATUS_SENT, generateKeywords(message), "text");

            // Enviar a mensagem para a conversa individual
            FirebaseUtil.getChatroomMessageReference(chatroomId).add(chatMessageModel)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Atualizar os dados da última mensagem no chatroom
                            FirebaseUtil.getChatroomReference(chatroomId).update(
                                    "lastMessageTimestamp", Timestamp.now(),
                                    "lastMessageSenderId", FirebaseUtil.currentUserId(),
                                    "lastMessage", message
                            );
                        }
                        checkIfAllMessagesSent();
                    });
        }
    }

    private void checkIfAllMessagesSent() {
        messagesSentCount++;
        if (messagesSentCount == memberIds.size()) {
            setInProgress(false);
            Toast.makeText(this, "Transmissão enviada!", Toast.LENGTH_SHORT).show();

            // Voltar para a tela principal
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private List<String> generateKeywords(String text) {
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

    void setInProgress(boolean inProgress) {
        if (inProgress) {
            progressBar.setVisibility(View.VISIBLE);
            sendButton.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            sendButton.setVisibility(View.VISIBLE);
        }
    }
}