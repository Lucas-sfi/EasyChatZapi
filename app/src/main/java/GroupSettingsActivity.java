package com.example.easychat;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.easychat.adapter.GroupMemberRecyclerAdapter;
import com.example.easychat.model.ChatroomModel;
import com.example.easychat.utils.FirebaseUtil;

import java.util.ArrayList;
import java.util.List;

public class GroupSettingsActivity extends AppCompatActivity {

    private String chatroomId;
    private ChatroomModel chatroomModel;
    private ImageButton backButton;
    private TextView groupNameView;
    private RecyclerView membersRecyclerView;
    private GroupMemberRecyclerAdapter adapter;
    private Button addMemberBtn;
    private Button leaveGroupBtn; // Novo botão

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_settings);

        backButton = findViewById(R.id.back_btn);
        groupNameView = findViewById(R.id.group_name_view);
        membersRecyclerView = findViewById(R.id.members_recycler_view);
        addMemberBtn = findViewById(R.id.add_member_btn);
        leaveGroupBtn = findViewById(R.id.leave_group_btn); // Referência do novo botão

        chatroomId = getIntent().getStringExtra("chatroomId");

        backButton.setOnClickListener(v -> onBackPressed());
        groupNameView.setOnClickListener(v -> showRenameGroupDialog());

        addMemberBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, SelectGroupMembersActivity.class);
            intent.putStringArrayListExtra("currentMembers", new ArrayList<>(chatroomModel.getUserIds()));
            intent.putExtra("chatroomId", chatroomId);
            startActivity(intent);
        });

        // Listener para o novo botão
        leaveGroupBtn.setOnClickListener(v -> showLeaveGroupDialog());

        getGroupDetails();
    }

    private void getGroupDetails() {
        FirebaseUtil.getChatroomReference(chatroomId).addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                chatroomModel = snapshot.toObject(ChatroomModel.class);
                if (chatroomModel != null) {
                    groupNameView.setText(chatroomModel.getGroupName());
                    setupMembersRecyclerView();
                }
            }
        });
    }

    private void setupMembersRecyclerView() {
        if (adapter == null) {
            adapter = new GroupMemberRecyclerAdapter(this, chatroomModel.getUserIds(), chatroomModel);
            membersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            membersRecyclerView.setAdapter(adapter);
        } else {
            adapter.updateMembers(chatroomModel.getUserIds());
        }
    }

    private void showRenameGroupDialog() {
        if (chatroomModel == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Group");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(chatroomModel.getGroupName());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && newName.length() >= 3) {
                chatroomModel.setGroupName(newName);
                FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Group renamed successfully", Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(this, "Name must be at least 3 characters", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // Nova função para o diálogo de confirmação
    private void showLeaveGroupDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Group")
                .setMessage("Are you sure you want to leave this group?")
                .setPositiveButton("Leave", (dialog, which) -> {
                    leaveGroup();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Nova função para a lógica de sair do grupo
    private void leaveGroup() {
        if (chatroomModel != null) {
            List<String> updatedUserIds = chatroomModel.getUserIds();
            updatedUserIds.remove(FirebaseUtil.currentUserId());
            chatroomModel.setUserIds(updatedUserIds);

            FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "You have left the group", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (chatroomModel != null) {
            getGroupDetails();
        }
    }
}