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
import androidx.recyclerview.widget.RecyclerView;

import com.example.easychat.ChatActivity;
import com.example.easychat.R;
import com.example.easychat.model.UserModel;
import com.example.easychat.utils.AndroidUtil;
import com.example.easychat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.util.List;

public class SearchUserRecyclerAdapter extends FirestoreRecyclerAdapter<UserModel, SearchUserRecyclerAdapter.UserModelViewHolder> {

    Context context;
    List<String> contactIds; // Lista de IDs de contactos existentes

    // Construtor atualizado para receber a lista de contactos
    public SearchUserRecyclerAdapter(@NonNull FirestoreRecyclerOptions<UserModel> options, Context context, List<String> contactIds) {
        super(options);
        this.context = context;
        this.contactIds = contactIds;
    }

    @Override
    protected void onBindViewHolder(@NonNull UserModelViewHolder holder, int position, @NonNull UserModel model) {
        holder.usernameText.setText(model.getUsername());

        // Lógica para exibir telefone
        if(model.getPhone() != null && !model.getPhone().isEmpty()){
            holder.phoneText.setText(model.getPhone());
            holder.phoneText.setVisibility(View.VISIBLE);
        } else {
            holder.phoneText.setVisibility(View.GONE);
        }

        // Lógica para exibir e-mail
        if(model.getEmail() != null && !model.getEmail().isEmpty()){
            holder.emailText.setText(model.getEmail());
            holder.emailText.setVisibility(View.VISIBLE);
        } else {
            holder.emailText.setVisibility(View.GONE);
        }

        if(model.getUserId().equals(FirebaseUtil.currentUserId())){
            holder.usernameText.setText(model.getUsername()+" (Me)");
        }

        // LÓGICA PARA MOSTRAR O CERTINHO
        if (contactIds.contains(model.getUserId())) {
            holder.checkmarkIcon.setVisibility(View.VISIBLE);
        } else {
            holder.checkmarkIcon.setVisibility(View.GONE);
        }

        FirebaseUtil.getOtherProfilePicStorageRef(model.getUserId()).getDownloadUrl()
                .addOnCompleteListener(t -> {
                    if(t.isSuccessful()){
                        Uri uri  = t.getResult();
                        AndroidUtil.setProfilePic(context,uri,holder.profilePic);
                    }
                });

        holder.itemView.setOnClickListener(v -> {
            // A lógica de clique não muda, a ChatActivity já trata de conversas novas e existentes.
            Intent intent = new Intent(context, ChatActivity.class);
            AndroidUtil.passUserModelAsIntent(intent,model);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
    }

    @NonNull
    @Override
    public UserModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.search_user_recycler_row,parent,false);
        return new UserModelViewHolder(view);
    }

    static class UserModelViewHolder extends RecyclerView.ViewHolder{
        TextView usernameText;
        TextView phoneText;
        TextView emailText; // Novo campo
        ImageView profilePic;
        ImageView checkmarkIcon;

        public UserModelViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.user_name_text);
            phoneText = itemView.findViewById(R.id.phone_text);
            emailText = itemView.findViewById(R.id.email_text); // Inicialização
            profilePic = itemView.findViewById(R.id.profile_pic_image_view);
            checkmarkIcon = itemView.findViewById(R.id.checkmark_icon);
        }
    }
}