package com.example.easychat.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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

public class RecentChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatroomModel, RecentChatRecyclerAdapter.ChatroomModelViewHolder> {

    Context context;

    public RecentChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatroomModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatroomModelViewHolder holder, int position, @NonNull ChatroomModel model) {

        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra("chatroomId", model.getChatroomId());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        boolean hasCustomNotif = model.getCustomNotificationStatus()
                .getOrDefault(FirebaseUtil.currentUserId(), false);

        if (model.isGroupChat()) {
            holder.usernameText.setText(model.getGroupName());
            holder.profilePic.setImageResource(R.drawable.chat_icon);
            holder.statusIndicator.setVisibility(View.GONE);
            holder.lastMessageTime.setText(FirebaseUtil.timestampToString(model.getLastMessageTimestamp()));
            holder.lastMessageText.setText(model.getLastMessage());
            holder.unreadCountText.setVisibility(View.GONE);
            holder.itemView.setBackground(ContextCompat.getDrawable(context, R.drawable.edit_text_rounded_corner));
        } else {
            FirebaseUtil.getOtherUserFromChatroom(model.getUserIds())
                    .get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            UserModel otherUserModel = documentSnapshot.toObject(UserModel.class);

                            holder.statusIndicator.setVisibility(View.VISIBLE);
                            if ("busy".equals(otherUserModel.getUserStatus())) {
                                holder.statusIndicator.setImageResource(R.drawable.busy_indicator);
                            } else if ("online".equals(otherUserModel.getUserStatus())) {
                                holder.statusIndicator.setImageResource(R.drawable.online_indicator);
                            } else {
                                holder.statusIndicator.setImageResource(R.drawable.offline_indicator);
                            }

                            AndroidUtil.passUserModelAsIntent(intent, otherUserModel);
                            boolean lastMessageSentByMe = model.getLastMessageSenderId().equals(FirebaseUtil.currentUserId());

                            FirebaseUtil.getOtherProfilePicStorageRef(otherUserModel.getUserId()).getDownloadUrl()
                                    .addOnCompleteListener(t -> {
                                        if (t.isSuccessful()) {
                                            Uri uri = t.getResult();
                                            AndroidUtil.setProfilePic(context, uri, holder.profilePic);
                                        }
                                    });

                            holder.usernameText.setText(otherUserModel.getUsername());
                            if (lastMessageSentByMe) holder.lastMessageText.setText("You: " + model.getLastMessage());
                            else holder.lastMessageText.setText(model.getLastMessage());
                            holder.lastMessageTime.setText(FirebaseUtil.timestampToString(model.getLastMessageTimestamp()));

                            // CORREÇÃO: Lógica de contagem agora usa um Listener
                            String chatroomId = FirebaseUtil.getChatroomId(FirebaseUtil.currentUserId(), otherUserModel.getUserId());
                            FirebaseUtil.getChatroomMessageReference(chatroomId)
                                    .whereEqualTo("senderId", otherUserModel.getUserId())
                                    .whereEqualTo("status", ChatMessageModel.STATUS_SENT)
                                    .addSnapshotListener((querySnapshot, e) -> {
                                        if (e != null) { return; }

                                        if (querySnapshot != null) {
                                            int unreadCount = querySnapshot.size();
                                            if (unreadCount > 0) {
                                                holder.unreadCountText.setText(String.valueOf(unreadCount));
                                                holder.unreadCountText.setVisibility(View.VISIBLE);
                                            } else {
                                                holder.unreadCountText.setVisibility(View.GONE);
                                            }
                                            // Lógica para o destaque da notificação personalizada
                                            if (hasCustomNotif && unreadCount > 0) {
                                                holder.itemView.setBackground(ContextCompat.getDrawable(context, R.drawable.list_item_background_highlight));
                                            } else {
                                                holder.itemView.setBackground(ContextCompat.getDrawable(context, R.drawable.edit_text_rounded_corner));
                                            }
                                        }
                                    });
                        }
                    });
        }

        holder.itemView.setOnClickListener(v -> context.startActivity(intent));
    }

    @NonNull
    @Override
    public ChatroomModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recent_chat_recycler_row, parent, false);
        return new ChatroomModelViewHolder(view);
    }

    static class ChatroomModelViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText, lastMessageText, lastMessageTime, unreadCountText;
        ImageView profilePic, statusIndicator;

        public ChatroomModelViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.user_name_text);
            lastMessageText = itemView.findViewById(R.id.last_message_text);
            lastMessageTime = itemView.findViewById(R.id.last_message_time_text);
            profilePic = itemView.findViewById(R.id.profile_pic_image_view);
            unreadCountText = itemView.findViewById(R.id.unread_message_count_text);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
        }
    }
}