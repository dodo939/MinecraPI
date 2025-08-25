package com.dodo939.minecraPI;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class CommandHandler implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission("minecrapi.admin")) {
            sender.sendMessage("§cYou do not have permission to do that.");
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0]) {
            case "help" -> sendHelpMessage(sender);
            case "reload" -> {
                MinecraPI.getPlugin(MinecraPI.class).reloadAll();
                sender.sendMessage("§aConfig reloaded.");
            }
            default -> sender.sendMessage("§cInvalid arguments. See /minecrapi help");
        }
        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("===== MinecraPI help =====");
        sender.sendMessage("/minecrapi help - Show this message");
        sender.sendMessage("/minecrapi reload - Reload config");
    }
}
