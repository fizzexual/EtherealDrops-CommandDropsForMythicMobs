package com.fizzexual.damagetracker.managers;

import com.fizzexual.damagetracker.DamageTracker;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages victory messages for the DamageTracker plugin.
 */
public class VictoryMessageManager {
    private final DamageTracker plugin;
    private FileConfiguration messageConfig;
    private File messageFile;
    private final Map<String, String> victoryMessages;
    private final Map<String, List<String>> positionFormats;
    private final Map<String, String> personalMessages;
    private final Map<String, String> nonParticipantMessages;

    /**
     * Constructor for VictoryMessageManager.
     *
     * @param plugin The main plugin instance.
     */
    public VictoryMessageManager(DamageTracker plugin) {
        this.plugin = plugin;
        this.victoryMessages = new HashMap<>();
        this.positionFormats = new HashMap<>();
        this.personalMessages = new HashMap<>();
        this.nonParticipantMessages = new HashMap<>();
        loadMessages();
    }

    /**
     * Loads the victory messages from the configuration file.
     */
    public void loadMessages() {
        if (messageFile == null) {
            messageFile = new File(plugin.getDataFolder(), "messages.yml");
        }

        if (!messageFile.exists()) {
            plugin.saveResource("messages.yml", false);
            plugin.getLogger().info("Created new messages.yml file");
        }

        messageConfig = YamlConfiguration.loadConfiguration(messageFile);
        loadVictoryMessages();
        loadPositionFormats();
        loadPersonalMessages();
        loadNonParticipantMessages();
    }

    /**
     * Loads the victory messages from the configuration section.
     */
    private void loadVictoryMessages() {
        victoryMessages.clear();
        var messagesSection = messageConfig.getConfigurationSection("victory");
        if (messagesSection != null) {
            for (String key : messagesSection.getKeys(false)) {
                List<String> lines = messagesSection.getStringList(key);
                victoryMessages.put(key.toUpperCase(), String.join("\n", lines));
            }
        }
    }

    /**
     * Loads the position formats from the configuration section.
     */
    private void loadPositionFormats() {
        positionFormats.clear();
        var formatsSection = messageConfig.getConfigurationSection("position_format");
        if (formatsSection != null) {
            for (String key : formatsSection.getKeys(false)) {
                positionFormats.put(key.toUpperCase(), formatsSection.getStringList(key));
            }
        }
    }

    /**
     * Loads the personal messages from the configuration section.
     */
    private void loadPersonalMessages() {
        personalMessages.clear();
        var personalSection = messageConfig.getConfigurationSection("personal");
        if (personalSection != null) {
            for (String key : personalSection.getKeys(false)) {
                personalMessages.put(key.toUpperCase(), personalSection.getString(key));
            }
        }
    }

    /**
     * Loads the non-participant messages from the configuration section.
     */
    private void loadNonParticipantMessages() {
        nonParticipantMessages.clear();
        var nonParticipantSection = messageConfig.getConfigurationSection("non_participant");
        if (nonParticipantSection != null) {
            for (String key : nonParticipantSection.getKeys(false)) {
                nonParticipantMessages.put(key.toUpperCase(), nonParticipantSection.getString(key));
            }
        }
    }

    /**
     * Gets the victory message for a given message ID.
     *
     * @param messageId The ID of the message.
     * @return The victory message.
     */
    public String getVictoryMessage(String messageId) {
        return victoryMessages.getOrDefault(messageId.toUpperCase(), victoryMessages.get("DEFAULT"));
    }

    /**
     * Gets the position format for a given format ID.
     *
     * @param formatId The ID of the format.
     * @return The list of position formats.
     */
    public List<String> getPositionFormat(String formatId) {
        return positionFormats.getOrDefault(formatId.toUpperCase(), positionFormats.get("DEFAULT"));
    }

    /**
     * Gets the personal message for a given message ID.
     *
     * @param messageId The ID of the message.
     * @return The personal message.
     */
    public String getPersonalMessage(String messageId) {
        return personalMessages.getOrDefault(messageId.toUpperCase(), personalMessages.get("DEFAULT"));
    }

    /**
     * Gets the non-participant message for a given message ID.
     *
     * @param messageId The ID of the message.
     * @return The non-participant message.
     */
    public String getNonParticipantMessage(String messageId) {
        return nonParticipantMessages.getOrDefault(messageId.toUpperCase(), nonParticipantMessages.get("DEFAULT"));
    }

    /**
     * Saves the configuration to the file.
     */
    public void saveConfig() {
        try {
            messageConfig.save(messageFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save messages.yml", e);
        }
    }

    /**
     * Reloads the configuration from the file.
     */
    public void reloadConfig() {
        loadMessages();
    }
}
