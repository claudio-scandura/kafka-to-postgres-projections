package com.mylaesoftware;

import com.mylaesoftware.domain.SpeedObservation;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpeedObservationDeserializerTest {

    private final Deserializer<SpeedObservation> underTest = new SpeedObservationDeserializer();

    private SpeedObservation deserialize(String string) {
        return underTest.deserialize("unused", Optional.ofNullable(string).map(String::getBytes).orElse(null));
    }

    @Test
    public void shouldFailDeserializingInvalidPayloads() {
        Stream.of("loikahnx", "ASDRDA:ASDFGH34:", "ASDRDA:ASDFGH34:-1", "ASDRDA:ASDFGH34:a",
                "ASD£DA:ASDFGH34:1", "ASDRDA:ASDFGH34:1333", "ASDRDA:ASDFGH34ASD:1")
                .forEach(invalidObservation ->
                        assertThatThrownBy(() -> deserialize(invalidObservation))
                                .isExactlyInstanceOf(SerializationException.class));
    }

    @Test
    public void shouldSucceedDeserializingValidPayloads() {
        Stream.of("ASDRDA:ASDFGH34:1", "ASDRDA:ASDFGH34:12", "ASDRDA:ASDFGH34:123", "ASDRDA:ASDFGHAD:12")
                .forEach(validObservation -> assertThat(deserialize(validObservation))
                        .hasNoNullFieldsOrProperties());
    }

}