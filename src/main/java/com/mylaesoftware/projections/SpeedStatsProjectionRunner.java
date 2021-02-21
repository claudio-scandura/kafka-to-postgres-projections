package com.mylaesoftware.projections;

import akka.actor.typed.ActorSystem;
import akka.projection.MergeableOffset;
import akka.projection.ProjectionId;
import akka.projection.RunningProjection;
import akka.projection.javadsl.SourceProvider;
import akka.projection.jdbc.javadsl.JdbcHandler;
import akka.projection.jdbc.javadsl.JdbcProjection;
import com.mylaesoftware.domain.SpeedObservation;
import com.mylaesoftware.PostgreSqlSessionProvider;
import lombok.AllArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@AllArgsConstructor
public class SpeedStatsProjectionRunner {

    private final SourceProvider<MergeableOffset<Long>, ConsumerRecord<String, SpeedObservation>> sourceProvider;
    private final PostgreSqlSessionProvider sessionProvider;

    public RunningProjection runStatsByRadar(ActorSystem<?> system) {
        return JdbcProjection.exactlyOnce(
                ProjectionId.of("StatsByRadar", "unused"),
                sourceProvider,
                sessionProvider::newSession,
                () -> JdbcHandler.fromFunction(SpeedObservationHandlers::updateStatsByRadar),
                system
        ).run(system);
    }

    public RunningProjection runStatsByLicensePlate(ActorSystem<?> system) {
        return JdbcProjection.exactlyOnce(
                ProjectionId.of("StatsByLicensePlate", "unused"),
                sourceProvider,
                sessionProvider::newSession,
                () -> JdbcHandler.fromFunction(SpeedObservationHandlers::updateStatsByLicensePlate),
                system
        ).run(system);
    }

}
