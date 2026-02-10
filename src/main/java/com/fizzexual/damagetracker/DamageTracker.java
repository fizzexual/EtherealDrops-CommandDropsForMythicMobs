package com.fizzexual.damagetracker;

import com.fizzexual.damagetracker.commands.DamageTrackerCommand;
import com.fizzexual.damagetracker.configs.BossConfig;
import com.fizzexual.damagetracker.listeners.MythicMobListeners;
import com.fizzexual.damagetracker.managers.DamageManager;
import com.fizzexual.damagetracker.managers.DatabaseManager;
import com.fizzexual.damagetracker.managers.TrackedBossManager;
import com.fizzexual.damagetracker.managers.VictoryMessageManager;
import com.fizzexual.damagetracker.managers.RewardManager;
import com.fizzexual.damagetracker.placeholders.DamageTrackerPlaceholder;
import com.fizzexual.damagetracker.utils.MessageUtils;
import com.fizzexual.damagetracker.managers.HologramManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.chat.Chat;
import java.util.*;

public class DamageTracker extends JavaPlugin {
    private Map<String, BossConfig> bossConfigs;
    private BossConfig defaultBossConfig;
    private DamageManager damageManager;
    private TrackedBossManager trackedBossManager;
    private VictoryMessageManager victoryMessageManager;
    private DatabaseManager databaseManager;
    private RewardManager rewardManager;
    private boolean useVault;
    private Chat vaultChat;
    public String personalMessageFormat;
    public String damageFormat;
    public String percentageFormat;
    private HologramManager hologramManager;
    
    @Override
    public void onEnable() {
        // Load configuration first
        saveDefaultConfig();
        reloadConfig();
        
        // Initialize database manager
        databaseManager = new DatabaseManager(this);
        // Initialize boss configurations
        bossConfigs = new HashMap<>();
        // Initialize the damage manager
        initializeDamageManager();
        // Initialize the tracked boss manager
        initializeTrackedBossManager();
        // Initialize the victory message manager
        victoryMessageManager = new VictoryMessageManager(this);
        // Initialize the reward manager
        rewardManager = new RewardManager(this);
        // Load all configurations
        loadConfig();
        // Initialize message utilities
        MessageUtils.init(this);
        // Register event handlers and commands
        registerHandlers();
        // Setup integrations with other plugins
        setupIntegrations();
        // Display ASCII art in the console
        displayAsciiArt();
        
        // Initialize hologram manager
        hologramManager = new HologramManager(this);
    }

    @Override
    public void onDisable() {
        // Close message utilities
        MessageUtils.close();
        // Close database connection
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        // Remove all holograms
        if (hologramManager != null) {
            hologramManager.removeAllHolograms();
        }
    }

    private void initializeDamageManager() {
        // Get damage and percentage formats from config
        String damageFormat = getConfig().getString("display.damage_format", "%.0f");
        String percentageFormat = getConfig().getString("display.percentage_format", "%.1f");
        // Initialize the damage manager with the formats
        this.damageManager = new DamageManager(damageFormat, percentageFormat);
    }

    private void initializeTrackedBossManager() {
        trackedBossManager = new TrackedBossManager(this);
    }

    private void registerHandlers() {
        // Register event listeners
        getServer().getPluginManager().registerEvents(new MythicMobListeners(this), this);
        // Register placeholder (unified expansion)
        new DamageTrackerPlaceholder(this, databaseManager).register();
    
        // Register command handler
        DamageTrackerCommand commandHandler = new DamageTrackerCommand(this, trackedBossManager);
        getCommand("etherealdrops").setExecutor(commandHandler);
        getCommand("etherealdrops").setTabCompleter(commandHandler);
    }

    private void setupIntegrations() {
        // Setup Vault integration only
        setupVault();
    }

