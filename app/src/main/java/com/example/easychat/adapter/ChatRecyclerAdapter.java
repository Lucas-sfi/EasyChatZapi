package com.example.easychat.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.easychat.R;
import com.example.easychat.model.ChatMessageModel;
import com.example.easychat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class ChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatMessageModel, ChatRecyclerAdapter.ChatModelViewHolder> {

    Context context;
    private String highlightedMessageId = null;
    private PinMessageListener pinMessageListener; // Interface para comunicar com a Activity

    public ChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatMessageModel> options, Context context, PinMessageListener listener) {
        super(options);
        this.context = context;
        this.pinMessageListener = listener;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatModelViewHolder holder, int position, @NonNull ChatMessageModel model) {
        String currentMessageId = getSnapshots().getSnapshot(position).getId();

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

        // Listener para clique longo na mensagem
        holder.itemView.setOnLongClickListener(v -> {
            showPinMessageDialog(currentMessageId);
            return true;
        });
    }

    private void showPinMessageDialog(String messageId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setItems(new CharSequence[]{"Pin Message"}, (dialog, which) -> {
            if (which == 0) {
                pinMessageListener.onPinMessageClicked(messageId);
            }
        });
        builder.create().show();
    }

    @NonNull
    @Override
    public ChatModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_message_recycler_row, parent, false);
        return new ChatModelViewHolder(view);
    }

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

    // Interface para comunicar a ação de fixar
    public interface PinMessageListener {
        void onPinMessageClicked(String messageId);
    }
}