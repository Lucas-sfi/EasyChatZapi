package com.example.easychat;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.example.easychat.model.ChatroomModel;
import com.example.easychat.model.UserModel;
import com.example.easychat.utils.AndroidUtil;
import com.example.easychat.utils.FirebaseUtil;
import com.google.firebase.Timestamp;
import java.util.ArrayList;

public class CreateGroupActivity extends AppCompatActivity {

    private EditText groupNameInput;
    private Button createGroupBtn;
    private ProgressBar progressBar;
    private ArrayList<String> memberIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        groupNameInput = findViewById(R.id.group_name_input);
        createGroupBtn = findViewById(R.id.create_group_btn);
        progressBar = findViewById(R.id.progress_bar);

        memberIds = getIntent().getStringArrayListExtra("userIds");

        createGroupBtn.setOnClickListener(v -> createGroup());
    }

    private void createGroup() {
        String groupName = groupNameInput.getText().toString().trim();
        if (groupName.isEmpty() || groupName.length() < 3) {
            groupNameInput.setError("Group name must be at least 3 characters");
            return;
        }
        setInProgress(true);

        // Adicionar o usuário atual à lista de membros
        memberIds.add(FirebaseUtil.currentUserId());

        // Criar um ID único para a sala de bate-papo do grupo
        String chatroomId = FirebaseUtil.allChatroomCollectionReference().document().getId();

        ChatroomModel chatroomModel = new ChatroomModel(
                chatroomId,
                memberIds,
                Timestamp.now(),
                "",
                groupName,
                true // Marcando como chat em grupo
        );

        FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel)
                .addOnCompleteListener(task -> {
                    setInProgress(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(CreateGroupActivity.this, "Group created successfully", Toast.LENGTH_SHORT).show();
                        // Redirecionar para a tela principal
                        Intent intent = new Intent(CreateGroupActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(CreateGroupActivity.this, "Failed to create group", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    void setInProgress(boolean inProgress) {
        if (inProgress) {
            progressBar.setVisibility(View.VISIBLE);
            createGroupBtn.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            createGroupBtn.setVisibility(View.VISIBLE);
        }
    }
}