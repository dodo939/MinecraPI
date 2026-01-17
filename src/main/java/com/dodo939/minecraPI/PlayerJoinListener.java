package com.dodo939.minecraPI;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.MessageFormat;
import java.util.UUID;

import static com.dodo939.minecraPI.MinecraPI.conn;
import static com.dodo939.minecraPI.MinecraPI.config;

public class PlayerJoinListener implements Listener {
    @EventHandler
    public void onPlayerJoin(AsyncPlayerPreLoginEvent event) {
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
}
