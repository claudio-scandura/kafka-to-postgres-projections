-- The offset store as required and defined by Akka Projection JDBC
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

-- The table that tracks for each radar the total number of observations
-- and the sum of the reported speed in meters per second
create TABLE IF NOT EXISTS STATS_BY_RADAR (
    RADAR_ID VARCHAR(6) NOT NULL,
    SUM_METERS_PER_SEC BIGINT NOT NULL,
    TOT_OBSERVATIONS BIGINT NOT NULL,
    PRIMARY KEY (RADAR_ID)
);

-- The table that tracks for each license plate the maximum observed speed
-- of the respective vehicle and the last radar it was seen
create TABLE IF NOT EXISTS STATS_BY_LICENSE_PLATE (
    LICENSE_PLATE VARCHAR(8) NOT NULL,
    MAX_METERS_PER_SEC BIGINT NOT NULL,
    LAST_SEEN_AT_RADAR VARCHAR(6) NOT NULL,
    PRIMARY KEY (LICENSE_PLATE)
);