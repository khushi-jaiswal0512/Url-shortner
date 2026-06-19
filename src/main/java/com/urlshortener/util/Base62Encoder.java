package com.urlshortener.util;

import org.springframework.stereotype.Component;

/**
 * Base62 encoder/decoder for converting numeric IDs to short URL codes.
 * Character set: 0-9, a-z, A-Z (62 characters total).
 */
@Component
public class Base62Encoder {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length(); // 62

    /**
     * Encodes a positive long ID into a Base62 string.
     *
     * @param id the numeric ID (must be >= 0)
     * @return the Base62-encoded string
     */
    public String encode(long id) {
        if (id < 0) {
            throw new IllegalArgumentException("ID must be non-negative, got: " + id);
        }
        if (id == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }

        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            sb.append(ALPHABET.charAt((int) (id % BASE)));
            id /= BASE;
        }
        return sb.reverse().toString();
    }

    /**
     * Decodes a Base62 string back into its numeric ID.
     *
     * @param shortCode the Base62-encoded string
     * @return the original numeric ID
     */
    public long decode(String shortCode) {
        long id = 0;
        for (char c : shortCode.toCharArray()) {
            int index = ALPHABET.indexOf(c);
            if (index == -1) {
                throw new IllegalArgumentException("Invalid Base62 character: " + c);
            }
            id = id * BASE + index;
        }
        return id;
    }
}
