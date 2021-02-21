package com.mylaesoftware;

import com.mylaesoftware.domain.SpeedObservation;
import lombok.NonNull;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class SpeedObservationDeserializer implements Deserializer<SpeedObservation> {

    private static final String radarId = "radarId";
    private static final String licensePlate = "licensePlate";
    private static final String metersPerSecond = "metersPerSecond";
    private static final String payloadRegex =
            String.format("(?<%s>[A-Z]{6}):(?<%s>[A-Z0-9]{8}):(?<%s>\\d{1,3})", radarId, licensePlate,
                    metersPerSecond);
    private static final Pattern pattern = Pattern.compile(payloadRegex);


    @Override
    public SpeedObservation deserialize(String topic, @NonNull byte[] data) {
        var payload = new String(data, StandardCharsets.UTF_8);
        var matcher = pattern.matcher(payload);
        if (!matcher.matches()) {
            throw new SerializationException("payload '" + payload + "' did not match '" + pattern.pattern() + "'");
        }

        return new SpeedObservation(
                matcher.group("radarId"),
                matcher.group("licensePlate"),
                Integer.parseInt(matcher.group("metersPerSecond"))
        );
    }
}
