package com.example.easychat.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easychat.R;
import com.example.easychat.model.ChatMessageModel;
import com.example.easychat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class ChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatMessageModel, ChatRecyclerAdapter.ChatModelViewHolder> {

    Context context;
    private String highlightedMessageId = null; // ID da mensagem a ser destacada

    public ChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatMessageModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatModelViewHolder holder, int position, @NonNull ChatMessageModel model) {
        String currentMessageId = getSnapshots().getSnapshot(position).getId();

        // LÓGICA DE DESTAQUE
        if (currentMessageId.equals(highlightedMessageId)) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.my_secondary));
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        if (model.getSenderId().equals(FirebaseUtil.currentUserId())) {
            holder.leftChatLayout.setVisibility(View.GONE);
            holder.rightChatLayout.setVisibility(View.VISIBLE);
            holder.rightChatTextview.setText(model.getMessage());

            if (model.getStatus() == ChatMessageModel.STATUS_READ) {
                holder.statusIcon.setImageResource(R.drawable.ic_status_read);
                holder.statusIcon.setColorFilter(ContextCompat.getColor(context, R.color.my_secondary));
            } else {
                holder.statusIcon.setImageResource(R.drawable.ic_status_sent);
                holder.statusIcon.setColorFilter(Color.GRAY);
            }

        } else {
            holder.rightChatLayout.setVisibility(View.GONE);
            holder.leftChatLayout.setVisibility(View.VISIBLE);
            holder.leftChatTextview.setText(model.getMessage());
        }
    }

    @NonNull
    @Override
    public ChatModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_message_recycler_row, parent, false);
        return new ChatModelViewHolder(view);
    }

    // Método para definir qual mensagem destacar
    public void highlightMessage(String messageId) {
        highlightedMessageId = messageId;
        notifyDataSetChanged();
    }

    static class ChatModelViewHolder extends RecyclerView.ViewHolder {
        LinearLayout leftChatLayout, rightChatLayout;
        TextView leftChatTextview, rightChatTextview;
        ImageView statusIcon;

        public ChatModelViewHolder(@NonNull View itemView) {
            super(itemView);
            leftChatLayout = itemView.findViewById(R.id.left_chat_layout);
            rightChatLayout = itemView.findViewById(R.id.right_chat_layout);
            leftChatTextview = itemView.findViewById(R.id.left_chat_textview);
            rightChatTextview = itemView.findViewById(R.id.right_chat_textview);
            statusIcon = itemView.findViewById(R.id.message_status_icon);
        }
    }
}