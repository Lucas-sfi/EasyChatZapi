package com.example.easychat.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.easychat.R;
import com.example.easychat.model.UserModel;
import com.example.easychat.utils.AndroidUtil;
import com.example.easychat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import java.util.ArrayList;

public class SelectUserRecyclerAdapter extends FirestoreRecyclerAdapter<UserModel, SelectUserRecyclerAdapter.UserModelViewHolder> {

    private Context context;
    private ArrayList<String> selectedUserIds;
    private ArrayList<String> currentMembers; // Lista de membros já existentes

    public SelectUserRecyclerAdapter(@NonNull FirestoreRecyclerOptions<UserModel> options, Context context, ArrayList<String> currentMembers) {
        super(options);
        this.context = context;
        this.selectedUserIds = new ArrayList<>();
        this.currentMembers = currentMembers;
    }

    @Override
    protected void onBindViewHolder(@NonNull UserModelViewHolder holder, int position, @NonNull UserModel model) {
        // Se o usuário já for um membro, oculta o item da lista
        if (currentMembers.contains(model.getUserId())) {
            holder.itemView.setVisibility(View.GONE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
            return;
        }

        holder.itemView.setVisibility(View.VISIBLE);
        holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        holder.usernameText.setText(model.getUsername());
        holder.phoneText.setText(model.getPhone());

        FirebaseUtil.getOtherProfilePicStorageRef(model.getUserId()).getDownloadUrl()
                .addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        Uri uri = t.getResult();
                        AndroidUtil.setProfilePic(context, uri, holder.profilePic);
                    }
                });

        holder.checkBox.setChecked(selectedUserIds.contains(model.getUserId()));

        holder.itemView.setOnClickListener(v -> {
            holder.checkBox.setChecked(!holder.checkBox.isChecked());
        });

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String userId = model.getUserId();
            if (isChecked) {
                if (!selectedUserIds.contains(userId)) {
                    selectedUserIds.add(userId);
                }
            } else {
                selectedUserIds.remove(userId);
            }
        });
    }

    @NonNull
    @Override
    public UserModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.select_user_recycler_row, parent, false);
        return new UserModelViewHolder(view);
    }

    public ArrayList<String> getSelectedUserIds() {
        return selectedUserIds;
    }

    class UserModelViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText;
        TextView phoneText;
        ImageView profilePic;
        CheckBox checkBox;

        public UserModelViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.user_name_text);
            phoneText = itemView.findViewById(R.id.phone_text);
            profilePic = itemView.findViewById(R.id.profile_pic_image_view);
            checkBox = itemView.findViewById(R.id.checkbox);
        }
    }
}