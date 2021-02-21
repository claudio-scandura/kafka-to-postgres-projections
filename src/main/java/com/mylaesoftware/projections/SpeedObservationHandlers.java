package com.mylaesoftware.projections;

import akka.projection.jdbc.JdbcSession;
import akka.projection.jdbc.javadsl.JdbcHandler;
import com.mylaesoftware.domain.SpeedObservation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import scala.runtime.BoxedUnit;

import java.sql.Connection;
import java.sql.PreparedStatement;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SpeedObservationHandlers {

    private static final String STATS_BY_RADAR_UPSERT =
            "INSERT INTO STATS_BY_RADAR AS sbr ("
                    + "     radar_id, sum_meters_per_sec, tot_observations)"
                    + " VALUES (?,?,?)"
                    + " ON CONFLICT (radar_id) "
                    + " DO UPDATE SET tot_observations = sbr.tot_observations + 1,"
                    + "    sum_meters_per_sec = sbr.sum_meters_per_sec + excluded.sum_meters_per_sec";

    private static final String STATS_BY_LICENSE_PLATE_UPSERT =
            "INSERT INTO STATS_BY_LICENSE_PLATE AS sblp (license_plate, max_meters_per_sec, last_seen_at_radar)"
                    + " VALUES (?,?,?)"
                    + " ON CONFLICT (license_plate) "
                    +
                    " DO UPDATE SET max_meters_per_sec = GREATEST(sblp.max_meters_per_sec, excluded.max_meters_per_sec),"
                    + " last_seen_at_radar = excluded.last_seen_at_radar";


    @SneakyThrows
    static BoxedUnit updateStatsByRadar(JdbcSession session,
                                               ConsumerRecord<String, SpeedObservation> observationRecord) {
        return session.withConnection(
                connection -> runUpdateQuery(connection, STATS_BY_RADAR_UPSERT, statement -> {
                    var observation = observationRecord.value();
                    statement.setString(1, observation.radarId);
                    statement.setInt(2, observation.metersPerSecond);
                    statement.setInt(3, 1);
                    return statement;
                }));
    }

    @SneakyThrows
    static BoxedUnit updateStatsByLicensePlate(JdbcSession session,
                                                       ConsumerRecord<String, SpeedObservation> observationRecord) {
        return session.withConnection(
                connection -> runUpdateQuery(connection, STATS_BY_LICENSE_PLATE_UPSERT, statement -> {
                    var observation = observationRecord.value();
                    statement.setString(1, observation.licensePlate);
                    statement.setInt(2, observation.metersPerSecond);
                    statement.setString(3, observation.radarId);
                    return statement;
                }));
    }

    @SneakyThrows
    private static BoxedUnit runUpdateQuery(Connection connection, String query,
                                            akka.japi.function.Function<PreparedStatement, PreparedStatement> statementBuilder) {
        var statement = connection.prepareStatement(query);
        statementBuilder.apply(statement).executeUpdate();
        return BoxedUnit.UNIT;
    }

}
