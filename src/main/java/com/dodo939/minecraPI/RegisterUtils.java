package com.dodo939.minecraPI;

import org.bukkit.Bukkit;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

import java.util.StringJoiner;

import static com.dodo939.minecraPI.MinecraPI.app;
import static com.dodo939.minecraPI.MinecraPI.plugin;

public class RegisterUtils {

    public static void registerPing(String path) {
        app.get(path, ctx -> ctx.result("pong"));
    }

    public static void registerCommand(String path) {
        app.post(path, ctx -> {
            try {
                String cmd = ctx.body();

                if (cmd.trim().isEmpty()) {
                    ctx.status(400).result("Missing command.");
                    return;
                }

                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));

                ctx.result("Executed command: " + cmd);
            } catch (Exception e) {
                ctx.status(400).result("Something went wrong: " + e.getMessage());
            }
        });
    }

    public static void registerPlaceholderAPI(String path) {
        app.post(path, ctx -> {
            String[] papi = ctx.body().split(" ");
            if (papi.length != 2) {
                ctx.status(400).result("Invalid request body: " + ctx.body());
                return;
            }

            String parsed_text = PlaceholderAPI.setPlaceholders(Bukkit.getOfflinePlayer(papi[1]), papi[0]);
            ctx.result(parsed_text);
        });
    }

    public static void registerListPlayers(String path) {
        app.get(path, ctx -> {
            StringJoiner joiner = new StringJoiner(", ");
            joiner.setEmptyValue("");
            for (Player player : Bukkit.getOnlinePlayers()) {
                joiner.add(player.getName());
            }
            ctx.result(joiner.toString());
        });
    }
}
