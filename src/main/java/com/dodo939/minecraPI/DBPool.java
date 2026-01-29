package com.dodo939.minecraPI;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

import static com.dodo939.minecraPI.MinecraPI.config;

public class DBPool {
    private static HikariDataSource dataSource;

    private DBPool() {}

    public static void init() {
        HikariConfig _config = new HikariConfig();

        _config.setJdbcUrl(config.mysql.url);
        _config.setUsername(config.mysql.username);
        _config.setPassword(config.mysql.password);
        _config.setDriverClassName(config.mysql.driver);

        _config.setMaximumPoolSize(20);
        _config.setMinimumIdle(5);
        _config.setIdleTimeout(300000);
        _config.setConnectionTimeout(20000);

        dataSource = new HikariDataSource(_config);
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
