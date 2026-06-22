package com.urlshortener.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for Base62 encoding/decoding.
 */
class Base62EncoderTest {

    private final Base62Encoder encoder = new Base62Encoder();

    @Test
    @DisplayName("encode(0) should return '0'")
    void encode_zero() {
        assertThat(encoder.encode(0)).isEqualTo("0");
    }

    @Test
    @DisplayName("encode(1) should return '1'")
    void encode_one() {
        assertThat(encoder.encode(1)).isEqualTo("1");
    }

    @ParameterizedTest(name = "encode({0}) = \"{1}\"")
    @CsvSource({
            "10, A",
            "35, Z",
            "36, a",
            "61, z",
            "62, 10",
            "1000, G8",
            "123456789, 8M0kX"
    })
    @DisplayName("Encode known values")
    void encode_knownValues(long id, String expected) {
        assertThat(encoder.encode(id)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "decode(\"{1}\") = {0}")
    @CsvSource({
            "0, 0",
            "1, 1",
            "10, A",
            "62, 10",
            "1000, G8",
            "123456789, 8M0kX"
    })
    @DisplayName("Decode known values")
    void decode_knownValues(long expected, String shortCode) {
        assertThat(encoder.decode(shortCode)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Encode/decode roundtrip integrity")
    void roundtrip_integrity() {
        long[] testIds = {1, 42, 100, 999, 10000, 1000000, Long.MAX_VALUE / 2};
        for (long id : testIds) {
            String encoded = encoder.encode(id);
            long decoded = encoder.decode(encoded);
            assertThat(decoded)
                    .as("Roundtrip failed for id=%d, encoded='%s'", id, encoded)
                    .isEqualTo(id);
        }
    }

    @Test
    @DisplayName("Encode negative ID throws exception")
    void encode_negativeId_throwsException() {
        assertThatThrownBy(() -> encoder.encode(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
    }

    @Test
    @DisplayName("Decode invalid character throws exception")
    void decode_invalidCharacter_throwsException() {
        assertThatThrownBy(() -> encoder.decode("abc!def"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Base62 character");
    }
}
