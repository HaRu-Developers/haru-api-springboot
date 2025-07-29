package com.haru.api.global.util;

import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HashIdUtil {
    private final Hashids hashids;

    public HashIdUtil(@Value("${hashid.salt}") String salt,
                      @Value("${hashid.min-length}") int minLength) {
        this.hashids = new Hashids(salt, minLength);
    }

    public String encode(Long id) {
        return hashids.encode(id);
    }

    public Long decode(String hash) {
        long[] decoded = hashids.decode(hash);
        if (decoded.length == 0) throw new IllegalArgumentException("Invalid hash ID");
        return decoded[0];
    }
}
