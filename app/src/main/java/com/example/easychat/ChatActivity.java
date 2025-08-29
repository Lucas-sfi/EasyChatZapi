package com.example.easychat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.easychat.adapter.ChatRecyclerAdapter;
import com.example.easychat.model.ChatMessageModel;
import com.example.easychat.model.ChatroomModel;
import com.example.easychat.model.UserModel;
import com.example.easychat.utils.AndroidUtil;
import com.example.easychat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    String chatroomId;
    ChatroomModel chatroomModel;
    UserModel otherUser;

    ChatRecyclerAdapter adapter;
    EditText messageInput;
    ImageButton sendMessageBtn;
    ImageButton backBtn;
    TextView toolbarTitle;
    RecyclerView recyclerView;
    ImageView toolbarProfilePic;
    RelativeLayout toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        messageInput = findViewById(R.id.chat_message_input);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        backBtn = findViewById(R.id.back_btn);
        toolbarTitle = findViewById(R.id.other_username);
        recyclerView = findViewById(R.id.chat_recycler_view);
        toolbarProfilePic = findViewById(R.id.profile_pic_image_view);
        toolbar = findViewById(R.id.toolbar);

        backBtn.setOnClickListener(v -> onBackPressed());

        otherUser = AndroidUtil.getUserModelFromIntent(getIntent());
        chatroomId = getIntent().getStringExtra("chatroomId");

        if (chatroomId == null && otherUser != null) {
            chatroomId = FirebaseUtil.getChatroomId(FirebaseUtil.currentUserId(), otherUser.getUserId());
        }

        getChatroomData();

        sendMessageBtn.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (message.isEmpty()) return;
            sendMessage(message);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        markMessagesAsRead();
    }

    private void markMessagesAsRead() {
        if (chatroomModel != null && !chatroomModel.isGroupChat() && otherUser != null) {
            FirebaseUtil.getChatroomMessageReference(chatroomId)
                    .whereEqualTo("senderId", otherUser.getUserId())
                    .whereEqualTo("status", ChatMessageModel.STATUS_SENT)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            for (DocumentSnapshot document : task.getResult().getDocuments()) {
                                document.getReference().update("status", ChatMessageModel.STATUS_READ);
                            }
                        }
                    });
        }
    }

    private void getChatroomData() {
        FirebaseUtil.getChatroomReference(chatroomId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                chatroomModel = task.getResult().toObject(ChatroomModel.class);
                if (chatroomModel == null && !FirebaseUtil.isGroupChat(chatroomId)) {
                    chatroomModel = new ChatroomModel(
                            chatroomId,
                            Arrays.asList(FirebaseUtil.currentUserId(), otherUser.getUserId()),
                            Timestamp.now(),
                            "",
                            "",
                            false
                    );
                    FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);
                }
                updateUI();
                setupChatRecyclerView();
            } else {
                Toast.makeText(this, "Failed to load chatroom", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void updateUI() {
        if (chatroomModel == null) return;
        if (chatroomModel.isGroupChat()) {
            toolbarTitle.setText(chatroomModel.getGroupName());
            toolbarProfilePic.setImageResource(R.drawable.chat_icon);
            toolbar.setOnClickListener(v -> {
                Intent intent = new Intent(this, GroupSettingsActivity.class);
                intent.putExtra("chatroomId", chatroomId);
                startActivity(intent);
            });
        } else {
            if (otherUser != null) {
                toolbarTitle.setText(otherUser.getUsername());
                FirebaseUtil.getOtherProfilePicStorageRef(otherUser.getUserId()).getDownloadUrl()
                        .addOnCompleteListener(t -> {
                            if (t.isSuccessful()) {
                                Uri uri = t.getResult();
                                AndroidUtil.setProfilePic(this, uri, toolbarProfilePic);
                            }
                        });
                toolbar.setOnClickListener(v -> {
                    Intent intent = new Intent(this, UserSettingsActivity.class);
                    intent.putExtra("chatroomId", chatroomId);
                    AndroidUtil.passUserModelAsIntent(intent, otherUser);
                    startActivity(intent);
                });
            }
        }
    }

    private void setupChatRecyclerView() {
        Query query = FirebaseUtil.getChatroomMessageReference(chatroomId)
                .orderBy("timestamp", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<ChatMessageModel> options = new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                .setQuery(query, ChatMessageModel.class).build();
        adapter = new ChatRecyclerAdapter(options, getApplicationContext());
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        adapter.startListening();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerView.smoothScrollToPosition(0);
            }
        });
    }

    private void sendMessage(String message) {
        if (chatroomModel == null) return;
        chatroomModel.setLastMessageTimestamp(Timestamp.now());
        chatroomModel.setLastMessageSenderId(FirebaseUtil.currentUserId());
        chatroomModel.setLastMessage(message);
        FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);

        // LÓGICA DE CRIAÇÃO DE PALAVRAS-CHAVE
        List<String> keywords = Arrays.asList(message.toLowerCase().split(" "));
        ChatMessageModel chatMessageModel = new ChatMessageModel(message, FirebaseUtil.currentUserId(), Timestamp.now(), ChatMessageModel.STATUS_SENT, keywords);

        FirebaseUtil.getChatroomMessageReference(chatroomId).add(chatMessageModel)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        messageInput.setText("");
                        if (!chatroomModel.isGroupChat() && otherUser != null) {
                            sendNotification(message);
                        }
                    }
                });
    }

    // As funções sendNotification e callApi continuam iguais
    void sendNotification(String message) {
        if (otherUser == null || otherUser.getFcmToken() == null) return;
        FirebaseUtil.currentUserDetails().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                UserModel currentUser = task.getResult().toObject(UserModel.class);
                if (currentUser == null) return;
                try {
                    JSONObject jsonObject = new JSONObject();
                    JSONObject notificationObj = new JSONObject();
                    notificationObj.put("title", currentUser.getUsername());
                    notificationObj.put("body", message);
                    JSONObject dataObj = new JSONObject();
                    dataObj.put("userId", currentUser.getUserId());
                    jsonObject.put("notification", notificationObj);
                    jsonObject.put("data", dataObj);
                    jsonObject.put("to", otherUser.getFcmToken());
                    callApi(jsonObject);
                } catch (Exception e) {
                    Log.e("ChatActivity", "sendNotification failed", e);
                }
            }
        });
    }

    void callApi(JSONObject jsonObject) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();
        String url = "https://fcm.googleapis.com/fcm/send";
        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Authorization", "Bearer YOUR_API_KEY")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {}
        });
    }
}