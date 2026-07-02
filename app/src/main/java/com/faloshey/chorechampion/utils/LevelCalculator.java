package com.faloshey.chorechampion.utils;

public class LevelCalculator {

    public static class LevelData {
        public final int currentLevel;
        public final int xpProgressInCurrentLevel;
        public final int xpRequiredForNextLevel;

        public LevelData(int currentLevel, int xpProgressInCurrentLevel, int xpRequiredForNextLevel) {
            this.currentLevel = currentLevel;
            this.xpProgressInCurrentLevel = xpProgressInCurrentLevel;
            this.xpRequiredForNextLevel = xpRequiredForNextLevel;
        }
    }

    public static LevelData evaluate(int totalXp) {
        int level = 1;
        int xpNeededForNextLevel = 100;
        int remainingXp = totalXp;

        while (remainingXp >= xpNeededForNextLevel) {
            remainingXp -= xpNeededForNextLevel;
            level++;
            xpNeededForNextLevel += 50;
        }

        return new LevelData(level, remainingXp, xpNeededForNextLevel);
    }
}
