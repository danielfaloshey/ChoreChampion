package com.faloshey.chorechampion.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.faloshey.chorechampion.R;
import com.faloshey.chorechampion.models.ShopItemModel;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class ParentInventoryAdapter extends RecyclerView.Adapter<ParentInventoryAdapter.InventoryViewHolder> {

    private List<ShopItemModel> rewardList;
    private final OnRewardLongClickListener longClickListener;

    public interface OnRewardLongClickListener {
        void onRewardLongClicked(ShopItemModel item, int position);
    }

    public ParentInventoryAdapter(List<ShopItemModel> rewardList, OnRewardLongClickListener longClickListener) {
        this.rewardList = rewardList;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_parent_shop, parent, false);
        return new InventoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        ShopItemModel item = rewardList.get(position);

        holder.titleText.setText(item.getTitle());
        holder.descriptionText.setText(item.getDescription());
        holder.costText.setText(item.getCost() + " 🪙");

        holder.cardRoot.setStrokeColor(Color.parseColor("#20000000"));
        holder.cardRoot.setStrokeWidth(4);
        holder.cardRoot.setCardBackgroundColor(Color.parseColor("#F5F5F5"));

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onRewardLongClicked(item, holder.getAdapterPosition());
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return rewardList.size();
    }

    public void updateList(List<ShopItemModel> newRewards) {
        this.rewardList = newRewards;
        notifyDataSetChanged();
    }

    static class InventoryViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardRoot;
        TextView titleText, costText, descriptionText;

        public InventoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.parent_shop_root);
            titleText = itemView.findViewById(R.id.shop_item_title);
            costText = itemView.findViewById(R.id.shop_item_cost);
            descriptionText = itemView.findViewById(R.id.shop_item_desc);
        }
    }
}
