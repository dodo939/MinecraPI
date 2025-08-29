package com.dodo939.minecraPI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static com.dodo939.minecraPI.MinecraPI.conn;

public class PlayerJoinListener implements Listener {
    @EventHandler
    public void onPlayerJoin(AsyncPlayerPreLoginEvent event) {
        if (!MinecraPI.player_auth_enabled) return;

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
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("请加入官方QQ群聊1030335845，发送以下内容：\n", NamedTextColor.YELLOW)
                    .append(Component.text("\n绑定" + code + "\n", NamedTextColor.AQUA, TextDecoration.UNDERLINED))
                    .append(Component.text("\n此验证码将于1分钟后失效，在此之前，切勿将验证码泄露于他人！", NamedTextColor.RED))
            );
        } catch (Exception e) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("发生内部错误，这不是你的问题，请截图并发送给管理员\n" + e, NamedTextColor.RED));
        }
    }
}
