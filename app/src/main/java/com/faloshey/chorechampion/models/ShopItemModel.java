package com.faloshey.chorechampion.models;

public class ShopItemModel {

    private String itemId;
    private String title;
    private String description;
    private int cost;

    public ShopItemModel() { }

    public ShopItemModel(String itemId, String title, String description, int cost) {
        this.itemId = itemId;
        this.title = title;
        this.description = description;
        this.cost = cost;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getCost() { return cost; }
    public void setCost(int cost) { this.cost = cost; }
}
