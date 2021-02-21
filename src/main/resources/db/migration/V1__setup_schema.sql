CREATE TABLE IF NOT EXISTS akka_projection_offset_store (
    projection_name VARCHAR(255) NOT NULL,
    projection_key VARCHAR(255) NOT NULL,
    current_offset VARCHAR(255) NOT NULL,
    manifest VARCHAR(4) NOT NULL,
    mergeable BOOLEAN NOT NULL,
    last_updated BIGINT NOT NULL,
    PRIMARY KEY(projection_name, projection_key)
);

CREATE INDEX IF NOT EXISTS projection_name_index ON akka_projection_offset_store (projection_name);

create TABLE IF NOT EXISTS STATS_BY_RADAR (
    RADAR_ID VARCHAR(6) NOT NULL,
    SUM_METERS_PER_SEC BIGINT NOT NULL,
    TOT_OBSERVATIONS BIGINT NOT NULL,
    PRIMARY KEY (RADAR_ID)
);

create TABLE IF NOT EXISTS STATS_BY_LICENSE_PLATE (
    LICENSE_PLATE VARCHAR(8) NOT NULL,
    MAX_METERS_PER_SEC BIGINT NOT NULL,
    LAST_SEEN_AT_RADAR VARCHAR(6) NOT NULL,
    PRIMARY KEY (LICENSE_PLATE)
);