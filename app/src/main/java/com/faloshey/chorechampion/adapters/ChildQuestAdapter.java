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
import com.faloshey.chorechampion.models.QuestModel;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class ChildQuestAdapter extends RecyclerView.Adapter<ChildQuestAdapter.ViewHolder> {

    private final List<QuestModel> displayedQuests;
    private final OnQuestClickListener listener;
    private int selectedPosition = -1;
    private boolean isQuestBoardTab = false;

    public interface OnQuestClickListener {
        void onQuestClick(QuestModel quest);
        void onSelectedCleared();
    }

    public ChildQuestAdapter(List<QuestModel> displayedQuests, OnQuestClickListener listener) {
        this.displayedQuests = displayedQuests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChildQuestAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_child_quest, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChildQuestAdapter.ViewHolder holder, int position) {
        QuestModel quest = displayedQuests.get(position);
        Context context = holder.itemView.getContext();

        holder.titleText.setText(quest.getTitle());
        holder.goldText.setText(String.valueOf(quest.getGoldReward()));
        holder.descText.setText(quest.getDescription());

        if (position == selectedPosition) {
            int activeColor = ContextCompat.getColor(context, isQuestBoardTab ? R.color.new_blue : R.color.forest_green);
            holder.cardRoot.setStrokeColor(activeColor);
            holder.cardRoot.setStrokeWidth(6);
            holder.cardRoot.setCardBackgroundColor(Color.parseColor("#F5F9FF"));
        }
        else {
            holder.cardRoot.setStrokeColor(ContextCompat.getColor(context, R.color.black));
            holder.cardRoot.setStrokeWidth(4);
            holder.cardRoot.setCardBackgroundColor(Color.parseColor("#EAEAEA"));
        }

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            int currentPosition = holder.getAdapterPosition();

            if (selectedPosition == currentPosition) {
                selectedPosition = -1;
                listener.onSelectedCleared();
            }
            else {
                selectedPosition = currentPosition;
                listener.onQuestClick(quest);
            }
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
        });
    }

    @Override
    public int getItemCount() {
        return displayedQuests.size();
    }

    public void updateData(List<QuestModel> newList, boolean isQuestBoard) {
        List<QuestModel> temporaryCopy = new ArrayList<>(newList);

        this.displayedQuests.clear();
        this.displayedQuests.addAll(temporaryCopy);

        this.isQuestBoardTab = isQuestBoard;
        this.selectedPosition = -1;
        notifyDataSetChanged();
    }

    public void clearSelection() {
        int oldPos = selectedPosition;
        selectedPosition = -1;
        if (oldPos != -1) {
            notifyItemChanged(oldPos);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardRoot;
        TextView titleText, goldText, descText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.child_quest_card_root);
            titleText = itemView.findViewById(R.id.child_quest_title);
            goldText = itemView.findViewById(R.id.child_quest_gold_amount);
            descText = itemView.findViewById(R.id.child_quest_description);
        }
    }
}
