package com.example.easychat;

import androidx.annotation.NonNull;
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

import com.example.easychat.adapter.ChatRecyclerAdapter;
import com.example.easychat.model.ChatMessageModel;
import com.example.easychat.model.ChatroomModel;
import com.example.easychat.model.UserModel;
import com.example.easychat.utils.AndroidUtil;
import com.example.easychat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
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
    boolean isGroupChat;
    String groupName;

    ChatRecyclerAdapter adapter;
    EditText messageInput;
    ImageButton sendMessageBtn;
    ImageButton backBtn;
    TextView toolbarTitle;
    RecyclerView recyclerView;
    ImageView toolbarProfilePic;
    ImageView statusIndicator;
    RelativeLayout toolbar;
    ImageButton attachFileButton;
    ActivityResultLauncher<Intent> imagePickerLauncher;
    Uri selectedImageUri;
    ListenerRegistration unreadMessagesListener;

    // UI para mensagem fixada
    RelativeLayout pinnedMessageLayout;
    TextView pinnedMessageText;
    ImageButton unpinBtn;

    // UI para pesquisa no chat
    ImageButton chatSearchBtn;
    RelativeLayout inChatSearchBar;
    EditText inChatSearchInput;
    ImageButton searchUpBtn;
    ImageButton searchDownBtn;
    private List<String> searchResults = new ArrayList<>();
    private int currentSearchIndex = -1;


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
        statusIndicator = findViewById(R.id.status_indicator);
        toolbar = findViewById(R.id.toolbar);
        attachFileButton = findViewById(R.id.attach_file_btn);

        pinnedMessageLayout = findViewById(R.id.pinned_message_layout);
        pinnedMessageText = findViewById(R.id.pinned_message_text);
        unpinBtn = findViewById(R.id.unpin_btn);

        // Inicialização dos componentes de pesquisa
        chatSearchBtn = findViewById(R.id.chat_search_btn);
        inChatSearchBar = findViewById(R.id.in_chat_search_bar);
        inChatSearchInput = findViewById(R.id.in_chat_search_input);
        searchUpBtn = findViewById(R.id.search_up_btn);
        searchDownBtn = findViewById(R.id.search_down_btn);

        // Verifica se é um grupo ou chat individual
        isGroupChat = getIntent().getBooleanExtra("isGroupChat", false);
        if (isGroupChat) {
            groupName = getIntent().getStringExtra("groupName");
            chatroomId = getIntent().getStringExtra("chatroomId");
        } else {
            otherUser = AndroidUtil.getUserModelFromIntent(getIntent());
            chatroomId = FirebaseUtil.getChatroomId(FirebaseUtil.currentUserId(), otherUser.getUserId());
        }

        backBtn.setOnClickListener(v -> onBackPressed());
        sendMessageBtn.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (message.isEmpty()) return;
            sendMessageToUser(message);
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

        // Lógica do botão de pesquisa
        chatSearchBtn.setOnClickListener(v -> {
            if (inChatSearchBar.getVisibility() == View.VISIBLE) {
                inChatSearchBar.setVisibility(View.GONE);
                searchResults.clear();
                currentSearchIndex = -1;
                adapter.highlightMessage(null);
            } else {
                inChatSearchBar.setVisibility(View.VISIBLE);
            }
        });

        // Lógica de busca e navegação
        setupSearchFunctionality();

        getOrCreateChatroomModel();
        setupChatRecyclerView();
    }

    private void setupSearchFunctionality() {
        inChatSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    searchInChat(s.toString().toLowerCase());
                } else {
                    searchResults.clear();
                    currentSearchIndex = -1;
                    adapter.highlightMessage(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchUpBtn.setOnClickListener(v -> navigateSearchResults(false));
        searchDownBtn.setOnClickListener(v -> navigateSearchResults(true));
    }

    private void searchInChat(String searchTerm) {
        FirebaseUtil.getChatroomMessageReference(chatroomId)
                .whereArrayContains("searchKeywords", searchTerm)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    searchResults.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        searchResults.add(doc.getId());
                    }
                    currentSearchIndex = -1;
                    if (!searchResults.isEmpty()) {
                        navigateSearchResults(true);
                    } else {
                        adapter.highlightMessage(null);
                        Toast.makeText(ChatActivity.this, "Nenhuma mensagem encontrada", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateSearchResults(boolean down) {
        if (searchResults.isEmpty()) return;

        if (down) {
            currentSearchIndex++;
            if (currentSearchIndex >= searchResults.size()) {
                currentSearchIndex = 0;
            }
        } else {
            currentSearchIndex--;
            if (currentSearchIndex < 0) {
                currentSearchIndex = searchResults.size() - 1;
            }
        }
        scrollToMessage(searchResults.get(currentSearchIndex));
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(chatroomId != null) {
            ChatActivityState.setActiveChatroomId(chatroomId);
            cancelNotification();
            setupUnreadMessagesListener();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        ChatActivityState.clearActiveChatroomId();
        if (unreadMessagesListener != null) {
            unreadMessagesListener.remove();
        }
    }

    private void setupUnreadMessagesListener() {
        if (isGroupChat || otherUser == null || chatroomId == null) return;

        unreadMessagesListener = FirebaseUtil.getChatroomMessageReference(chatroomId)
                .whereEqualTo("senderId", otherUser.getUserId())
                .whereNotEqualTo("status", ChatMessageModel.STATUS_READ)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e("ChatActivity", "Listen for unread messages failed.", e);
                        return;
                    }

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        WriteBatch batch = FirebaseUtil.getFirestore().batch();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            batch.update(doc.getReference(), "status", ChatMessageModel.STATUS_READ);
                        }
                        batch.commit().addOnFailureListener(fail ->
                                Log.e("ChatActivity", "Failed to mark messages as read", fail)
                        );
                    }
                });
    }

    private void cancelNotification() {
        if (chatroomId != null) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(chatroomId.hashCode());
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

    private void setupPinnedMessageUI() {
        if (chatroomModel != null && chatroomModel.getPinnedMessageId() != null && !chatroomModel.getPinnedMessageId().isEmpty()) {
            FirebaseUtil.getChatroomMessageReference(chatroomId).document(chatroomModel.getPinnedMessageId()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            ChatMessageModel pinnedMessage = documentSnapshot.toObject(ChatMessageModel.class);
                            if (pinnedMessage != null) {
                                pinnedMessageText.setText(pinnedMessage.getMessage());
                                pinnedMessageLayout.setVisibility(View.VISIBLE);

                                pinnedMessageLayout.setOnClickListener(v -> scrollToMessage(chatroomModel.getPinnedMessageId()));
                                unpinBtn.setOnClickListener(v -> unpinMessage());
                            }
                        } else {
                            unpinMessage();
                        }
                    });
        } else {
            pinnedMessageLayout.setVisibility(View.GONE);
        }
    }

    private void scrollToMessage(String messageId) {
        int position = -1;
        for (int i = 0; i < adapter.getItemCount(); i++) {
            if (adapter.getSnapshots().getSnapshot(i).getId().equals(messageId)) {
                position = i;
                break;
            }
        }
        if (position != -1) {
            recyclerView.smoothScrollToPosition(position);
            adapter.highlightMessage(messageId);
        } else {
            Toast.makeText(this, "Message not found.", Toast.LENGTH_SHORT).show();
        }
    }

    private void unpinMessage() {
        if (chatroomModel != null) {
            chatroomModel.setPinnedMessageId(null);
            FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);
        }
    }

    @Override
    public void onPinMessageClicked(String messageId) {
        if (chatroomModel != null) {
            chatroomModel.setPinnedMessageId(messageId);
            FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Message pinned", Toast.LENGTH_SHORT).show());
        }
    }

    private void sendMessageToUser(String message) {
        if (chatroomModel == null) return;
        chatroomModel.setLastMessageTimestamp(Timestamp.now());
        chatroomModel.setLastMessageSenderId(FirebaseUtil.currentUserId());
        chatroomModel.setLastMessage(message);
        FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);

        List<String> keywords = generateKeywords(message);
        ChatMessageModel chatMessage = new ChatMessageModel(message, FirebaseUtil.currentUserId(), Timestamp.now(), ChatMessageModel.STATUS_SENT, keywords, "text");

        FirebaseUtil.getChatroomMessageReference(chatroomId).add(chatMessage)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        messageInput.setText("");
                        if (!isGroupChat) {
                            sendNotification(message);
                        }
                    }
                });
    }

    private void getOrCreateChatroomModel() {
        FirebaseUtil.getChatroomReference(chatroomId).addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                chatroomModel = snapshot.toObject(ChatroomModel.class);
            } else {
                if(!isGroupChat) {
                    chatroomModel = new ChatroomModel(
                            chatroomId,
                            Arrays.asList(FirebaseUtil.currentUserId(), otherUser.getUserId()),
                            Timestamp.now(),
                            "", "", false
                    );
                    FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel)
                            .addOnSuccessListener(aVoid -> {
                                // Adicionar usuários à lista de contatos um do outro
                                addContact(FirebaseUtil.currentUserId(), otherUser.getUserId());
                                addContact(otherUser.getUserId(), FirebaseUtil.currentUserId());
                            });
                }
            }
            updateToolbarUI();
            setupPinnedMessageUI();
        });
    }

    private void addContact(String userId, String contactId) {
        if (userId == null || contactId == null) return;
        DocumentReference userDocRef = FirebaseUtil.allUserCollectionReference().document(userId);
        // Usa arrayUnion para adicionar o contato de forma atômica, evitando duplicatas
        userDocRef.update("contacts", FieldValue.arrayUnion(contactId));
    }


    private void updateToolbarUI() {
        if (isGroupChat) {
            toolbarTitle.setText(groupName);
            toolbarProfilePic.setImageResource(R.drawable.chat_icon);
            statusIndicator.setVisibility(View.GONE);
            toolbar.setOnClickListener(v -> {
                Intent intent = new Intent(this, GroupSettingsActivity.class);
                intent.putExtra("chatroomId", chatroomId);
                startActivity(intent);
            });
        } else {
            if (otherUser != null) {
                FirebaseUtil.allUserCollectionReference().document(otherUser.getUserId()).addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        return;
                    }
                    UserModel updatedOtherUser = value.toObject(UserModel.class);
                    if (updatedOtherUser != null) {
                        toolbarTitle.setText(updatedOtherUser.getUsername());

                        statusIndicator.setVisibility(View.VISIBLE);
                        switch (updatedOtherUser.getUserStatus()) {
                            case "online":
                                statusIndicator.setImageResource(R.drawable.online_indicator);
                                break;
                            case "busy":
                                statusIndicator.setImageResource(R.drawable.busy_indicator);
                                break;
                            default:
                                statusIndicator.setImageResource(R.drawable.offline_indicator);
                                break;
                        }
                    }
                });

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
                    intent.putExtra("chatroomId", chatroomId);
                    startActivity(intent);
                });
            }
        }
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

    void uploadImageToFirebase() {
        if (selectedImageUri == null) return;
        String imageId = "img_" + System.currentTimeMillis();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("chat_images").child(chatroomId).child(imageId);

        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    sendImageMessage(uri.toString());
                }))
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

        ChatMessageModel chatMessage = new ChatMessageModel(imageUrl, FirebaseUtil.currentUserId(), Timestamp.now(), ChatMessageModel.STATUS_SENT, new ArrayList<>(), "image");

        FirebaseUtil.getChatroomMessageReference(chatroomId).add(chatMessage);
    }

    private List<String> generateKeywords(String text) {
        String searchableString = text.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "");
        List<String> keywords = new ArrayList<>();
        for (String word : searchableString.split("\\s+")) {
            if (!word.isEmpty()) {
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
}