package com.mylaesoftware;

import akka.Done;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.kafka.ConsumerSettings;
import akka.projection.kafka.javadsl.KafkaSourceProvider;
import com.mylaesoftware.config.ApplicationConfig;
import com.mylaesoftware.domain.SpeedObservation;
import com.mylaesoftware.projections.SpeedStatsProjectionRunner;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import scala.compat.java8.FutureConverters;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

public class SpeedStatsProjectionTest {

    private static final Duration timeout = Duration.ofSeconds(10);

    private final ActorSystem<?> system =
            ActorSystem.create(Behaviors.empty(), SpeedStatsProjectionTest.class.getSimpleName());

    private static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:12.2"));

    private static final KafkaContainer kafka = new KafkaContainer()
            .withEmbeddedZookeeper();

    private final ConsumerSettings<String, SpeedObservation> consumerSettings =
            ConsumerSettings.create(system, new StringDeserializer(), new SpeedObservationDeserializer()).withGroupId("projection");


    @BeforeAll
    public static void beforeAll() {
        postgres.start();
        kafka.start();
    }

    @AfterAll
    public static void afterAll() {
        postgres.stop();
        kafka.stop();
    }

    @Test
    public void shouldProjectStatsByRadar() throws Exception {
        var testTopic = createRandomTopic();
        var sessionProvider = createSessionProvider();

        var eventSource = KafkaSourceProvider
                .create(system, consumerSettings.withBootstrapServers(kafka.getBootstrapServers()), Set.of(testTopic));

        Stream.of("ASDASA:ASDFGH34:123",
                "ASDASA:ASDFGH34:321",
                "ASDASB:ASDFGH34:100").forEach(value -> produceMessage(testTopic, value));

        var projection = new SpeedStatsProjectionRunner(eventSource, sessionProvider).runStatsByRadar(system);

        Awaitility.await().untilAsserted(() ->
                withConnection(connection -> {
                    var resultSet = connection.prepareStatement("select * from STATS_BY_RADAR").executeQuery();
                    assertThat(queryStatsTable(resultSet, StatsByRadarRow::fromResultSet)).containsExactlyInAnyOrder(
                            new StatsByRadarRow("ASDASA", 444, 2),
                            new StatsByRadarRow("ASDASB", 100, 1)
                    );
                })

        );


        assertThat(FutureConverters.toJava(projection.stop())).succeedsWithin(timeout).isEqualTo(Done.done());
    }


    @Test
    public void shouldProjectStatsByLicensePlates() throws Exception {
        var testTopic = createRandomTopic();
        var sessionProvider = createSessionProvider();

        var eventSource = KafkaSourceProvider
                .create(system, consumerSettings.withBootstrapServers(kafka.getBootstrapServers()), Set.of(testTopic));

        Stream.of("ASDASA:ASDFGH34:123",
                "ASDASA:DSAFGH34:321",
                "ASDASB:DSAFGH34:1",
                "ASDASB:ASCFGH34:100").forEach(value -> produceMessage(testTopic, value));

        var projection = new SpeedStatsProjectionRunner(eventSource, sessionProvider).runStatsByLicensePlate(system);

        Awaitility.await().untilAsserted(() ->
                withConnection(connection -> {
                    var resultSet =
                            connection.prepareStatement("select * from STATS_BY_LICENSE_PLATE").executeQuery();
                    assertThat(queryStatsTable(resultSet, StatsByLicensePlateRow::fromResultSet))
                            .containsExactlyInAnyOrder(
                                    new StatsByLicensePlateRow("ASDFGH34", 123, "ASDASA"),
                                    new StatsByLicensePlateRow("DSAFGH34", 321, "ASDASB"),
                                    new StatsByLicensePlateRow("ASCFGH34", 100, "ASDASB")
                            );
                })

        );

        assertThat(FutureConverters.toJava(projection.stop())).succeedsWithin(timeout).isEqualTo(Done.done());
    }

    private PostgreSqlSessionProvider createSessionProvider() {
        var appConfig = TestConfig.builder()
                .dbName(postgres.getDatabaseName())
                .dbUsername(postgres.getUsername())
                .dbPassword(postgres.getPassword())
                .dbServerName(postgres.getHost())
                .dbPort(postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT))
                .build();
        return new PostgreSqlSessionProvider(appConfig);
    }

    @SneakyThrows
    private void withConnection(ThrowingConsumer<Connection> consumer) {
        try (var connection = DriverManager
                .getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            consumer.accept(connection);
        }
    }


    private <T> List<T> queryStatsTable(ResultSet resultSet, Function<ResultSet, T> mapper) throws SQLException {
        var fetchedRecords = new ArrayList<T>();
        while (resultSet.next()) {
            fetchedRecords.add(mapper.apply(resultSet));
        }
        return List.copyOf(fetchedRecords);
    }

    private String createRandomTopic() throws InterruptedException, ExecutionException, TimeoutException {
        var topic = UUID.randomUUID().toString();
        try (var client = AdminClient.create(Map.of(BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()))) {
            client.createTopics(List.of(new NewTopic(topic, 3, (short) 1))).all()
                    .get(timeout.getSeconds(), TimeUnit.SECONDS);
        }
        return topic;
    }

    @SneakyThrows
    private void produceMessage(String topic, String message) {

        var config = Map.<String, Object>of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        try (var producer = new KafkaProducer<>(config, new StringSerializer(), new StringSerializer())) {
            producer.send(new ProducerRecord<>(topic, message)).get(timeout.getSeconds(), TimeUnit.SECONDS);
        }
    }

    @Value
    static class StatsByRadarRow {
        String radarId;
        int metersPerSecond;
        int totalObservations;

        @SneakyThrows
        static StatsByRadarRow fromResultSet(ResultSet resultSet) {
            return new StatsByRadarRow(
                    resultSet.getString("radar_id"),
                    resultSet.getInt("sum_meters_per_sec"),
                    resultSet.getInt("tot_observations"));
        }
    }

    @Value
    static class StatsByLicensePlateRow {
        String licensePlate;
        int maxMetersPerSecond;
        String latSeenAtRadar;

        @SneakyThrows
        static StatsByLicensePlateRow fromResultSet(ResultSet resultSet) {
            return new StatsByLicensePlateRow(
                    resultSet.getString("license_plate"),
                    resultSet.getInt("max_meters_per_sec"),
                    resultSet.getString("last_seen_at_radar"));
        }
    }

    @Builder
    @Value
    static class TestConfig implements ApplicationConfig {
        String speedObservationTopic;
        String dbServerName;
        Integer dbPort;
        String dbUsername;
        String dbName;
        String dbPassword;

        @Override
        public String speedObservationsTopic() {
            return speedObservationTopic;
        }

        @Override
        public String dbServerName() {
            return dbServerName;
        }

        @Override
        public String dbName() {
            return dbName;
        }

        @Override
        public Integer dbPort() {
            return dbPort;
        }

        @Override
        public String dbUsername() {
            return dbUsername;
        }

        @Override
        public String dbPassword() {
            return dbPassword;
        }
    }
}
