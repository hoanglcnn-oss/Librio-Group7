package com.librio.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IsbnUtilsTest {

    @Test
    void normalizeIsbn13_valid() {
        String res = IsbnUtils.normalizeToIsbn13("978-0-13-235088-4");
        assertThat(res).isEqualTo("9780132350884");
    }

    @Test
    void normalizeIsbn10_validAndConverts() {
        String res = IsbnUtils.normalizeToIsbn13("0-13-235088-2");
        assertThat(res).isEqualTo("9780132350884");
    }

    @Test
    void normalizeIsbn10_withX() {
        String res = IsbnUtils.normalizeToIsbn13("0-8044-2957-X");
        assertThat(res).isEqualTo("9780804429573");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "978-0-13-235088-5", // invalid checksum 13
            "0-13-235088-3", // invalid checksum 10
            "abc",
            "123456789", // length 9
            "0-8044-2957-Y" // invalid char
    })
    void normalize_invalid_throws(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> IsbnUtils.normalizeToIsbn13(invalid));
    }
}
