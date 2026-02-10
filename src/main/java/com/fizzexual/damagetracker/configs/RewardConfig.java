package com.fizzexual.damagetracker.configs;

import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Configuration for boss rewards based on leaderboard position.
 */
public class RewardConfig {
    private boolean enabled;
    private final List<Reward> rewards;

    public RewardConfig() {
        this.enabled = false;
        this.rewards = new ArrayList<>();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void addReward(Reward reward) {
        rewards.add(reward);
    }

    public List<Reward> getRewards() {
        return Collections.unmodifiableList(rewards);
    }

    public List<Reward> getRewardsForPosition(int position) {
        List<Reward> positionRewards = new ArrayList<>();
        for (Reward reward : rewards) {
            if (reward.getRequiredPlace() == position) {
                positionRewards.add(reward);
            }
        }
        return positionRewards;
    }

    /**
     * Represents a single reward.
     */
    public static class Reward {
        private String type; // "item" or "command"
        private int requiredPlace;
        private boolean giveToInventory; // true = inventory, false = drop on ground
        private boolean perPlayerDrop; // if dropping, whether each player gets their own drop
        private String visibility; // "all" or "player_only" - who can see/pick up the drop
        private boolean glow;
        private ItemStack item;
        private String command;

        public Reward() {
            this.type = "item";
            this.requiredPlace = 1;
            this.giveToInventory = true;
            this.perPlayerDrop = false;
            this.visibility = "all";
            this.glow = false;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getRequiredPlace() {
            return requiredPlace;
        }

        public void setRequiredPlace(int requiredPlace) {
            this.requiredPlace = requiredPlace;
        }

        public boolean isGiveToInventory() {
            return giveToInventory;
        }

        public void setGiveToInventory(boolean giveToInventory) {
            this.giveToInventory = giveToInventory;
        }

        public boolean isPerPlayerDrop() {
            return perPlayerDrop;
        }

        public void setPerPlayerDrop(boolean perPlayerDrop) {
            this.perPlayerDrop = perPlayerDrop;
        }

        public String getVisibility() {
            return visibility;
        }

        public void setVisibility(String visibility) {
            this.visibility = visibility;
        }

        public boolean isGlow() {
            return glow;
        }

        public void setGlow(boolean glow) {
            this.glow = glow;
        }

        public ItemStack getItem() {
            return item;
        }

        public void setItem(ItemStack item) {
            this.item = item;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }
    }
}
