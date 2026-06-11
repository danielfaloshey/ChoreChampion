package com.faloshey.chorechampion.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChildModel {

    private String childId;
    private String username;
    private String avatarIconId;
    private int gold;
    private int xp;
    private List<String> rewards;
    private List<String> quests;

    @ServerTimestamp
    private Date createdAt;

    public ChildModel() { }

    public ChildModel(String childId, String username) {
        this.childId = childId;
        this.username = username;
        this.avatarIconId = null;
        this.gold = 0;
        this.xp = 0;
        this.rewards = new ArrayList<>();
        this.quests = new ArrayList<>();
    }

    public ChildModel(String childId, String username, String avatarIconId, int gold, int xp,
                      List<String> rewards, List<String> quests, Date createdAt) {
        this.childId = childId;
        this.username = username;
        this.avatarIconId = avatarIconId;
        this.gold = gold;
        this.xp = xp;
        this.rewards = rewards;
        this.quests = quests;
        this.createdAt = createdAt;
    }

    public String getChildId() { return childId; }
    public void setChildId(String childId) { this.childId = childId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAvatarIconId() { return avatarIconId; }
    public void setAvatarIconId(String avatarIconId) { this.avatarIconId = avatarIconId; }

    public int getGold() { return gold; }
    public void setGold(int gold) { this.gold = gold; }

    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }

    public List<String> getRewards() { return rewards; }
    public void setRewards(List<String> rewards) { this.rewards = rewards; }

    public List<String> getQuests() { return quests; }
    public void setQuests(List<String> quests) { this.quests = quests; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

}
