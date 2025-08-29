package com.dodo939.minecraPI;

import io.javalin.Javalin;
import io.javalin.http.UnauthorizedResponse;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.Objects;
import java.util.logging.Logger;

public final class MinecraPI extends JavaPlugin {
    public static Javalin app;
    public static Logger logger;
    public static JavaPlugin plugin;
    public static Connection conn;
    public boolean papi_existed = false;
    public static boolean player_auth_enabled = false;
    private static long TIMESTAMP_TOLERANCE;

    @Override
    public void onEnable() {
        // Plugin startup logic
        logger = getLogger();
        plugin = this;

        // Check PlaceholderAPI
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            papi_existed = true;
            logger.info("PlaceholderAPI detected.");
        } else {
            logger.info("PlaceholderAPI not detected.");
        }

        try {
            reloadAll();
        } catch (SQLException e) {
            logger.severe("Failed to close existed sqlite connection.");
        }

        // Register commands
        Objects.requireNonNull(getCommand("minecrapi")).setExecutor(new CommandHandler());

        // Register listeners
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (app != null) {
            logger.info("Stopping Javalin server...");
            app.stop();
        }
        if (conn != null) {
            try {
                logger.info("Closing SQLite connection...");
                conn.close();
            } catch (SQLException e) {
                logger.severe("Failed to close existed sqlite connection.");
            }
        }
    }

    public void reloadAll() throws SQLException {
        if (app != null) {
            logger.info("Stopping Javalin server...");
            app.stop();
        }
        if (conn != null) {
            logger.info("Closing SQLite connection...");
            conn.close();
        }
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:" + (new File(plugin.getDataFolder(), "datas.db")).getAbsolutePath());
        } catch (SQLException e) {
            logger.severe("Failed to create sqlite connection. Disabling plugin!");
            logger.severe(e.toString());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        reloadConfig();

        // create table
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS players (id INTEGER PRIMARY KEY AUTOINCREMENT, uuid TEXT, spid TEXT)");
        }

        // Load config
        String host = Objects.requireNonNull(getConfig().getString("host"));
        int port = getConfig().getInt("port");
        String SECRET_KEY = getConfig().getString("secret-key");
        TIMESTAMP_TOLERANCE = getConfig().getInt("timestamp-tolerance");
        ConfigurationSection services = getConfig().getConfigurationSection("services");
        player_auth_enabled = getConfig().getBoolean("enable-player-auth");

        app = Javalin.create().start(host, port);

        // Authentication
        if (SECRET_KEY != null && !SECRET_KEY.isEmpty()) {
            app.before(ctx -> {
                String clientSignature = ctx.header("X-Signature");
                String clientTimestamp = ctx.header("X-Timestamp");
                String requestBody = ctx.body();

                if (clientSignature == null || clientTimestamp == null || clientTimestamp.isEmpty()) {
                    throw new UnauthorizedResponse("Missing authentication headers");
                }

                long serverTimestamp = System.currentTimeMillis();
                if (Math.abs(serverTimestamp - Long.parseLong(clientTimestamp)) > TIMESTAMP_TOLERANCE) {
                    throw new UnauthorizedResponse("Signature has expired");
                }

                String signatureBase = String.join("\n",
                    clientTimestamp,
                    ctx.method().name(),
                    ctx.path(),
                    requestBody
                );

                String serverSignature = generateHmacSHA256AsBase64(signatureBase, SECRET_KEY);
                if (!serverSignature.equals(clientSignature)) {
                    throw new UnauthorizedResponse("Signature inconsistency");
                }
            });
        } else {
            logger.warning("No secret key provided. Your server is unsafe now!");
        }

        // Register services
        if (services != null) {
            for (String key : services.getKeys(false)) {
                ConfigurationSection svc = services.getConfigurationSection(key);
                if (svc == null) continue;

                String path = Objects.requireNonNull(svc.getString("path"));
                String type = Objects.requireNonNull(svc.getString("type"));

                logger.info("Registering " + path);
                switch (type) {
                    case "ping" -> RegisterUtils.registerPing(path);
                    case "command" -> RegisterUtils.registerCommand(path);
                    case "papi" -> {
                        if (papi_existed) RegisterUtils.registerPlaceholderAPI(path);
                        else logger.warning("PlaceholderAPI not detected.");
                    }
                    case "list_players" -> RegisterUtils.registerListPlayers(path);
                    case "bind" -> RegisterUtils.registerBind(path);
                    case "unbind" -> RegisterUtils.registerUnbind(path);
                    default -> logger.warning("Unknown type: " + type);
                }
            }
        }
    }

    private static String generateHmacSHA256AsBase64(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] signatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate HMAC", e);
        }
    }
}
