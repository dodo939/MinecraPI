package com.dodo939.minecraPI;

import io.javalin.http.UnauthorizedResponse;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import io.javalin.Javalin;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;
import java.util.logging.Logger;

public final class MinecraPI extends JavaPlugin {
    public static Javalin app;
    public static Logger logger;
    public static JavaPlugin plugin;
    public boolean papi_existed = false;
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

        reloadAll();

        // Register commands
        Objects.requireNonNull(getCommand("minecrapi")).setExecutor(new CommandHandler());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (app != null) {
            logger.info("Stopping Javalin server...");
            app.stop();
        }
    }

    public void reloadAll() {
        if (app != null) {
            logger.info("Stopping Javalin server...");
            app.stop();
        }

        saveDefaultConfig();
        reloadConfig();

        // Load config
        String host = Objects.requireNonNull(getConfig().getString("host"));
        int port = getConfig().getInt("port");
        String SECRET_KEY = getConfig().getString("secret-key");
        TIMESTAMP_TOLERANCE = getConfig().getInt("timestamp-tolerance");
        ConfigurationSection services = getConfig().getConfigurationSection("services");

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
