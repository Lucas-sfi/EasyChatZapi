package com.example.easychat.adapter;

// Adicione as importações necessárias
import android.content.Context;
import android.content.Intent;
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
import com.example.easychat.utils.FirebaseUtil;
import java.util.List;

public class MessageSearchAdapter extends RecyclerView.Adapter<MessageSearchAdapter.MessageViewHolder> {

    private Context context;
    private List<ChatMessageModel> messageList;
    private List<ChatroomModel> chatroomList;

    public MessageSearchAdapter(Context context, List<ChatMessageModel> messageList, List<ChatroomModel> chatroomList) {
        this.context = context;
        this.messageList = messageList;
        this.chatroomList = chatroomList;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.search_message_recycler_row, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessageModel messageModel = messageList.get(position);
        ChatroomModel chatroomModel = chatroomList.get(position);

        holder.messageText.setText(messageModel.getMessage());

        if (chatroomModel.isGroupChat()) {
            holder.usernameText.setText(chatroomModel.getGroupName());
            holder.profilePic.setImageResource(R.drawable.chat_icon);
        } else {
            FirebaseUtil.getOtherUserFromChatroom(chatroomModel.getUserIds()).get().addOnSuccessListener(documentSnapshot -> {
                UserModel otherUser = documentSnapshot.toObject(UserModel.class);
                if (otherUser != null) {
                    holder.usernameText.setText(otherUser.getUsername());
                    // Lógica para foto de perfil pode ser adicionada aqui
                }
            });
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("chatroomId", chatroomModel.getChatroomId());
            // Passar o timestamp para a ChatActivity saber para onde navegar
            intent.putExtra("messageTimestamp", messageModel.getTimestamp().toDate().getTime());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText;
        TextView messageText;
        ImageView profilePic;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.user_name_text);
            messageText = itemView.findViewById(R.id.last_message_text);
            profilePic = itemView.findViewById(R.id.profile_pic_image_view);
        }
    }
}