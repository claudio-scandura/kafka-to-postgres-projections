package com.mylaesoftware.config;

import com.mylaesoftware.annotations.ConfigType;
import com.mylaesoftware.annotations.ConfigValue;

@ConfigType(contextPath = "crazy-highway")
public interface ApplicationConfig extends DbConfig {

    @ConfigValue(atPath = "speed-observations-topic")
    String speedObservationsTopic();

}
