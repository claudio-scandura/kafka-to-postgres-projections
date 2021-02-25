package com.mylaesoftware;

import akka.japi.function.Function;
import akka.projection.jdbc.JdbcSession;
import com.mylaesoftware.config.DbConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.SQLException;

public class HikariSessionProvider {

    private final HikariDataSource dataSource;

    public HikariSessionProvider(DbConfig config) {

        initSchema(config);

        var hikariConfig = new HikariConfig();
        hikariConfig.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
        hikariConfig.addDataSourceProperty("serverName", config.dbServerName());
        hikariConfig.addDataSourceProperty("portNumber", config.dbPort());
        hikariConfig.addDataSourceProperty("databaseName", config.dbName());
        hikariConfig.addDataSourceProperty("user", config.dbUsername());
        hikariConfig.addDataSourceProperty("password", config.dbPassword());
        hikariConfig.setAutoCommit(false);
        hikariConfig.setMaximumPoolSize(8);

        dataSource = new HikariDataSource(hikariConfig);
    }

    private void initSchema(DbConfig config) {
        Flyway.configure()
                .dataSource(config.jdbcUrl(),
                        config.dbUsername(),
                        config.dbPassword())
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }

    @SneakyThrows
    public JdbcSession newSession() {
        return new HikariSession(dataSource.getConnection());
    }

    @AllArgsConstructor
    private static class HikariSession implements JdbcSession {

        private final Connection connection;

        @SneakyThrows
        @Override
        public <Result> Result withConnection(Function<Connection, Result> func) {
            return func.apply(connection);
        }

        @Override
        public void commit() throws SQLException {
            connection.commit();
        }

        @Override
        public void rollback() throws SQLException {
            connection.rollback();
        }

        @Override
        public void close() throws SQLException {
            connection.close();
        }
    }
}
