package com.example.easychat;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.DialogInterface;
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

public class GroupSettingsActivity extends AppCompatActivity {

    private String chatroomId;
    private ChatroomModel chatroomModel;
    private ImageButton backButton;
    private TextView groupNameView;
    private RecyclerView membersRecyclerView;
    private GroupMemberRecyclerAdapter adapter;
    private Button addMemberBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_settings);

        backButton = findViewById(R.id.back_btn);
        groupNameView = findViewById(R.id.group_name_view);
        membersRecyclerView = findViewById(R.id.members_recycler_view);
        addMemberBtn = findViewById(R.id.add_member_btn);

        chatroomId = getIntent().getStringExtra("chatroomId");

        backButton.setOnClickListener(v -> onBackPressed());

        groupNameView.setOnClickListener(v -> showRenameGroupDialog());

        addMemberBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, SelectGroupMembersActivity.class);
            // Passar os membros atuais para a próxima tela para que ela possa filtrá-los
            intent.putStringArrayListExtra("currentMembers", new ArrayList<>(chatroomModel.getUserIds()));
            // AQUI ESTÁ A CORREÇÃO: Passar o ID do grupo para a tela de seleção
            intent.putExtra("chatroomId", chatroomId);
            startActivity(intent);
        });

        getGroupDetails();
    }

    private void getGroupDetails() {
        // Usar addSnapshotListener para atualizações em tempo real
        FirebaseUtil.getChatroomReference(chatroomId).addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                // Lidar com o erro
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
            // Apenas notificar o adapter de que os dados mudaram
            adapter.updateMembers(chatroomModel.getUserIds());
        }
    }

    private void showRenameGroupDialog() {
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

    // Adicionar onResume para garantir que a lista seja atualizada ao voltar para a tela
    @Override
    protected void onResume() {
        super.onResume();
        if (chatroomModel != null) {
            getGroupDetails();
        }
    }
}