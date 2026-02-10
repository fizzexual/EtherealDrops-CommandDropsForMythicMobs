package com.fizzexual.damagetracker.commands;

import com.fizzexual.damagetracker.DamageTracker;
import com.fizzexual.damagetracker.utils.MessageUtils;
import com.fizzexual.damagetracker.managers.TrackedBossManager;
import com.fizzexual.damagetracker.configs.RewardConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Command executor and tab completer for the DamageTracker plugin.
 */
public class DamageTrackerCommand implements CommandExecutor, TabCompleter {
    private final DamageTracker plugin;
    private final String damageFormat;
    private final TrackedBossCommands trackedBossCommands;

    /**
     * Constructor for DamageTrackerCommand.
     *
     * @param plugin The main plugin instance.
     * @param trackedBossManager The manager for tracked bosses.
     */
    public DamageTrackerCommand(DamageTracker plugin, TrackedBossManager trackedBossManager) {
        this.plugin = plugin;
        this.damageFormat = plugin.getConfig().getString("damage_format", "%.2f");
        this.trackedBossCommands = new TrackedBossCommands(plugin, trackedBossManager);
    }

    /**
     * Handles the execution of the /damagetracker command.
     *
     * @param sender The sender of the command.
     * @param command The command that was executed.
     * @param label The alias of the command that was used.
     * @param args The arguments passed to the command.
     * @return true if the command was handled successfully, false otherwise.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("etherealdrops")) {
            return false;
        }

        if (args.length == 0) {
            return showHelp(sender);
        }

        return switch (args[0].toLowerCase()) {
            case "help" -> showHelp(sender);
            case "reload" -> handleReloadCommand(sender);
            case "check" -> trackedBossCommands.handleCheckDamageCommand(sender, args);
            case "top" -> trackedBossCommands.handleCheckTopCommand(sender, args);
            case "clear" -> trackedBossCommands.handleClearDataCommand(sender, args);
            default -> showHelp(sender);
        };
    }

    /**
     * Shows the help message for the /etherealdrops command.
     *
     * @param sender The sender of the command.
     * @return true always.
     */
    private boolean showHelp(CommandSender sender) {
        MessageUtils.sendMessage(sender, "");
        MessageUtils.sendMessage(sender, "<gradient:aqua:blue><bold>═══════════════════════════</bold></gradient>");
        MessageUtils.sendMessage(sender, "<gradient:aqua:blue><bold>    EtherealDrops Commands</bold></gradient>");
        MessageUtils.sendMessage(sender, "<gradient:aqua:blue><bold>═══════════════════════════</bold></gradient>");
        MessageUtils.sendMessage(sender, "");
        MessageUtils.sendMessage(sender, "<aqua>/ed help</aqua> <gray>- Show this help menu</gray>");
        
        if (sender.hasPermission("etherealdrops.reload")) {
            MessageUtils.sendMessage(sender, "<aqua>/ed reload</aqua> <gray>- Reload all configurations</gray>");
        }
        
        if (sender.hasPermission("etherealdrops.check")) {
            MessageUtils.sendMessage(sender, "<aqua>/ed check <boss></aqua> <gray>- Check your damage to a boss</gray>");
        }
        
        if (sender.hasPermission("etherealdrops.checktop")) {
            MessageUtils.sendMessage(sender, "<aqua>/ed top <boss></aqua> <gray>- View boss damage leaderboard</gray>");
        }
        
        if (sender.hasPermission("etherealdrops.cleardata")) {
            MessageUtils.sendMessage(sender, "<aqua>/ed clear <boss></aqua> <gray>- Clear boss damage data</gray>");
        }
        
        MessageUtils.sendMessage(sender, "");
        MessageUtils.sendMessage(sender, "<gray>Aliases: <white>/etherealdrops</white>, <white>/ed</white>, <white>/drops</white></gray>");
        MessageUtils.sendMessage(sender, "<gradient:aqua:blue><bold>═══════════════════════════</bold></gradient>");
        MessageUtils.sendMessage(sender, "");
        return true;
    }

    /**
     * Handles the /etherealdrops reload command.
     *
     * @param sender The sender of the command.
     * @return true always.
     */
    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("etherealdrops.reload")) {
            MessageUtils.sendMessage(sender, "<red>You don't have permission to use this command.</red>");
            return true;
        }

        try {
            // Reload main plugin configuration
            plugin.loadConfig();

            MessageUtils.sendMessage(sender, "");
            MessageUtils.sendMessage(sender, "<green><bold>✓ Configuration Reloaded!</bold></green>");
            MessageUtils.sendMessage(sender, "");
            MessageUtils.sendMessage(sender, "<gray>Reloaded:</gray>");
            MessageUtils.sendMessage(sender, "<white>  • Main configuration</white>");
            MessageUtils.sendMessage(sender, "<white>  • Tracked bosses</white>");
            MessageUtils.sendMessage(sender, "<white>  • Messages & formats</white>");
            MessageUtils.sendMessage(sender, "<white>  • Rewards</white>");
            MessageUtils.sendMessage(sender, "<white>  • Holograms</white>");
            MessageUtils.sendMessage(sender, "");
        } catch (Exception e) {
            MessageUtils.sendMessage(sender, "<red>Error reloading configuration: " + e.getMessage() + "</red>");
            plugin.getLogger().severe("Error during config reload: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Handles tab completion for the /etherealdrops command.
     *
     * @param sender The sender of the command.
     * @param command The command that was executed.
     * @param alias The alias of the command that was used.
     * @param args The arguments passed to the command.
     * @return A list of possible completions for the last argument.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("etherealdrops")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("help");
            if (sender.hasPermission("etherealdrops.reload")) {
                completions.add("reload");
            }
            if (sender.hasPermission("etherealdrops.check")) {
                completions.add("check");
            }
            if (sender.hasPermission("etherealdrops.checktop")) {
                completions.add("top");
            }
            if (sender.hasPermission("etherealdrops.cleardata")) {
                completions.add("clear");
            }
            return completions.stream()
                    .filter(c -> c.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("check") ||
                args[0].equalsIgnoreCase("top") ||
                args[0].equalsIgnoreCase("clear"))) {
            return trackedBossCommands.onTabComplete(args);
        }

        return new ArrayList<>();
    }
}
