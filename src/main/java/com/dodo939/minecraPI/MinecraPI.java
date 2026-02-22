package com.dodo939.minecraPI;

import io.javalin.Javalin;
import io.javalin.http.UnauthorizedResponse;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Base64;
import java.util.Objects;
import java.util.logging.Logger;

public final class MinecraPI extends JavaPlugin {
    public static Javalin app;
    public static Logger logger;
    public static JavaPlugin plugin;
    public boolean papi_existed = false;
    public static MyConfig config = new MyConfig();

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
        } catch (Exception e) {
            logger.severe("Something went wrong: " + e);
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

        logger.info("Closing MySQL connection...");
        DBPool.close();
    }

    public void reloadAll() throws Exception {
        if (app != null) {
            logger.info("Stopping Javalin server...");
            app.stop();
        }

        DBPool.close();

        if (!plugin.getDataFolder().exists()) {
            // noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
        }

        saveDefaultConfig();
        reloadConfig();

        if (Objects.equals(getConfig().getString("mysql.username"), "please_change_me")) {
            logger.warning("Please change the config in config.yml and then /mapi reload");
            return;
        }

        // Load config
        FileConfiguration _config = getConfig();
        config.host = Objects.requireNonNull(_config.getString("host"));
        config.port = _config.getInt("port");
        config.secret_key = _config.getString("secret_key");
        config.timestamp_tolerance = _config.getInt("timestamp_tolerance");
        config.enable_player_auth = _config.getBoolean("enable_player_auth");

        config.mysql.url = _config.getString("mysql.url");
        config.mysql.username = _config.getString("mysql.username");
        config.mysql.password = _config.getString("mysql.password");

        config.redis_url = _config.getString("redis_url");

        config.max_player_per_ip = _config.getInt("max_player_per_ip");
        config.ip_limit_message = _config.getStringList("ip_limit_message");
        config.notice_message = _config.getStringList("notice_message");
        config.error_message = _config.getStringList("error_message");

        RedisManager.init();
        DBPool.init();

        try (Connection conn = DBPool.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS minecrapi_players (id INT PRIMARY KEY AUTO_INCREMENT, uuid VARCHAR(36), spid VARCHAR(20))");
        }

        app = Javalin.create().start(config.host, config.port);

        // Authentication
        if (config.secret_key != null && !config.secret_key.isEmpty()) {
            app.before(ctx -> {
                String clientSignature = ctx.header("X-Signature");
                String clientTimestamp = ctx.header("X-Timestamp");
                String requestBody = ctx.body();

                if (clientSignature == null || clientTimestamp == null || clientTimestamp.isEmpty()) {
                    throw new UnauthorizedResponse("Missing authentication headers");
                }

                long serverTimestamp = System.currentTimeMillis();
                if (Math.abs(serverTimestamp - Long.parseLong(clientTimestamp)) > config.timestamp_tolerance) {
                    throw new UnauthorizedResponse("Signature has expired");
                }

                String signatureBase = String.join("\n",
                    clientTimestamp,
                    ctx.method().name(),
                    ctx.path(),
                    requestBody
                );

                String serverSignature = generateHmacSHA256AsBase64(signatureBase, config.secret_key);
                if (!serverSignature.equals(clientSignature)) {
                    throw new UnauthorizedResponse("Signature inconsistency");
                }
            });
        } else {
            logger.warning("No secret key provided. Your server is unsafe now!");
        }

        // Register services
        logger.info("Registering services...");
        RegisterUtils.registerPing("/ping");
        RegisterUtils.registerCommand("/command");
        if (papi_existed) {
            RegisterUtils.registerPlaceholderAPI("/papi");
        } else {
            logger.warning("PlaceholderAPI not detected.");
        }
        RegisterUtils.registerListPlayers("/list");
        RegisterUtils.registerBind("/bind");
        RegisterUtils.registerUnbind("/unbind");
        RegisterUtils.registerClear("/clear");
        RegisterUtils.registerQuery("/query");
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
