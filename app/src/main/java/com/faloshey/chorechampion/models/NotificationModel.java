package com.faloshey.chorechampion.models;

public class NotificationModel {

    private String id;
    private String username;
    private String actionText;
    private String type;
    private long timestamp;

    public NotificationModel() {}

    public NotificationModel(String id, String username, String actionText, String type, long timestamp) {
        this.id = id;
        this.username = username;
        this.actionText = actionText;
        this.type = type;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getActionText() { return actionText; }
    public void setActionText(String actionText) { this.actionText = actionText; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
