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
import com.google.firebase.firestore.Query;

public class RecentChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatroomModel, RecentChatRecyclerAdapter.ChatroomModelViewHolder> {

    Context context;

    public RecentChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatroomModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatroomModelViewHolder holder, int position, @NonNull ChatroomModel model) {

        // Intent que será usada para abrir a tela de chat
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra("chatroomId", model.getChatroomId());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (model.isGroupChat()) {
            // LÓGICA PARA CHAT EM GRUPO
            holder.usernameText.setText(model.getGroupName());
            holder.profilePic.setImageResource(R.drawable.chat_icon); // Ícone genérico para grupo
            holder.lastMessageTime.setText(FirebaseUtil.timestampToString(model.getLastMessageTimestamp()));
            holder.lastMessageText.setText(model.getLastMessage());
            holder.unreadCountText.setVisibility(View.GONE); // Ocultar contador por enquanto
        } else {
            // LÓGICA PARA CHAT INDIVIDUAL
            FirebaseUtil.getOtherUserFromChatroom(model.getUserIds())
                    .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            UserModel otherUserModel = task.getResult().toObject(UserModel.class);
                            if (otherUserModel == null) return;

                            // Passar o outro usuário para o intent, pois a ChatActivity precisará dele
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
                            if (lastMessageSentByMe) {
                                holder.lastMessageText.setText("You: " + model.getLastMessage());
                            } else {
                                holder.lastMessageText.setText(model.getLastMessage());
                            }
                            holder.lastMessageTime.setText(FirebaseUtil.timestampToString(model.getLastMessageTimestamp()));

                            String chatroomId = FirebaseUtil.getChatroomId(FirebaseUtil.currentUserId(), otherUserModel.getUserId());
                            FirebaseUtil.getChatroomMessageReference(chatroomId)
                                    .whereEqualTo("senderId", otherUserModel.getUserId())
                                    .whereEqualTo("status", ChatMessageModel.STATUS_SENT)
                                    .get()
                                    .addOnCompleteListener(countTask -> {
                                        if (countTask.isSuccessful()) {
                                            int unreadCount = countTask.getResult().size();
                                            if (unreadCount > 0) {
                                                holder.unreadCountText.setText(String.valueOf(unreadCount));
                                                holder.unreadCountText.setVisibility(View.VISIBLE);
                                            } else {
                                                holder.unreadCountText.setVisibility(View.GONE);
                                            }
                                        }
                                    });
                        }
                    });
        }

        // Definir o listener de clique para iniciar a Activity com o intent configurado
        holder.itemView.setOnClickListener(v -> {
            context.startActivity(intent);
        });
    }

    @NonNull
    @Override
    public ChatroomModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recent_chat_recycler_row, parent, false);
        return new ChatroomModelViewHolder(view);
    }

    class ChatroomModelViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText;
        TextView lastMessageText;
        TextView lastMessageTime;
        ImageView profilePic;
        TextView unreadCountText;

        public ChatroomModelViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.user_name_text);
            lastMessageText = itemView.findViewById(R.id.last_message_text);
            lastMessageTime = itemView.findViewById(R.id.last_message_time_text);
            profilePic = itemView.findViewById(R.id.profile_pic_image_view);
            unreadCountText = itemView.findViewById(R.id.unread_message_count_text);
        }
    }
}