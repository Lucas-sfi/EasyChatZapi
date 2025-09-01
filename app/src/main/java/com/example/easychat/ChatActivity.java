package com.example.easychat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.easychat.adapter.ChatRecyclerAdapter;
import com.example.easychat.model.ChatMessageModel;
import com.example.easychat.model.ChatroomModel;
import com.example.easychat.model.UserModel;
import com.example.easychat.utils.AndroidUtil;
import com.example.easychat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity implements ChatRecyclerAdapter.PinMessageListener {

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
    ImageButton attachFileButton;
    ActivityResultLauncher<Intent> imagePickerLauncher;
    Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Inicialização de Views
        messageInput = findViewById(R.id.chat_message_input);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        backBtn = findViewById(R.id.back_btn);
        toolbarTitle = findViewById(R.id.other_username);
        recyclerView = findViewById(R.id.chat_recycler_view);
        toolbarProfilePic = findViewById(R.id.profile_pic_image_view);
        toolbar = findViewById(R.id.toolbar);
        attachFileButton = findViewById(R.id.attach_file_btn);

        // Pega os dados da Intent
        otherUser = AndroidUtil.getUserModelFromIntent(getIntent());
        chatroomId = getIntent().getStringExtra("chatroomId");

        if (chatroomId == null && otherUser != null) {
            chatroomId = FirebaseUtil.getChatroomId(FirebaseUtil.currentUserId(), otherUser.getUserId());
        }

        cancelNotification();

        backBtn.setOnClickListener(v -> onBackPressed());
        sendMessageBtn.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (message.isEmpty()) return;
            sendMessage(message);
        });

        attachFileButton.setOnClickListener(v -> {
            ImagePicker.with(this).crop().compress(1024)
                    .createIntent(intent -> {
                        imagePickerLauncher.launch(intent);
                        return null;
                    });
        });

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                if (data != null && data.getData() != null) {
                    selectedImageUri = data.getData();
                    uploadImageToFirebase();
                }
            }
        });

        getChatroomData();
        setupChatRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(chatroomId != null) {
            ChatActivityState.setActiveChatroomId(chatroomId);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        ChatActivityState.clearActiveChatroomId();
    }

    private void cancelNotification() {
        if (chatroomId != null) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(chatroomId.hashCode());
        }
    }

    private void getChatroomData() {
        FirebaseUtil.getChatroomReference(chatroomId).addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e("ChatActivity", "Error fetching chatroom data", e);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                chatroomModel = snapshot.toObject(ChatroomModel.class);
            } else {
                if (!FirebaseUtil.isGroupChat(chatroomId) && otherUser != null) {
                    chatroomModel = new ChatroomModel(
                            chatroomId,
                            Arrays.asList(FirebaseUtil.currentUserId(), otherUser.getUserId()),
                            Timestamp.now(), "", "", false
                    );
                    FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);
                }
            }

            if (chatroomModel == null) return;

            markMessagesAsRead();
            updateUI();
        });
    }

    private void updateUI() {
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
                AndroidUtil.passUserModelAsIntent(intent, otherUser);
                startActivity(intent);
            });
        }
    }

    private void setupChatRecyclerView() {
        Query query = FirebaseUtil.getChatroomMessageReference(chatroomId)
                .orderBy("timestamp", Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<ChatMessageModel> options = new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                .setQuery(query, ChatMessageModel.class).build();

        adapter = new ChatRecyclerAdapter(options, this, this);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setStackFromEnd(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        adapter.startListening();

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerView.smoothScrollToPosition(adapter.getItemCount());
            }
        });
    }

    private void markMessagesAsRead() {
        if (otherUser == null || chatroomId == null) return;

        FirebaseUtil.getChatroomMessageReference(chatroomId)
                .whereEqualTo("senderId", otherUser.getUserId())
                .whereNotEqualTo("status", ChatMessageModel.STATUS_READ)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) return;

                    WriteBatch batch = FirebaseUtil.allChatroomCollectionReference().getFirestore().batch();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        batch.update(doc.getReference(), "status", ChatMessageModel.STATUS_READ);
                    }
                    batch.commit().addOnFailureListener(e ->
                            Log.e("ChatActivity", "Failed to mark messages as read", e)
                    );
                });
    }

    private void sendMessage(String message) {
        if (chatroomModel == null) return;
        chatroomModel.setLastMessageTimestamp(Timestamp.now());
        chatroomModel.setLastMessageSenderId(FirebaseUtil.currentUserId());
        chatroomModel.setLastMessage(message);
        FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);

        List<String> keywords = generateKeywords(message);
        ChatMessageModel chatMessageModel = new ChatMessageModel(message, FirebaseUtil.currentUserId(), Timestamp.now(), ChatMessageModel.STATUS_SENT, keywords, "text");

        FirebaseUtil.getChatroomMessageReference(chatroomId).add(chatMessageModel)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        messageInput.setText("");
                        sendNotification(message);
                    }
                });
    }

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
                    dataObj.put("chatroomId", chatroomId);
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

    // As funções abaixo (upload de imagem, API call, etc.) não precisam de alteração
    void uploadImageToFirebase() {
        if (selectedImageUri == null) return;
        String imageId = "img_" + System.currentTimeMillis();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("chat_images").child(chatroomId).child(imageId);

        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        sendImageMessage(uri.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Falha no upload da imagem.", Toast.LENGTH_LONG).show();
                });
    }

    void sendImageMessage(String imageUrl) {
        if (chatroomModel == null) return;
        chatroomModel.setLastMessageTimestamp(Timestamp.now());
        chatroomModel.setLastMessageSenderId(FirebaseUtil.currentUserId());
        chatroomModel.setLastMessage("Imagem");
        FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);

        ChatMessageModel chatMessageModel = new ChatMessageModel(imageUrl, FirebaseUtil.currentUserId(), Timestamp.now(), ChatMessageModel.STATUS_SENT, new ArrayList<>(), "image");

        FirebaseUtil.getChatroomMessageReference(chatroomId).add(chatMessageModel);
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

    void callApi(JSONObject jsonObject) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();
        String url = "https://fcm.googleapis.com/fcm/send";
        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Authorization", "Bearer AAAA_X_E9V0:APA91bFNZt_M3k7zQ8Ue-9kY0f8o-4v5d6c7b8a9g0h1i2j3k4l5m6n7o8p9q0r1s2t3u4v5w6x7y8z9A0B1C2D3E4F5G6H7I8J9K0L")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {}
        });
    }

    @Override public void onPinMessageClicked(String messageId) {}
}