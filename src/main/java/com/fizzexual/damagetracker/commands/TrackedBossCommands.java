package com.fizzexual.damagetracker.commands;

import com.fizzexual.damagetracker.DamageTracker;
import com.fizzexual.damagetracker.managers.TrackedBossManager;
import com.fizzexual.damagetracker.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

/**
 * Handles commands related to tracked bosses.
 */
public class TrackedBossCommands {
    private final DamageTracker plugin;
    private final TrackedBossManager trackedBossManager;

    /**
     * Constructor for TrackedBossCommands.
     * @param plugin The instance of the DamageTracker plugin.
     * @param trackedBossManager The manager for tracked bosses.
     */
    public TrackedBossCommands(DamageTracker plugin, TrackedBossManager trackedBossManager) {
        this.plugin = plugin;
        this.trackedBossManager = trackedBossManager;
    }

    /**
     * Handles the command to check damage dealt to a boss.
     * @param sender The command sender.
     * @param args The command arguments.
     * @return True if the command was handled, false otherwise.
     */
    public boolean handleCheckDamageCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtils.sendMessage(sender, "<red>This command can only be used by players.</red>");
            return true;
        }

        if (!sender.hasPermission("etherealdrops.check")) {
            MessageUtils.sendMessage(sender, "<red>You do not have permission to use this command.</red>");
            return true;
        }

        if (args.length < 2) {
            MessageUtils.sendMessage(sender, "<red>Usage: /ed check <boss></red>");
            return true;
        }

        String bossId = args[1].toUpperCase();
        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        if (!trackedBossManager.isTrackedBoss(bossId)) {
            MessageUtils.sendMessage(sender, "<red>Boss '<yellow>" + args[1] + "</yellow>' is not being tracked.</red>");
            return true;
        }

        double damage = trackedBossManager.getPlayerDamage(bossId, playerId);
        double percentage = trackedBossManager.getPlayerDamagePercentage(bossId, playerId);

        MessageUtils.sendMessage(sender, "");
        MessageUtils.sendMessage(sender, "<gold>Your damage to <yellow>" + args[1] + "</yellow>:</gold>");
        MessageUtils.sendMessage(sender, "<white>  Damage: <aqua>" + trackedBossManager.formatDamage(damage, bossId) + "</aqua></white>");
        MessageUtils.sendMessage(sender, "<white>  Percentage: <aqua>" + String.format("%.1f", percentage) + "%</aqua></white>");
        MessageUtils.sendMessage(sender, "");
        return true;
    }

    /**
     * Handles the command to check the top damage dealt to a boss.
     * @param sender The command sender.
     * @param args The command arguments.
     * @return True if the command was handled, false otherwise.
     */
    public boolean handleCheckTopCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("etherealdrops.checktop")) {
            MessageUtils.sendMessage(sender, "<red>You do not have permission to use this command.</red>");
            return true;
        }

        if (args.length < 2) {
            MessageUtils.sendMessage(sender, "<red>Usage: /ed top <boss></red>");
            return true;
        }

        String bossId = args[1].toUpperCase();
        if (!trackedBossManager.isTrackedBoss(bossId)) {
            MessageUtils.sendMessage(sender, "<red>Boss '<yellow>" + args[1] + "</yellow>' is not being tracked.</red>");
            return true;
        }

        List<Map.Entry<UUID, Double>> topDamage = trackedBossManager.getTopDamage(bossId, 10);
        if (topDamage.isEmpty()) {
            MessageUtils.sendMessage(sender, "<red>No damage data available for this boss.</red>");
            return true;
        }

        MessageUtils.sendMessage(sender, "");
        MessageUtils.sendMessage(sender, "<gold><bold>═══ Top Damage: <yellow>" + args[1] + "</yellow> ═══</bold></gold>");
        MessageUtils.sendMessage(sender, "");

        for (int i = 0; i < topDamage.size(); i++) {
            Map.Entry<UUID, Double> entry = topDamage.get(i);
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                String position = String.valueOf(i + 1);
                String emoji = i == 0 ? "🥇" : i == 1 ? "🥈" : i == 2 ? "🥉" : "  ";
                MessageUtils.sendMessage(sender, "<yellow>" + emoji + " #" + position + "</yellow> <white>" + player.getName() + 
                    "</white> - <aqua>" + trackedBossManager.formatDamage(entry.getValue(), bossId) + 
                    "</aqua> <gray>(" + String.format("%.1f", trackedBossManager.getPlayerDamagePercentage(bossId, entry.getKey())) + "%)</gray>");
            }
        }

        MessageUtils.sendMessage(sender, "");
        return true;
    }

    /**
     * Handles the command to clear damage data for a boss.
     * @param sender The command sender.
     * @param args The command arguments.
     * @return True if the command was handled, false otherwise.
     */
    public boolean handleClearDataCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("etherealdrops.cleardata")) {
            MessageUtils.sendMessage(sender, "<red>You do not have permission to use this command.</red>");
            return true;
        }

        if (args.length < 2) {
            MessageUtils.sendMessage(sender, "<red>Usage: /ed clear <boss></red>");
            return true;
        }

        String bossId = args[1].toUpperCase();
        if (!trackedBossManager.isTrackedBoss(bossId)) {
            MessageUtils.sendMessage(sender, "<red>Boss '<yellow>" + args[1] + "</yellow>' is not being tracked.</red>");
            return true;
        }

        if (trackedBossManager.getDamageData(bossId).isEmpty()) {
            MessageUtils.sendMessage(sender, "<red>There is no data to clear for this boss.</red>");
            return true;
        }

        trackedBossManager.clearBossData(bossId);
        MessageUtils.sendMessage(sender, "<green>✓ Damage data for <yellow>" + args[1] + "</yellow> has been cleared.</green>");
        return true;
    }

    /**
     * Provides tab completion suggestions for commands.
     * @param args The command arguments.
     * @return A list of tab completion suggestions.
     */
    public List<String> onTabComplete(String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            // Suggestions for subcommands check, top, and clear
            String lowercaseArg = args[1].toLowerCase();
            trackedBossManager.getTrackedBossIds().stream()
                    .filter(bossId -> bossId.toLowerCase().startsWith(lowercaseArg))
                    .forEach(completions::add);
        }

        return completions;
    }
}
