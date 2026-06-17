package com.faloshey.chorechampion.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.faloshey.chorechampion.R;

public class AvatarGridAdapter extends RecyclerView.Adapter<AvatarGridAdapter.AvatarViewHolder> {

    private int[] avatarIds;
    private int selectedIndex = -1;
    private final Context context;

    public AvatarGridAdapter(Context context, int[] initialPool) {
        this.context = context;
        this.avatarIds = initialPool;
    }

    public void updateDataPool(int[] newPool) {
        this.avatarIds = newPool;
        this.selectedIndex = -1;
        notifyDataSetChanged();
    }

    public int getSelectedResourceId() {
        if (selectedIndex >= 0 && selectedIndex < avatarIds.length) {
            return avatarIds[selectedIndex];
        }
        return -1;
    }

    @NonNull
    @Override
    public AvatarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context layoutContext = parent.getContext();
        ImageView imageView = new ImageView(layoutContext);

        int sizeInDp = (int) (80 * layoutContext.getResources().getDisplayMetrics().density);
        GridLayoutManager.LayoutParams params = new GridLayoutManager.LayoutParams(sizeInDp, sizeInDp);

        int marginInDp = (int) (8 * layoutContext.getResources().getDisplayMetrics().density);
        params.setMargins(marginInDp, marginInDp, marginInDp, marginInDp);
        imageView.setLayoutParams(params);

        imageView.setPadding(12, 12, 12, 12);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setFocusable(true);
        imageView.setClickable(true);

        return new AvatarViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull AvatarViewHolder holder, int position) {
        int currentResId = avatarIds[position];
        holder.avatarImage.setImageResource(currentResId);

        if (position == selectedIndex) {
            holder.avatarImage.setBackground(ContextCompat.getDrawable(context, R.drawable.avatar_selected_ring));
        } else {
            holder.avatarImage.setBackground(ContextCompat.getDrawable(context, R.drawable.avatar_default_box));
        }

        holder.avatarImage.setOnClickListener(v -> {
            int previousIndex = selectedIndex;
            selectedIndex = holder.getAdapterPosition();

            notifyItemChanged(previousIndex);
            notifyItemChanged(selectedIndex);
        });
    }

    @Override
    public int getItemCount() {
        return avatarIds.length;
    }

    class AvatarViewHolder extends RecyclerView.ViewHolder {
        final ImageView avatarImage;
        AvatarViewHolder(@NonNull View itemView) {
            super(itemView);
            this.avatarImage = (ImageView) itemView;
        }
    }
}
