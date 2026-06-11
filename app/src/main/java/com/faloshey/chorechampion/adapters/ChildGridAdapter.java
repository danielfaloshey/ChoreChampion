package com.faloshey.chorechampion.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.faloshey.chorechampion.R;
import com.faloshey.chorechampion.models.ChildModel;
import java.util.List;

@SuppressWarnings("ClassEscapesDefinedScope")
public class ChildGridAdapter extends RecyclerView.Adapter<ChildGridAdapter.ChildViewHolder> {

    private final List<ChildModel> childList;
    private final OnChildClickListener clickListener;

    public interface OnChildClickListener {
        void onChildClick(ChildModel child);
    }

    public ChildGridAdapter(List<ChildModel> childList, OnChildClickListener clickListener) {
        this.childList = childList;
        this.clickListener = clickListener;

    }

    @NonNull
    @Override
    public ChildGridAdapter.ChildViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_child_tile, parent, false);
        return new ChildViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChildGridAdapter.ChildViewHolder holder, int position) {
        ChildModel child = childList.get(position);
        holder.nameText.setText(child.getUsername());

        //TODO: Change when Lordicon is ready
        holder.avatarImage.setImageResource(R.drawable.ic_placeholder_user);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onChildClick(child);
        });
    }

    @Override
    public int getItemCount() {
        return childList.size();
    }

    static class ChildViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarImage;
        TextView nameText;

        public ChildViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImage = itemView.findViewById(R.id.knight_avatar);
            nameText = itemView.findViewById(R.id.knight_name);
        }
    }
}
