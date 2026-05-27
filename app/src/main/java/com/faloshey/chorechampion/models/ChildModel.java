package com.faloshey.chorechampion.models;

public class ChildModel {

    // TODO: Needs other properties added later (gold, xp, etc)

    private String childId;
    private String username;
    private int avatarIconId;

    // Required for Firestore
    public ChildModel() { }

    public ChildModel(String childId, String username, int avatarIconId) {
        this.childId = childId;
        this.username = username;
        this.avatarIconId = avatarIconId;
    }

    public String getChildId() { return childId; }

    public String getUsername() { return username; }

    public int getAvatarIconId() { return avatarIconId; }

    public void setChildId (String childId) {
        this.childId = childId;
    }

    public void setUsername (String username) {
        this.username = username;
    }

    public void setAvatarIconId (int avatarIconId) {
        this.avatarIconId = avatarIconId;
    }


}