    private void setupVault() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().info("Vault plugin not found. Prefix features will be disabled.");
            useVault = false;
            vaultChat = null;
            return;
        }

        try {
            org.bukkit.plugin.RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
            if (rsp != null) {
                vaultChat = rsp.getProvider();
                useVault = true;
                getLogger().info("Vault integration enabled successfully.");
            } else {
                getLogger().warning("Vault found but no chat provider available. Prefix features will be disabled.");
                useVault = false;
                vaultChat = null;
            }
        } catch (Exception e) {
            getLogger().warning("Error setting up Vault integration. Prefix features will be disabled.");
            useVault = false;
            vaultChat = null;
        }
    }

    public void loadConfig() {
        // Save default config if not present
        saveDefaultConfig();
        // Reload config from file
        reloadConfig();
        // Clear existing boss configurations
        bossConfigs.clear();
        // Load formats from config
        loadFormats();
        // Load boss configurations from config
        loadBossConfigs();
        // Load default boss configuration from config
        loadDefaultConfig();
        // Load personal message format from config
        loadPersonalMessageFormat();
        // Load tracked boss manager configuration
        trackedBossManager.loadConfig();
        // Load victory message manager configuration
        victoryMessageManager.reloadConfig();
        // Load reward configurations
        rewardManager.loadRewards();
    }

    private void loadFormats() {
        // Get damage and percentage formats from config
        damageFormat = getConfig().getString("damage_format", "%.2f");
        percentageFormat = getConfig().getString("percentage_format", "%.1f%%");
        // Get personal message format from config
        personalMessageFormat = getConfig().getString("personal_message_format",
                "&6Your contribution: &ePosition: {position}, Damage: {damage} ({percentage}%)");
    }

    private void loadBossConfigs() {
        // Clear existing boss configurations
        bossConfigs.clear();

        // Load from tracked_bosses.yml via TrackedBossManager
        FileConfiguration trackedConfig = trackedBossManager.getConfig();
        var bossesSection = trackedConfig.getConfigurationSection("bosses");
        
        if (bossesSection != null) {
            // Iterate through each boss configuration
            for (String bossName : bossesSection.getKeys(false)) {
                var bossSection = bossesSection.getConfigurationSection(bossName);
                if (bossSection != null && bossSection.getBoolean("enabled", false)) {
                    var messagesSection = bossSection.getConfigurationSection("messages");
                    
                    // Create new boss configuration with message IDs
                    BossConfig config = new BossConfig(
                            messagesSection != null ? messagesSection.getString("victory", "default") : "default",
                            messagesSection != null ? messagesSection.getString("position_format", "default") : "default",
                            messagesSection != null ? messagesSection.getString("personal", "default") : "default",
                            messagesSection != null ? messagesSection.getString("non_participant", "default") : "default",
                            bossSection.getInt("top_players_shown", 3),
                            bossSection.getBoolean("broadcast", true),
                            bossSection.getBoolean("hologram", false) ? "FANCY" : "NONE"
                    );

                    // Validate the configuration
                    config.validate();

                    // Add to boss configurations map
                    bossConfigs.put(bossName.toUpperCase(), config);
                }
            }
        }

        getLogger().info("Configuration loaded. Number of configured bosses: " + bossConfigs.size());
    }

    private void loadDefaultConfig() {
        // Load default from tracked_bosses.yml
        FileConfiguration trackedConfig = trackedBossManager.getConfig();
        var defaultSection = trackedConfig.getConfigurationSection("default");
        
        if (defaultSection != null) {
            var messagesSection = defaultSection.getConfigurationSection("messages");
            
            // Initialize default boss configuration with message IDs
            defaultBossConfig = new BossConfig(
                    messagesSection != null ? messagesSection.getString("victory", "default") : "default",
                    messagesSection != null ? messagesSection.getString("position_format", "default") : "default",
                    messagesSection != null ? messagesSection.getString("personal", "default") : "default",
                    messagesSection != null ? messagesSection.getString("non_participant", "default") : "default",
                    defaultSection.getInt("top_players_shown", 3),
                    defaultSection.getBoolean("broadcast", true),
                    defaultSection.getBoolean("hologram", false) ? "FANCY" : "NONE"
            );
        } else {
            // Create a default configuration if none exists
            defaultBossConfig = new BossConfig();
        }

        // Validate the default configuration
        defaultBossConfig.validate();

        // Log default configuration details
        getLogger().info("Default configuration loaded:");
        getLogger().info("- Victory Message ID: " + defaultBossConfig.getVictoryMessageId());
        getLogger().info("- Position Format ID: " + defaultBossConfig.getPositionFormatId());
        getLogger().info("- Top Players to Show: " + defaultBossConfig.getTopPlayersToShow());
        getLogger().info("- Broadcast Messages: " + defaultBossConfig.isBroadcastMessage());
    }

    private void loadPersonalMessageFormat() {
        // Get personal message format from config
        personalMessageFormat = getConfig().getString("personal_message_format",
                "&6Your contribution: &ePosition: {position}, Damage: {damage} ({percentage}%)");
    }

    private void displayAsciiArt() {
        // Display ASCII art in the console
        String version = getDescription().getVersion();
        String enableMessage = String.format("v%s enabled! ~Fizzexual", version);

        String[] asciiArt = {
                "  _____ _   _                      _ ____                       ",
                " | ____| |_| |__   ___ _ __ ___  __ _| |  _ \\ _ __ ___  _ __  ___ ",
                " |  _| | __| '_ \\ / _ \\ '__/ _ \\/ _` | | | | | '__/ _ \\| '_ \\/ __|",
                " | |___| |_| | | |  __/ | |  __/ (_| | | |_| | | | (_) | |_) \\__ \\",
                " |_____|\\__|_| |_|\\___|_|  \\___|\\__,_|_|____/|_|  \\___/| .__/|___/",
                "                                                        |_|        "
        };

        Arrays.stream(asciiArt).forEach(line -> {
            if (line.contains("| |___")) {
                Bukkit.getConsoleSender().sendMessage("§b" + line + "  §9" + enableMessage);
            } else {
                Bukkit.getConsoleSender().sendMessage("§b" + line);
            }
        });
    }

    // Getters

    public DamageManager getDamageManager() {
        return damageManager;
    }

    public TrackedBossManager getTrackedBossManager() {
        return trackedBossManager;
    }

    public VictoryMessageManager getVictoryMessageManager() {
        return victoryMessageManager;
    }

    public Map<String, BossConfig> getBossConfigs() {
        return bossConfigs;
    }

    public BossConfig getDefaultBossConfig() {
        return defaultBossConfig;
    }

    public int getDefaultTopPlayersToShow() {
        return defaultBossConfig != null ? defaultBossConfig.getTopPlayersToShow() : 3;
    }
 
    public int getTopPlayersToShow(String bossName) {
        if (bossName == null) {
            return getDefaultTopPlayersToShow();
        }
        
        BossConfig config = bossConfigs.get(bossName.toUpperCase());
        if (config != null) {
            return config.getTopPlayersToShow();
        } else {
            return getDefaultTopPlayersToShow();
        }
    }


    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public String getPlayerPrefix(Player player) {
        String prefix = "";
        try {
            if (useVault && vaultChat != null) {
                prefix = vaultChat.getPlayerPrefix(player);
            }
        } catch (Exception e) {
            getLogger().warning("Error getting player prefix: " + e.getMessage());
        }
        return prefix != null ? prefix : "";
    }

    public void addDamage(UUID bossId, Player player, double damage) {
        damageManager.addDamage(bossId, player, damage);
    }

    public Map<UUID, Double> getBossDamageMap(UUID bossId) {
        return damageManager.getBossDamageMap(bossId);
    }

    public Map<UUID, Map<UUID, Double>> getAllDamageData() {
        return damageManager.getAllDamageData();
    }

    public double getBossMaxHealth(UUID bossId) {
        return damageManager.getBossMaxHealth(bossId);
    }

    public boolean hasBossMaxHealth(UUID bossId) {
        return damageManager.hasBossMaxHealth(bossId);
    }

    public void setBossMaxHealth(UUID bossId, double health) {
        damageManager.setBossMaxHealth(bossId, health);
    }

    public void removeBossData(UUID bossId) {
        damageManager.removeBossData(bossId);
    }

    public String formatDamage(double damage, double maxHealth, String displayType) {
        return damageManager.formatDamage(damage, maxHealth, displayType);
    }

    public Map<UUID, Double> calculateTotalDamage() {
        return damageManager.calculateTotalDamage();
    }

    public List<Map.Entry<UUID, Double>> getTopDamage(Map<UUID, Double> damageMap, int limit) {
        return damageManager.getTopDamage(damageMap, limit);
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }
}
