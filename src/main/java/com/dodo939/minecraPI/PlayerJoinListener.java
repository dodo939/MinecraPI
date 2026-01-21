package com.dodo939.minecraPI;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.net.InetAddress;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.UUID;

import static com.dodo939.minecraPI.MinecraPI.conn;
import static com.dodo939.minecraPI.MinecraPI.config;

@SuppressWarnings("deprecation")
public class PlayerJoinListener implements Listener {
    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!config.enable_player_auth) return;

        UUID uuid = event.getUniqueId();
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM players WHERE uuid = ? LIMIT 1")) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                event.allow();
                return;
            }

            // gen code
            VerificationUtils.cleanCodes();

            String code = VerificationUtils.setVerificationCode(uuid);
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    MessageFormat.format(String.join("\n", config.notice_message), code)
            );
        } catch (Exception e) {
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    MessageFormat.format(String.join("\n", config.error_message), e.toString())
            );
        }
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        if (config.max_player_per_ip <= 0 || player.hasPermission("minecrapi.minecrapi.skip_ip_check"))
            return;

        InetAddress addr = event.getAddress();
        int count = 1;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (Objects.requireNonNull(p.getAddress()).getAddress().equals(addr))
                count++;
            if (count > config.max_player_per_ip) {
                event.disallow(
                        PlayerLoginEvent.Result.KICK_OTHER,
                        String.join("\n", config.ip_limit_message)
                );
                break;
            }
        }
    }
}
