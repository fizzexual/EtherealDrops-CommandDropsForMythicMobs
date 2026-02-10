package com.fizzexual.damagetracker.managers;

import com.fizzexual.damagetracker.DamageTracker;
import com.fizzexual.damagetracker.configs.RewardConfig;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages rewards distribution based on damage leaderboard positions.
 */
public class RewardManager {
    private final DamageTracker plugin;
    private final Map<String, RewardConfig> bossRewards;
    private File rewardsFile;
    private FileConfiguration rewardsConfig;

    public RewardManager(DamageTracker plugin) {
        this.plugin = plugin;
        this.bossRewards = new HashMap<>();
        loadRewardsFile();
    }

    private void loadRewardsFile() {
        rewardsFile = new File(plugin.getDataFolder(), "rewards.yml");
        if (!rewardsFile.exists()) {
            plugin.saveResource("rewards.yml", false);
        }
        rewardsConfig = YamlConfiguration.loadConfiguration(rewardsFile);
    }

    /**
     * Loads reward configurations for all bosses.
     */
    public void loadRewards() {
        bossRewards.clear();
        
        if (rewardsConfig == null) {
            loadRewardsFile();
        }

        ConfigurationSection bossesSection = rewardsConfig.getConfigurationSection("bosses");
        if (bossesSection == null) {
            plugin.getLogger().warning("No 'bosses' section found in rewards.yml");
            return;
        }

        for (String bossName : bossesSection.getKeys(false)) {
            ConfigurationSection bossSection = bossesSection.getConfigurationSection(bossName);
            if (bossSection == null) continue;

            RewardConfig config = new RewardConfig();
            config.setEnabled(bossSection.getBoolean("enabled", false));

            // Load rewards as a list of maps
            List<Map<?, ?>> rewardsList = bossSection.getMapList("rewards");
            if (rewardsList != null && !rewardsList.isEmpty()) {
                plugin.getLogger().info("Loading " + rewardsList.size() + " rewards for boss: " + bossName);
                
                for (Map<?, ?> rawMap : rewardsList) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> rewardMap = (Map<String, Object>) rawMap;
                    
                    RewardConfig.Reward reward = new RewardConfig.Reward();
                    reward.setType((String) rewardMap.getOrDefault("type", "item"));
                    reward.setRequiredPlace(((Number) rewardMap.getOrDefault("position", 1)).intValue());
                    
                    // Read boolean values - use helper method for consistent parsing
                    boolean inventory = parseBooleanValue(rewardMap, "inventory", true);
                    reward.setGiveToInventory(inventory);
                    
                    boolean perPlayer = parseBooleanValue(rewardMap, "per_player", false);
                    reward.setPerPlayerDrop(perPlayer);
                    
                    reward.setVisibility((String) rewardMap.getOrDefault("visibility", "all"));
                    
                    boolean glow = parseBooleanValue(rewardMap, "glow", false);
                    reward.setGlow(glow);
                    
                    plugin.getLogger().info("  Loaded reward: type=" + reward.getType() + 
                        ", position=" + reward.getRequiredPlace() + 
                        ", inventory=" + inventory + 
                        ", per_player=" + perPlayer + 
                        ", visibility=" + reward.getVisibility() +
                        ", glow=" + glow);

                    if ("item".equalsIgnoreCase(reward.getType())) {
                        String material = (String) rewardMap.get("material");
                        int amount = ((Number) rewardMap.getOrDefault("amount", 1)).intValue();
                        
                        if (material != null) {
                            try {
                                Material mat = Material.valueOf(material.toUpperCase());
                                ItemStack item = new ItemStack(mat, amount);
                                
                                if (reward.isGlow()) {
                                    ItemMeta meta = item.getItemMeta();
                                    if (meta != null) {
                                        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                                        item.setItemMeta(meta);
                                    }
                                }
                                
                                reward.setItem(item);
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("Invalid material: " + material);
                            }
                        }
                    } else if ("command".equalsIgnoreCase(reward.getType())) {
                        reward.setCommand((String) rewardMap.get("command"));
                    }

                    config.addReward(reward);
                }
            }

            bossRewards.put(bossName.toUpperCase(), config);
            plugin.getLogger().info("Loaded " + config.getRewards().size() + " rewards for boss: " + bossName + " (stored as: " + bossName.toUpperCase() + ")");
        }
        
