package com.mylaesoftware.streams;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.japi.Pair;
import akka.stream.testkit.TestSubscriber;
import akka.stream.testkit.javadsl.TestSink;
import com.mylaesoftware.repo.VehicleStatsRepo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.verification.AtLeast;
import org.mockito.verification.Timeout;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StatsStreamerTest {

    private static final ActorTestKit testKit = ActorTestKit.create();

    private final VehicleStatsRepo repoMock = Mockito.mock(VehicleStatsRepo.class);

    @AfterAll
    public static void teardown() {
        testKit.shutdownTestKit();
    }

    @BeforeEach
    public void setup() {
        Mockito.reset(repoMock);
    }

    @Test
    public void shouldStartPollingRepoWhenSpawned() {
        when(repoMock.findFastestVechicles(anyInt())).thenReturn(CompletableFuture.completedFuture(List.of()));

        testKit.spawn(StatsStreamer.create(repoMock));

        repoIsPolledAtLeast(1);
    }

    @Test
    public void shouldPublishUpdateToSubscriber() {
        var update = Pair.create("XYZ", 12345);
        repoReturns(update);

        var underTest = testKit.spawn(StatsStreamer.create(repoMock));

        repoIsPolledAtLeast(1);

        var subscriberProbe = testKit.<StatsStreamer.SubscriberAdded>createTestProbe();

        underTest.tell(new StatsStreamer.Subscribe(subscriberProbe.ref()));

        var actualEventsProbe = subscriberProbe
                .expectMessageClass(StatsStreamer.SubscriberAdded.class)
                .events.runWith(TestSink.probe(testKit.system().classicSystem()), testKit.system());

        expectProbeReceives(actualEventsProbe, update);

        testKit.stop(underTest);
        actualEventsProbe.expectComplete();
    }

    @Test
    public void shouldPublishMultipleUpdatesToSubscriber() {
        var update = Pair.create("XYZ", 12345);
        repoReturns(update);

        var underTest = testKit.spawn(StatsStreamer.create(repoMock));

        repoIsPolledAtLeast(1);

        var subscriberProbe = testKit.<StatsStreamer.SubscriberAdded>createTestProbe();

        underTest.tell(new StatsStreamer.Subscribe(subscriberProbe.ref()));

        var actualEventsProbe = subscriberProbe
                .expectMessageClass(StatsStreamer.SubscriberAdded.class)
                .events.runWith(TestSink.probe(testKit.system().classicSystem()), testKit.system());

        expectProbeReceives(actualEventsProbe, update);

        repoIsPolledAtLeast(1);
        actualEventsProbe.expectNoMessage();

        var newUpdate = Pair.create("ZYX", 54321);
        repoReturns(update, newUpdate);

        expectProbeReceives(actualEventsProbe, update, newUpdate);

        testKit.stop(underTest);
        actualEventsProbe.expectComplete();
    }

    @Test
    public void shouldNotPublishUpdatesToUnsubscribedSubscriber() {
        var update = Pair.create("XYZ", 12345);
        repoReturns(update);

        var underTest = testKit.spawn(StatsStreamer.create(repoMock));

        repoIsPolledAtLeast(1);

        var subscriberProbe = testKit.<StatsStreamer.SubscriberAdded>createTestProbe();

        underTest.tell(new StatsStreamer.Subscribe(subscriberProbe.ref()));

        var actualEventsProbe = subscriberProbe
                .expectMessageClass(StatsStreamer.SubscriberAdded.class)
                .events.runWith(TestSink.probe(testKit.system().classicSystem()), testKit.system());

        expectProbeReceives(actualEventsProbe, update);

        actualEventsProbe.cancel();

        var newUpdate = Pair.create("ZYX", 54321);
        repoReturns(update, newUpdate);

        repoIsPolledAtLeast(1);
        actualEventsProbe.expectNoMessage();

        testKit.stop(underTest);
    }


    private void repoIsPolledAtLeast(int times) {
        verify(repoMock, new Timeout(2000, new AtLeast(times))).findFastestVechicles(Mockito.anyInt());
    }

    private void repoReturns(Pair<String, Integer>... update) {
        when(repoMock.findFastestVechicles(anyInt())).thenReturn(CompletableFuture.completedFuture(List.of(update)));
    }

    @SafeVarargs
    private void expectProbeReceives(TestSubscriber.Probe<StatsStreamer.FastestNCars> actualEventsProbe,
                                     Pair<String, Integer>... updates) {
        assertThat(actualEventsProbe.requestNext()).isEqualTo(new StatsStreamer.FastestNCars(List.of(updates)));
    }


}