package com.mylaesoftware.streams;

import akka.Done;
import akka.NotUsed;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.japi.Pair;
import akka.japi.function.Function2;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.SourceQueueWithComplete;
import com.mylaesoftware.repo.VehicleStatsRepo;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class StatsStreamer extends AbstractBehavior<StatsStreamer.StatsStreaming> {

    private final VehicleStatsRepo repo;
    private List<Pair<String, Integer>> stats = List.of();
    private final Map<Long, SourceQueueWithComplete<FastestNCars>> subscribers = new HashMap<>();
    private long nextSubscriberId = 0L;

    private StatsStreamer(ActorContext<StatsStreaming> context,
                          VehicleStatsRepo repo) {
        super(context);
        this.repo = repo;
    }


    public static Behavior<StatsStreaming> create(VehicleStatsRepo repo) {
        return Behaviors.withTimers(timer -> {
            timer.startTimerWithFixedDelay(PollRepo.INSTANCE, Duration.ofSeconds(1));
            return Behaviors.setup(ctx -> new StatsStreamer(ctx, repo));
        });
    }

    @Override
    public Receive<StatsStreaming> createReceive() {

        return newReceiveBuilder()
                .onMessageEquals(PollRepo.INSTANCE, this::pollRepo)
                .onMessage(UpdateStats.class, this::updateStats)
                .onMessage(Subscribe.class, this::subscribe)
                .onMessage(AddSubscriber.class, this::addSubscriber)
                .onMessage(RemoveSubscriber.class, this::removeSubscriber)
                .onSignalEquals(PostStop.instance(), this::completeAllSubscribers)
                .build();
    }

    private Behavior<StatsStreaming> completeAllSubscribers() {
        subscribers.values().forEach(SourceQueueWithComplete::complete);
        subscribers.clear();
        return this;
    }

    private Behavior<StatsStreaming> addSubscriber(AddSubscriber addSubscriber) {
        addSubscriber.subscriberQueue.offer(new FastestNCars(stats));
        subscribers.put(addSubscriber.subscriberId, addSubscriber.subscriberQueue);
        return this;
    }

    private Behavior<StatsStreaming> removeSubscriber(RemoveSubscriber removeSubscriber) {
        subscribers.remove(removeSubscriber.subscriberId);
        log.info("Subscriber {} successfully unsubscribed", removeSubscriber.subscriberId);
        return this;
    }

    private Behavior<StatsStreaming> subscribe(Subscribe subscribe) {
        long subscriberId = ++nextSubscriberId;
        var events = Source.<FastestNCars>queue(100, OverflowStrategy.backpressure())
                .mapMaterializedValue(queue -> {
                    getContext().getSelf().tell(new AddSubscriber(subscriberId, queue));
                    return NotUsed.notUsed();
                }).watchTermination((nu, completion) -> {
                    getContext().pipeToSelf(completion, handleSubscriberTermination(subscriberId));
                    return nu;
                });
        subscribe.replyTo.tell(new SubscriberAdded(events));
        return this;
    }

    private Function2<Done, Throwable, StatsStreaming> handleSubscriberTermination(long subscriberId) {
        return (done, throwable) -> {
            if (!Objects.isNull(throwable)) {
                log.warn("Subscriber {} terminated with error", subscriberId, throwable);
            }
            log.info("Subscriber {} terminated gracefully", subscriberId);
            return new RemoveSubscriber(subscriberId);
        };
    }

    private Behavior<StatsStreaming> updateStats(UpdateStats a) {
        if (!a.stats.equals(stats)) {
            stats = a.stats;
            var update = new FastestNCars(a.stats);
            subscribers.values().forEach(s -> s.offer(update));
        }
        return this;
    }

    private Behavior<StatsStreaming> pollRepo() {
        var latestResult = repo.findFastestVechicles(10);
        getContext().pipeToSelf(latestResult, (ok, ko) -> new UpdateStats(ok));
        return this;
    }


    public interface StatsStreaming {
    }

    private enum PollRepo implements StatsStreaming {INSTANCE}

    @AllArgsConstructor
    private static class UpdateStats implements StatsStreaming {
        final List<Pair<String, Integer>> stats;
    }

    @AllArgsConstructor
    private static class AddSubscriber implements StatsStreaming {
        final Long subscriberId;
        final SourceQueueWithComplete<FastestNCars> subscriberQueue;
    }

    @AllArgsConstructor
    private static class RemoveSubscriber implements StatsStreaming {
        final Long subscriberId;
    }

    @AllArgsConstructor
    public static class Subscribe implements StatsStreaming {
        ActorRef<SubscriberAdded> replyTo;
    }


    @AllArgsConstructor
    public static class SubscriberAdded {
        final Source<FastestNCars, NotUsed> events;
    }


    public interface StatsSubscriber {
    }

    @Value
    public static class FastestNCars implements StatsSubscriber {
        final List<Pair<String, Integer>> cars;
    }
}