        plugin.getLogger().info("Total bosses with rewards configured: " + bossRewards.size());
        plugin.getLogger().info("Boss names in rewards map: " + String.join(", ", bossRewards.keySet()));
    }

    /**
     * Distributes rewards to players based on their leaderboard position.
     */
    public void distributeRewards(String bossName, Map<UUID, Double> damageMap, double maxHealth) {
        plugin.getLogger().info("distributeRewards called for boss: " + bossName);
        plugin.getLogger().info("Looking up rewards with key: " + bossName.toUpperCase());
        plugin.getLogger().info("Available boss rewards: " + bossRewards.keySet());
        
        RewardConfig config = bossRewards.get(bossName.toUpperCase());
        
        if (config == null) {
            plugin.getLogger().warning("No reward config found for boss: " + bossName.toUpperCase());
            plugin.getLogger().warning("Make sure the boss name in rewards.yml matches your MythicMobs mob name exactly!");
            return;
        }
        
        if (!config.isEnabled()) {
            plugin.getLogger().info("Rewards are disabled for boss: " + bossName.toUpperCase());
            return;
        }
        
        plugin.getLogger().info("Reward config found and enabled for boss: " + bossName.toUpperCase() + " with " + config.getRewards().size() + " total rewards");

        List<Map.Entry<UUID, Double>> sortedPlayers = new ArrayList<>(damageMap.entrySet());
        sortedPlayers.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        // Track which shared drops we've already created (position -> reward)
        Map<Integer, Set<RewardConfig.Reward>> sharedDropsCreated = new HashMap<>();

        for (int i = 0; i < sortedPlayers.size(); i++) {
            Map.Entry<UUID, Double> entry = sortedPlayers.get(i);
            Player player = Bukkit.getPlayer(entry.getKey());
            
            if (player == null || !player.isOnline()) continue;

            int position = i + 1;
            List<RewardConfig.Reward> positionRewards = config.getRewardsForPosition(position);

            plugin.getLogger().info("Distributing " + positionRewards.size() + " rewards to " + player.getName() + " for position " + position + " on boss " + bossName);

            for (RewardConfig.Reward reward : positionRewards) {
                plugin.getLogger().info("Processing reward - Type: " + reward.getType() + 
                    ", inventory: " + reward.isGiveToInventory() + 
                    ", per_player: " + reward.isPerPlayerDrop() + 
                    ", Item: " + (reward.getItem() != null ? reward.getItem().getType() : "null"));
                
                // Handle shared drops (only create once per position)
                if ("item".equalsIgnoreCase(reward.getType()) && 
                    !reward.isGiveToInventory() && 
                    !reward.isPerPlayerDrop()) {
                    
                    plugin.getLogger().info("This is a SHARED DROP reward");
                    
                    // Check if we already created this shared drop for this position
                    Set<RewardConfig.Reward> dropsForPosition = sharedDropsCreated.computeIfAbsent(position, k -> new HashSet<>());
                    
                    if (!dropsForPosition.contains(reward)) {
                        if (reward.getItem() != null) {
                            // Create the shared drop
                            org.bukkit.entity.Item droppedItem = player.getWorld().dropItem(player.getLocation(), reward.getItem());
                            
                            if ("player_only".equalsIgnoreCase(reward.getVisibility())) {
                                // For shared drops with player_only visibility, set owner to first player at this position
                                droppedItem.setOwner(player.getUniqueId());
                                droppedItem.setPickupDelay(0);
                            }
                            
                            dropsForPosition.add(reward);
                            plugin.getLogger().info("Created shared drop for position " + position + " at " + player.getLocation() + " (visibility: " + reward.getVisibility() + ")");
                        } else {
                            plugin.getLogger().warning("Cannot create shared drop - item is null!");
                        }
                    } else {
                        plugin.getLogger().info("Shared drop already created for position " + position);
                    }
                } else if ("item".equalsIgnoreCase(reward.getType()) && 
                           !reward.isGiveToInventory() && 
                           reward.isPerPlayerDrop()) {
                    plugin.getLogger().info("This is a PER-PLAYER DROP reward");
                    // Handle per-player drops
                    giveReward(player, reward, bossName, position);
                } else {
                    plugin.getLogger().info("This is an INVENTORY or COMMAND reward");
                    // Handle inventory rewards and commands
                    giveReward(player, reward, bossName, position);
                }
            }
        }
    }

    /**
     * Gives a reward to a player.
     */
    private void giveReward(Player player, RewardConfig.Reward reward, String bossName, int position) {
        try {
            plugin.getLogger().info("giveReward called for " + player.getName() + " - Type: " + reward.getType() + 
                ", inventory: " + reward.isGiveToInventory() + 
                ", per_player: " + reward.isPerPlayerDrop());
            
            if ("item".equalsIgnoreCase(reward.getType()) && reward.getItem() != null) {
                if (reward.isGiveToInventory()) {
                    // Give to inventory
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(reward.getItem());
                    if (!leftover.isEmpty()) {
                        // If inventory is full, drop at player location
                        for (ItemStack drop : leftover.values()) {
                            player.getWorld().dropItem(player.getLocation(), drop);
                        }
                    }
                    plugin.getLogger().info("Gave item reward to inventory for " + player.getName() + " at position " + position);
                } else {
                    plugin.getLogger().info("Item should be dropped on ground (inventory=false)");
                    // Drop on ground
                    if (reward.isPerPlayerDrop()) {
                        // Each player gets their own drop
                        org.bukkit.entity.Item droppedItem = player.getWorld().dropItem(player.getLocation(), reward.getItem());
                        
                        if ("player_only".equalsIgnoreCase(reward.getVisibility())) {
                            // Make it visible only to this player
                            droppedItem.setOwner(player.getUniqueId());
                            // Set pickup delay so only owner can pick it up initially
                            droppedItem.setPickupDelay(0);
                        }
                        
                        plugin.getLogger().info("Dropped per-player item for " + player.getName() + " at position " + position + " (visibility: " + reward.getVisibility() + ")");
                    } else {
                        // Single drop for all players at this position (only drop once)
                        // This will be handled by checking if we already dropped for this position
                        plugin.getLogger().info("Skipping shared drop for " + player.getName() + " - handled separately");
                    }
                }
            } else if ("command".equalsIgnoreCase(reward.getType()) && reward.getCommand() != null) {
                String processedCommand = reward.getCommand()
                        .replace("{player}", player.getName())
                        .replace("{boss}", bossName)
                        .replace("{position}", String.valueOf(position));
                
                plugin.getLogger().info("Executing command: " + processedCommand);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
                plugin.getLogger().info("Executed command reward for " + player.getName() + " for position " + position);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error giving reward to " + player.getName(), e);
        }
    }

    /**
     * Gets the reward configuration for a boss.
     */
    public RewardConfig getRewardConfig(String bossName) {
        return bossRewards.get(bossName.toUpperCase());
    }

    /**
     * Checks if rewards are enabled for a boss.
     */
    public boolean hasRewards(String bossName) {
        RewardConfig config = bossRewards.get(bossName.toUpperCase());
        return config != null && config.isEnabled();
    }

    /**
     * Helper method to parse boolean values from YAML map with proper type handling.
     */
    private boolean parseBooleanValue(Map<String, Object> map, String key, boolean defaultValue) {
        if (!map.containsKey(key)) {
            plugin.getLogger().info("    Key '" + key + "' not found in map, using default: " + defaultValue);
            return defaultValue;
        }
        
        Object value = map.get(key);
        
        // Log the raw value for debugging
        plugin.getLogger().info("    Parsing '" + key + "': value=" + value + 
            ", class=" + (value != null ? value.getClass().getSimpleName() : "null"));
        
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            String strValue = (String) value;
            return "true".equalsIgnoreCase(strValue) || "yes".equalsIgnoreCase(strValue);
        } else if (value instanceof Number) {
            // Handle numeric values (1 = true, 0 = false)
            return ((Number) value).intValue() != 0;
        }
        
        plugin.getLogger().warning("    Unexpected type for boolean key '" + key + "': " + 
            (value != null ? value.getClass().getName() : "null") + ", using default: " + defaultValue);
        return defaultValue;
    }
}
