package com.example.easychat.adapter;

import android.content.Context;
import android.content.Intent;
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

import com.bumptech.glide.Glide;
import com.example.easychat.FullSizeImageActivity;
import com.example.easychat.R;
import com.example.easychat.model.ChatMessageModel;
import com.example.easychat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class ChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatMessageModel, ChatRecyclerAdapter.ChatModelViewHolder> {

    Context context;
    private String highlightedMessageId = null;
    private PinMessageListener pinMessageListener;

    public ChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatMessageModel> options, Context context, PinMessageListener listener) {
        super(options);
        this.context = context;
        this.pinMessageListener = listener;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatModelViewHolder holder, int position, @NonNull ChatMessageModel model) {
        String currentMessageId = getSnapshots().getSnapshot(position).getId();

        // Lógica do destaque
        if (currentMessageId.equals(highlightedMessageId)) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.my_secondary));
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        boolean isImage = "image".equals(model.getType());

        if (model.getSenderId().equals(FirebaseUtil.currentUserId())) {
            // MENSAGEM ENVIADA (DIREITA)
            holder.leftChatLayout.setVisibility(View.GONE);
            holder.rightChatLayout.setVisibility(View.VISIBLE);

            if (isImage) {
                // Se for imagem
                holder.rightChatTextview.setVisibility(View.GONE);
                holder.rightChatImageView.setVisibility(View.VISIBLE);
                Glide.with(context).load(model.getMessage()).into(holder.rightChatImageView);
            } else {
                // Se for texto
                holder.rightChatImageView.setVisibility(View.GONE);
                holder.rightChatTextview.setVisibility(View.VISIBLE);
                holder.rightChatTextview.setText(model.getMessage());
            }

            // Lógica do status da mensagem (só para mensagens de texto)
            if (!isImage) {
                holder.statusIcon.setVisibility(View.VISIBLE);
                if (model.getStatus() == ChatMessageModel.STATUS_READ) {
                    holder.statusIcon.setImageResource(R.drawable.ic_status_read);
                    holder.statusIcon.setColorFilter(ContextCompat.getColor(context, R.color.my_secondary));
                } else {
                    holder.statusIcon.setImageResource(R.drawable.ic_status_sent);
                    holder.statusIcon.setColorFilter(Color.GRAY);
                }
            } else {
                holder.statusIcon.setVisibility(View.GONE); // Esconde o status para imagens
            }


        } else {
            // MENSAGEM RECEBIDA (ESQUERDA)
            holder.rightChatLayout.setVisibility(View.GONE);
            holder.leftChatLayout.setVisibility(View.VISIBLE);

            if (isImage) {
                // Se for imagem
                holder.leftChatTextview.setVisibility(View.GONE);
                holder.leftChatImageView.setVisibility(View.VISIBLE);
                Glide.with(context).load(model.getMessage()).into(holder.leftChatImageView);
            } else {
                // Se for texto
                holder.leftChatImageView.setVisibility(View.GONE);
                holder.leftChatTextview.setVisibility(View.VISIBLE);
                holder.leftChatTextview.setText(model.getMessage());
            }
        }

        // LÓGICA DE CLIQUE
        if (isImage) {
            // Se for uma imagem, o clique abre a tela de visualização
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, FullSizeImageActivity.class);
                intent.putExtra("imageUrl", model.getMessage());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            });
        } else {
            // Se for texto, não faz nada ao clicar
            holder.itemView.setOnClickListener(null);
        }

        // Lógica de clique longo para fixar
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
        ImageView leftChatImageView, rightChatImageView;
        ImageView statusIcon;

        public ChatModelViewHolder(@NonNull View itemView) {
            super(itemView);
            leftChatLayout = itemView.findViewById(R.id.left_chat_layout);
            rightChatLayout = itemView.findViewById(R.id.right_chat_layout);
            leftChatTextview = itemView.findViewById(R.id.left_chat_textview);
            rightChatTextview = itemView.findViewById(R.id.right_chat_textview);
            leftChatImageView = itemView.findViewById(R.id.left_chat_imageview);
            rightChatImageView = itemView.findViewById(R.id.right_chat_imageview);
            statusIcon = itemView.findViewById(R.id.message_status_icon);
        }
    }

    public interface PinMessageListener {
        void onPinMessageClicked(String messageId);
    }
}