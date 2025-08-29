package com.example.easychat.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.easychat.R;
import com.example.easychat.model.ChatroomModel;
import com.example.easychat.model.UserModel;
import com.example.easychat.utils.AndroidUtil;
import com.example.easychat.utils.FirebaseUtil;
import java.util.List;

public class GroupMemberRecyclerAdapter extends RecyclerView.Adapter<GroupMemberRecyclerAdapter.UserModelViewHolder> {

    private Context context;
    private List<String> memberIds;
    private ChatroomModel chatroomModel;

    public GroupMemberRecyclerAdapter(Context context, List<String> memberIds, ChatroomModel chatroomModel) {
        this.context = context;
        this.memberIds = memberIds;
        this.chatroomModel = chatroomModel;
    }

    @NonNull
    @Override
    public UserModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.group_member_recycler_row, parent, false);
        return new UserModelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserModelViewHolder holder, int position) {
        String userId = memberIds.get(position);
        FirebaseUtil.allUserCollectionReference().document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                UserModel userModel = task.getResult().toObject(UserModel.class);
                if (userModel == null) return;

                holder.usernameText.setText(userModel.getUsername());
                FirebaseUtil.getOtherProfilePicStorageRef(userModel.getUserId()).getDownloadUrl()
                        .addOnCompleteListener(t -> {
                            if (t.isSuccessful()) {
                                Uri uri = t.getResult();
                                AndroidUtil.setProfilePic(context, uri, holder.profilePic);
                            }
                        });

                // Esconder o botão de remover para o próprio usuário
                if (userId.equals(FirebaseUtil.currentUserId())) {
                    holder.removeMemberBtn.setVisibility(View.GONE);
                } else {
                    holder.removeMemberBtn.setVisibility(View.VISIBLE);
                }

                holder.removeMemberBtn.setOnClickListener(v -> {
                    removeMember(userId);
                });
            }
        });
    }

    private void removeMember(String userId) {
        memberIds.remove(userId);
        chatroomModel.setUserIds(memberIds);
        FirebaseUtil.getChatroomReference(chatroomModel.getChatroomId()).set(chatroomModel)
                .addOnSuccessListener(aVoid -> {
                    notifyDataSetChanged();
                });
    }

    // MÉTODO NOVO ADICIONADO AQUI
    public void updateMembers(List<String> newMemberIds) {
        this.memberIds = newMemberIds;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return memberIds.size();
    }

    class UserModelViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText;
        ImageView profilePic;
        ImageButton removeMemberBtn;

        public UserModelViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.user_name_text);
            profilePic = itemView.findViewById(R.id.profile_pic_image_view);
            removeMemberBtn = itemView.findViewById(R.id.remove_member_btn);
        }
    }
}