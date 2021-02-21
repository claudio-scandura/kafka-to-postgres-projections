package com.mylaesoftware.config;

import com.mylaesoftware.annotations.ConfigType;
import com.mylaesoftware.annotations.ConfigValue;

@ConfigType(contextPath = "crazy-highway.db")
public interface DbConfig {

    @ConfigValue(atPath = "jdbc-url")
    default String jdbcUrl() {
        return String.format("jdbc:postgresql://%s:%d/%s", dbServerName(), dbPort(), dbName());
    }

    @ConfigValue(atPath = "server-name")
    String dbServerName();

    @ConfigValue(atPath = "name")
    String dbName();

    @ConfigValue(atPath = "port")
    Integer dbPort();

    @ConfigValue(atPath = "username")
    String dbUsername();

    @ConfigValue(atPath = "password")
    String dbPassword();

}
