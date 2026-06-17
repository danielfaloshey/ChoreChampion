package com.faloshey.chorechampion.models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class QuestModel {

    private String questId;
    private String title;
    private String description;
    private int goldReward;
    private int xpReward;
    private String assignedChildId;
    private String assignedChildName;
    private boolean isCompleted;
    private boolean isApproved;

    @ServerTimestamp
    private Date createdAt;

    public QuestModel() { }

    public QuestModel(String questId, String title, String description, int goldReward, int xpReward) {
        this.questId = questId;
        this.title = title;
        this.description = description;
        this.goldReward = goldReward;
        this.xpReward = xpReward;
        this.assignedChildId = "";
        this.assignedChildName = "Unassigned";
        this.isCompleted = false;
        this.isApproved = false;
    }

    public String getQuestId() { return questId; }
    public void setQuestId(String questId) { this.questId = questId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getGoldReward() { return goldReward; }
    public void setGoldReward(int goldReward) { this.goldReward = goldReward; }

    public int getXpReward() { return xpReward; }
    public void setXpReward(int xpReward) { this.xpReward = xpReward; }

    public String getAssignedChildId() { return assignedChildId; }
    public void setAssignedChildId(String assignedChildId) { this.assignedChildId = assignedChildId; }

    public String getAssignedChildName() { return assignedChildName; }
    public void setAssignedChildName(String assignedChildName) { this.assignedChildName = assignedChildName; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public boolean isApproved() { return isApproved; }
    public void setApproved(boolean approved) { isApproved = approved; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

}
