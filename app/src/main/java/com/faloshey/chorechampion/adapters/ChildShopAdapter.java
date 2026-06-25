package com.faloshey.chorechampion.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.faloshey.chorechampion.R;
import com.faloshey.chorechampion.models.ShopItemModel;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

public class ChildShopAdapter extends RecyclerView.Adapter<ChildShopAdapter.ShopViewHolder> {

    private final List<ShopItemModel> itemList;
    private final OnItemClickListener clickListener;
    private int selectedPosition = -1;

    public interface OnItemClickListener {
        void onItemSelected(ShopItemModel item);
        void onItemCleared();
    }

    public ChildShopAdapter(List<ShopItemModel> itemList, OnItemClickListener clickListener) {
        this.itemList = new ArrayList<>(itemList);
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ShopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_parent_shop, parent, false);
        return new ShopViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShopViewHolder holder, int position) {
        ShopItemModel item = itemList.get(position);
        Context context = holder.itemView.getContext();

        holder.titleText.setText(item.getTitle());
        holder.costText.setText(item.getCost() + " 🪙");
        holder.descriptionText.setText(item.getDescription());

        if (position == selectedPosition) {
            holder.cardRoot.setStrokeColor(ContextCompat.getColor(context, R.color.forest_green));
            holder.cardRoot.setStrokeWidth(6);
            holder.cardRoot.setCardBackgroundColor(Color.parseColor("#F5F9FF"));
        } else {
            holder.cardRoot.setStrokeColor(Color.parseColor("#20000000"));
            holder.cardRoot.setStrokeWidth(4);
            holder.cardRoot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.glassWhite));
        }

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            int currentPosition = holder.getAdapterPosition();

            if (selectedPosition == currentPosition) {
                selectedPosition = -1;
                if (clickListener != null) clickListener.onItemCleared();
            } else {
                selectedPosition = currentPosition;
                if (clickListener != null) clickListener.onItemSelected(item);
            }

            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public void updateList(List<ShopItemModel> newItems) {
        List<ShopItemModel> safeCopy = new ArrayList<>(newItems);
        this.itemList.clear();
        this.itemList.addAll(safeCopy);
        this.selectedPosition = -1;
        notifyDataSetChanged();
    }

    static class ShopViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardRoot;
        TextView titleText, costText, descriptionText;

        public ShopViewHolder(@NonNull View itemView) {
            super(itemView);

            cardRoot = itemView.findViewById(R.id.parent_shop_root);
            titleText = itemView.findViewById(R.id.shop_item_title);
            costText = itemView.findViewById(R.id.shop_item_cost);
            descriptionText = itemView.findViewById(R.id.shop_item_desc);
        }
    }
}
