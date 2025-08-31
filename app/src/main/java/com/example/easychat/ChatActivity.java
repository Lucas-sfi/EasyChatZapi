package com.example.easychat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.app.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
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

    ImageButton chatSearchButton;
    RelativeLayout inChatSearchBar;
    EditText inChatSearchInput;
    ImageButton searchUpBtn, searchDownBtn;
    private List<Integer> searchResultPositions = new ArrayList<>();
    private int currentSearchIndex = -1;

    RelativeLayout pinnedMessageLayout;
    TextView pinnedMessageText;
    ImageButton unpinButton;

    ImageButton attachFileButton;
    ActivityResultLauncher<Intent> imagePickerLauncher;
    Uri selectedImageUri;

    private long targetMessageTimestamp = -1;
    private ListenerRegistration messageReadListener;

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
        chatSearchButton = findViewById(R.id.chat_search_btn);
        inChatSearchBar = findViewById(R.id.in_chat_search_bar);
        inChatSearchInput = findViewById(R.id.in_chat_search_input);
        searchUpBtn = findViewById(R.id.search_up_btn);
        searchDownBtn = findViewById(R.id.search_down_btn);
        pinnedMessageLayout = findViewById(R.id.pinned_message_layout);
        pinnedMessageText = findViewById(R.id.pinned_message_text);
        unpinButton = findViewById(R.id.unpin_btn);
        attachFileButton = findViewById(R.id.attach_file_btn);

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            selectedImageUri = data.getData();
                            Toast.makeText(this, "Imagem selecionada. Fazendo upload...", Toast.LENGTH_SHORT).show();
                            uploadImageToFirebase();
                        }
                    }
                }
        );

        otherUser = AndroidUtil.getUserModelFromIntent(getIntent());
        chatroomId = getIntent().getStringExtra("chatroomId");
        targetMessageTimestamp = getIntent().getLongExtra("messageTimestamp", -1);

        if (chatroomId == null && otherUser != null) {
            chatroomId = FirebaseUtil.getChatroomId(FirebaseUtil.currentUserId(), otherUser.getUserId());
        }

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
        chatSearchButton.setOnClickListener(v -> toggleSearchBar());
        searchUpBtn.setOnClickListener(v -> navigateSearchResults(false));
        searchDownBtn.setOnClickListener(v -> navigateSearchResults(true));
        inChatSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    searchInChat(s.toString().toLowerCase());
                } else {
                    clearSearch();
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        getChatroomData();
    }

    @Override
    public void onPinMessageClicked(String messageId) {
        if (chatroomModel != null) {
            chatroomModel.setPinnedMessageId(messageId);
            FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Message pinned", Toast.LENGTH_SHORT).show());
        }
    }

    private void getChatroomData() {
        FirebaseUtil.getChatroomReference(chatroomId).addSnapshotListener((snapshot, e) -> {
            if (e != null) { return; }

            if (snapshot == null || !snapshot.exists()) {
                if (!FirebaseUtil.isGroupChat(chatroomId) && otherUser != null) {
                    chatroomModel = new ChatroomModel(
                            chatroomId,
                            Arrays.asList(FirebaseUtil.currentUserId(), otherUser.getUserId()),
                            Timestamp.now(), "", "", false
                    );
                    FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);
                } else {
                    Toast.makeText(this, "Chat not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            } else {
                chatroomModel = snapshot.toObject(ChatroomModel.class);
            }

            if (chatroomModel == null) { return; }

            if (!chatroomModel.isGroupChat() && otherUser == null) {
                FirebaseUtil.getOtherUserFromChatroom(chatroomModel.getUserIds()).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            otherUser = documentSnapshot.toObject(UserModel.class);
                            updateUI();
                            setupChatRecyclerView();
                            markMessagesAsRead();
                        });
            } else {
                updateUI();
                setupChatRecyclerView();
                markMessagesAsRead();
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

        if (chatroomModel.getPinnedMessageId() != null && !chatroomModel.getPinnedMessageId().isEmpty()) {
            pinnedMessageLayout.setVisibility(View.VISIBLE);
            FirebaseUtil.getChatroomMessageReference(chatroomId).document(chatroomModel.getPinnedMessageId()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        ChatMessageModel pinnedMessage = documentSnapshot.toObject(ChatMessageModel.class);
                        if (pinnedMessage != null) {
                            pinnedMessageText.setText(pinnedMessage.getMessage());
                        }
                    });
            unpinButton.setOnClickListener(v -> {
                chatroomModel.setPinnedMessageId(null);
                FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);
            });
        } else {
            pinnedMessageLayout.setVisibility(View.GONE);
        }
    }

    private void setupChatRecyclerView() {
        if (adapter == null) {
            Query query = FirebaseUtil.getChatroomMessageReference(chatroomId)
                    .orderBy("timestamp", Query.Direction.ASCENDING);

            FirestoreRecyclerOptions<ChatMessageModel> options = new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                    .setQuery(query, ChatMessageModel.class).build();

            adapter = new ChatRecyclerAdapter(options, this, this);
            LinearLayoutManager manager = new LinearLayoutManager(this);
            manager.setStackFromEnd(true); // Garante que a view comece do final
            recyclerView.setLayoutManager(manager);
            recyclerView.setAdapter(adapter);
            adapter.startListening();

            adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    super.onItemRangeInserted(positionStart, itemCount);
                    if (adapter.getItemCount() > 1) {
                        recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
                    }
                }
            });

            if (targetMessageTimestamp != -1) {
                // ...
            } else {
                new Handler().postDelayed(() -> {
                    if(adapter.getItemCount() > 0){
                        recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
                    }
                }, 200);
            }
        }
    }

    void uploadImageToFirebase() {
        if (selectedImageUri == null) return;
        String imageId = "img_" + System.currentTimeMillis();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("chat_images").child(chatroomId).child(imageId);

        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        Toast.makeText(this, "Upload concluído. Enviando imagem...", Toast.LENGTH_SHORT).show();
                        sendImageMessage(uri.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Falha no upload da imagem: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    void sendImageMessage(String imageUrl) {
        if (chatroomModel == null) return;
        chatroomModel.setLastMessageTimestamp(Timestamp.now());
        chatroomModel.setLastMessageSenderId(FirebaseUtil.currentUserId());
        chatroomModel.setLastMessage("Imagem");
        FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);

        ChatMessageModel chatMessageModel = new ChatMessageModel(imageUrl, FirebaseUtil.currentUserId(), Timestamp.now(), ChatMessageModel.STATUS_SENT, new ArrayList<>(), "image");

        FirebaseUtil.getChatroomMessageReference(chatroomId).add(chatMessageModel)
                .addOnSuccessListener(documentReference -> {
                    // A rolagem já é tratada pelo AdapterDataObserver
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Falha ao enviar a imagem para o chat.", Toast.LENGTH_SHORT).show();
                });
    }

    private void toggleSearchBar() {
        if (inChatSearchBar.getVisibility() == View.VISIBLE) {
            inChatSearchBar.setVisibility(View.GONE);
            clearSearch();
        } else {
            inChatSearchBar.setVisibility(View.VISIBLE);
        }
    }

    private void markMessagesAsRead() {
        if (chatroomModel != null && !chatroomModel.isGroupChat() && otherUser != null) {
            if (messageReadListener != null) {
                messageReadListener.remove();
            }

            messageReadListener = FirebaseUtil.getChatroomMessageReference(chatroomId)
                    .whereEqualTo("senderId", otherUser.getUserId())
                    .whereEqualTo("status", ChatMessageModel.STATUS_SENT)
                    .addSnapshotListener((querySnapshot, e) -> {
                        if (e != null || querySnapshot == null) {
                            return;
                        }
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            document.getReference().update("status", ChatMessageModel.STATUS_READ);
                        }
                    });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (messageReadListener != null) {
            messageReadListener.remove();
        }
    }

    private void searchInChat(String searchTerm) {
        // Lógica de busca
    }

    private void navigateSearchResults(boolean down) {
        // Lógica de navegação
    }

    private void navigateToCurrentSearchResult() {
        // Lógica de navegação
    }

    private void clearSearch() {
        // Lógica de limpeza
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
                        if (!chatroomModel.isGroupChat() && otherUser != null) {
                            sendNotification(message);
                        }
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
                .header("Authorization", "Bearer AAAA_X_E9V0:APA91bFNZt_M3k7zQ8Ue-9kY0f8o-4v5d6c7b8a9g0h1i2j3k4l5m6n7o8p9q0r1s2t3u4v5w6x7y8z9A0B1C2D3E4F5G6H7I8J9K0L") // Substitua pela sua chave de servidor do FCM
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {}
        });
    }
}