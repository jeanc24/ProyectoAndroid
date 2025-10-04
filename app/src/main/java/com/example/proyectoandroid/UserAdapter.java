package com.example.proyectoandroid;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.proyectoandroid.R;
import com.example.proyectoandroid.data.model.User;
import com.squareup.picasso.Picasso;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    private List<User> userList;
    private OnUserClickListener listener;

    private boolean groupMode = false;
    private final Set<String> selectedUserIds = new HashSet<>();
    private OnSelectionChangedListener selectionChangedListener;

    public UserAdapter(List<User> userList, OnUserClickListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener l) {
        this.selectionChangedListener = l;
    }

    public void setGroupMode(boolean enabled) {
        if (this.groupMode != enabled) {
            this.groupMode = enabled;
            if (!enabled) {
                selectedUserIds.clear();
                if (selectionChangedListener != null) selectionChangedListener.onSelectionChanged(0);
            }
            notifyDataSetChanged();
        }
    }

    public boolean isGroupMode() {
        return groupMode;
    }

    public Set<String> getSelectedUserIds() {
        return new HashSet<>(selectedUserIds);
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        holder.tvUserName.setText(user.getDisplayName());
        holder.tvUserEmail.setText(user.getEmail());
        // Si tienes imagen de perfil, la cargas así:
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Picasso.get().load(user.getProfileImageUrl()).into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_launcher_foreground);
        }

        holder.cbSelect.setVisibility(groupMode ? View.VISIBLE : View.GONE);
        boolean checked = selectedUserIds.contains(user.getUid());
        holder.cbSelect.setOnCheckedChangeListener(null);
        holder.cbSelect.setChecked(checked);
        holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleSelection(user, isChecked);
        });

        holder.itemView.setOnClickListener(v -> {
            if (groupMode) {
                boolean newState = !holder.cbSelect.isChecked();
                holder.cbSelect.setChecked(newState);
            } else {
                if (listener != null) listener.onUserClick(user);
            }
        });
    }

    private void toggleSelection(User user, boolean isChecked) {
        if (user == null || user.getUid() == null) return;
        if (isChecked) selectedUserIds.add(user.getUid());
        else selectedUserIds.remove(user.getUid());
        if (selectionChangedListener != null) selectionChangedListener.onSelectionChanged(selectedUserIds.size());
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvUserName, tvUserEmail;
        CheckBox cbSelect;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            cbSelect = itemView.findViewById(R.id.cbSelect);
        }
    }
}