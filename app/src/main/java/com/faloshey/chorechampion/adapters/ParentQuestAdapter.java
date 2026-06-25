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

public class ParentQuestAdapter extends RecyclerView.Adapter<ParentQuestAdapter.QuestViewHolder> {

    private final List<QuestModel> questList;
    private final OnQuestClickListener clickListener;
    private int selectedPosition = -1;

    public interface OnQuestClickListener {
        void onQuestSelected(QuestModel quest);
        void onQuestCleared();
    }

    public ParentQuestAdapter(List<QuestModel> questList, OnQuestClickListener clickListener) {
        this.questList = questList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public QuestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_parent_quest, parent, false);
        return new QuestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestViewHolder holder, int position) {
        QuestModel quest = questList.get(position);
        Context context = holder.itemView.getContext();

        holder.titleText.setText(quest.getTitle());
        holder.goldAmountText.setText(quest.getGoldReward() + " 🪙");
        holder.descriptionText.setText(quest.getDescription());

        if (quest.getAssignedChildName() != null && !quest.getAssignedChildName().isEmpty()) {
            holder.assignmentText.setText("Assigned to: " + quest.getAssignedChildName());
        }
        else {
            holder.assignmentText.setText("Assigned to: Unassigned");
        }


        if (position == selectedPosition) {

            holder.cardRoot.setStrokeColor(ContextCompat.getColor(context, R.color.new_blue));
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
                if (clickListener != null) clickListener.onQuestCleared();
            }
            else {
                selectedPosition = currentPosition;
                if (clickListener != null) clickListener.onQuestSelected(quest);
            }

            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
        });


    }

    @Override
    public int getItemCount() {
        return questList.size();
    }

    public QuestModel getSelectedQuest() {
        if (selectedPosition >= 0 && selectedPosition < questList.size()) {
            return questList.get(selectedPosition);
        }
        return null;
    }

    public void updateList(List<QuestModel> newQuests) {
        List<QuestModel> safeCopy = new ArrayList<>(newQuests);
        this.questList.clear();
        this.questList.addAll(safeCopy);
        this.selectedPosition = -1;
        notifyDataSetChanged();
    }


    static class QuestViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardRoot;
        TextView titleText, goldAmountText, descriptionText, assignmentText;

        public QuestViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.quest_card_root);
            titleText = itemView.findViewById(R.id.quest_title_text);
            goldAmountText = itemView.findViewById(R.id.quest_gold_amount);
            descriptionText = itemView.findViewById(R.id.quest_description_text);
            assignmentText = itemView.findViewById(R.id.quest_assignment_text);
        }
    }
}
