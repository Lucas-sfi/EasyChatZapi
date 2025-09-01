package com.example.easychat.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.easychat.ChatActivity;
import com.example.easychat.R;
import com.example.easychat.model.ChatMessageModel;
import com.example.easychat.model.ChatroomModel;
import com.example.easychat.model.UserModel;
import com.example.easychat.utils.AndroidUtil;
import com.example.easychat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.ListenerRegistration;

public class RecentChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatroomModel, RecentChatRecyclerAdapter.ChatroomModelViewHolder> {

    Context context;

    public RecentChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatroomModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatroomModelViewHolder holder, int position, @NonNull ChatroomModel model) {
        holder.removeListeners(); // Limpa listeners antigos

        // Lógica da última mensagem
        String lastMessage = model.getLastMessage() != null ? model.getLastMessage() : "";
        boolean lastMessageSentByMe = model.getLastMessageSenderId() != null && model.getLastMessageSenderId().equals(FirebaseUtil.currentUserId());

        if (lastMessageSentByMe && !model.isGroupChat()) {
            holder.lastMessageText.setText("You: " + lastMessage);
        } else {
            holder.lastMessageText.setText(lastMessage);
        }
        holder.lastMessageTime.setText(FirebaseUtil.timestampToString(model.getLastMessageTimestamp()));

        // Lógica para grupos vs. conversas individuais
        if (model.isGroupChat()) {
            holder.usernameText.setText(model.getGroupName());
            holder.profilePic.setImageResource(R.drawable.chat_icon);
            holder.statusIndicator.setVisibility(View.GONE);
            holder.unreadCountText.setVisibility(View.GONE);

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("chatroomId", model.getChatroomId());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            });
        } else {
            FirebaseUtil.getOtherUserFromChatroom(model.getUserIds())
                    .get().addOnSuccessListener(documentSnapshot -> {
                        if (!documentSnapshot.exists()) return;

                        UserModel otherUserModel = documentSnapshot.toObject(UserModel.class);
                        if (otherUserModel == null) return;

                        holder.usernameText.setText(otherUserModel.getUsername());

                        FirebaseUtil.getOtherProfilePicStorageRef(otherUserModel.getUserId()).getDownloadUrl()
                                .addOnCompleteListener(t -> {
                                    if (t.isSuccessful()) {
                                        AndroidUtil.setProfilePic(context, t.getResult(), holder.profilePic);
                                    }
                                });

                        // Anexa um listener para o chatroom que controlará o destaque e a contagem de não lidos
                        holder.attachChatroomListener(model.getChatroomId(), otherUserModel.getUserId());

                        holder.itemView.setOnClickListener(v -> {
                            Intent intent = new Intent(context, ChatActivity.class);
                            AndroidUtil.passUserModelAsIntent(intent, otherUserModel);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        });
                    });
        }
    }

    @NonNull
    @Override
    public ChatroomModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recent_chat_recycler_row, parent, false);
        return new ChatroomModelViewHolder(view);
    }

    @Override
    public void onViewRecycled(@NonNull ChatroomModelViewHolder holder) {
        super.onViewRecycled(holder);
        holder.removeListeners();
    }

    static class ChatroomModelViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText, lastMessageText, lastMessageTime, unreadCountText;
        ImageView profilePic, statusIndicator;
        ListenerRegistration unreadCountListener;
        ListenerRegistration chatroomListener; // Listener para o chatroom

        public ChatroomModelViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.user_name_text);
            lastMessageText = itemView.findViewById(R.id.last_message_text);
            lastMessageTime = itemView.findViewById(R.id.last_message_time_text);
            profilePic = itemView.findViewById(R.id.profile_pic_image_view);
            unreadCountText = itemView.findViewById(R.id.unread_message_count_text);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
        }

        void attachChatroomListener(String chatroomId, String otherUserId) {
            chatroomListener = FirebaseUtil.getChatroomReference(chatroomId)
                    .addSnapshotListener((snapshot, e) -> {
                        if (e != null || snapshot == null || !snapshot.exists()) return;

                        ChatroomModel chatroomModel = snapshot.toObject(ChatroomModel.class);
                        if (chatroomModel == null) return;

                        boolean hasCustomNotif = chatroomModel.getCustomNotificationStatus()
                                .getOrDefault(FirebaseUtil.currentUserId(), false);

                        // Agora, anexa ou reavalia a contagem de mensagens não lidas
                        attachUnreadCountListener(chatroomId, otherUserId, hasCustomNotif);
                    });
        }

        void attachUnreadCountListener(String chatroomId, String otherUserId, boolean hasCustomNotif) {
            removeUnreadCountListener(); // Remove o listener antigo antes de adicionar um novo

            unreadCountListener = FirebaseUtil.getChatroomMessageReference(chatroomId)
                    .whereEqualTo("senderId", otherUserId)
                    .whereEqualTo("status", ChatMessageModel.STATUS_SENT)
                    .addSnapshotListener((querySnapshot, e) -> {
                        if (e != null) return;

                        if (querySnapshot != null) {
                            int unreadCount = querySnapshot.size();
                            if (unreadCount > 0) {
                                unreadCountText.setText(String.valueOf(unreadCount));
                                unreadCountText.setVisibility(View.VISIBLE);
                            } else {
                                unreadCountText.setVisibility(View.GONE);
                            }

                            // Lógica de destaque
                            if (hasCustomNotif && unreadCount > 0) {
                                itemView.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.list_item_background_highlight));
                            } else {
                                itemView.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.edit_text_rounded_corner));
                            }
                        }
                    });
        }

        void removeListeners() {
            removeUnreadCountListener();
            if (chatroomListener != null) {
                chatroomListener.remove();
                chatroomListener = null;
            }
        }

        void removeUnreadCountListener() {
            if (unreadCountListener != null) {
                unreadCountListener.remove();
                unreadCountListener = null;
            }
        }
    }
}