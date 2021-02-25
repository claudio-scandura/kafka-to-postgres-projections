
package com.mylaesoftware;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.kafka.ConsumerSettings;
import akka.projection.kafka.javadsl.KafkaSourceProvider;
import com.mylaesoftware.config.ApplicationConfig;
import com.mylaesoftware.projections.SpeedStatsProjectionRunner;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Set;

public class App {

    public static void main(String[] args) {
        var system = ActorSystem.create(Behaviors.empty(), "Kafka-to-Postgres-projection");

        ApplicationConfig config = ConfigComposer.wire(system.settings().config());

        var sessionProvider = new HikariSessionProvider(config);

        var consumerSettings =
                ConsumerSettings.create(system, new StringDeserializer(), new SpeedObservationDeserializer())
                        .withGroupId("projection-runner");
        var eventSource = KafkaSourceProvider.create(system, consumerSettings, Set.of(config.speedObservationsTopic()));

        var projectionRunner = new SpeedStatsProjectionRunner(eventSource, sessionProvider);

        projectionRunner.runStatsByRadar(system);
        projectionRunner.runStatsByLicensePlate(system);

    }

}

