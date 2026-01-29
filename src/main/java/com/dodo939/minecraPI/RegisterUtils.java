package com.dodo939.minecraPI;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringJoiner;

import static com.dodo939.minecraPI.MinecraPI.*;

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

    public static void registerBind(String path) {
        app.post(path, ctx -> {
            String[] body = ctx.body().split(" ");
            if (body.length != 2) {
                ctx.status(400).result("Invalid request body: " + ctx.body());
                return;
            }
            String spid = body[0];
            String code = body[1];
            String uuid;

            try (Connection conn = DBPool.getConnection(); PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM minecrapi_players WHERE spid = ? LIMIT 1")) {
                pstmt.setString(1, spid);

                ResultSet st = pstmt.executeQuery();
                if (st.next()) {
                    ctx.status(409).result(st.getString("uuid"));
                    return;
                }

                uuid = VerificationUtils.getUUIDByCode(code);
                if (uuid == null) {
                    ctx.status(400).result();
                    return;
                }
            } catch (SQLException e) {
                ctx.status(500).result(e.toString());
                return;
            }

            // Remove verification code
            VerificationUtils.removeVerificationCode(code);

            try (Connection conn = DBPool.getConnection(); PreparedStatement pstmt = conn.prepareStatement("INSERT INTO minecrapi_players (uuid, spid) VALUES (?, ?)")) {
                pstmt.setString(1, uuid);
                pstmt.setString(2, spid);
                pstmt.execute();
                ctx.result(uuid);
            } catch (SQLException e) {
                ctx.status(500).result(e.toString());
            }
        });
    }

    public static void registerUnbind(String path) {
        app.post(path, ctx -> {
            String spid = ctx.body();

            try (Connection conn = DBPool.getConnection(); PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM minecrapi_players WHERE spid = ? LIMIT 1")) {
                pstmt.setString(1, spid);

                ResultSet rs = pstmt.executeQuery();
                if (!rs.next()) {
                    ctx.status(404).result("");
                    return;
                }

                PreparedStatement ps = conn.prepareStatement("DELETE FROM minecrapi_players WHERE spid = ?");
                ps.setString(1, spid);
                ps.execute();
                ctx.result(rs.getString("uuid"));
            } catch (SQLException e) {
                ctx.status(500).result(e.toString());
            }
        });
    }

    public static void registerClear(String path) {
        app.post(path, ctx -> {
            String[] spids = ctx.body().split(",");

            try (Connection conn = DBPool.getConnection(); PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM minecrapi_players")) {
                ResultSet rs = pstmt.executeQuery();
                int count = 0;
                while (rs.next()) {
                    boolean found = false;
                    for (String spid : spids) {
                        if (spid.equals(rs.getString("spid"))) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        count++;
                        PreparedStatement ps = conn.prepareStatement("DELETE FROM minecrapi_players WHERE spid = ?");
                        ps.setString(1, rs.getString("spid"));
                        ps.execute();
                    }
                }
                ctx.result(count + "");
            } catch (SQLException e) {
                ctx.status(500).result(e.toString());
            }
        });
    }

    public static void registerQuery(String path) {
        app.get(path + "{spid}", ctx -> {
            String spid = ctx.pathParam("spid");

            try (Connection conn = DBPool.getConnection(); PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM minecrapi_players WHERE spid = ? LIMIT 1")) {
                pstmt.setString(1, spid);

                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    ctx.result(rs.getString("uuid"));
                } else {
                    ctx.status(404).result("");
                }
            } catch (SQLException e) {
                ctx.status(500).result(e.toString());
            }
        });
    }
}
