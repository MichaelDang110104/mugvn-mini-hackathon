package com.hackathon.backend.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LenientStringToIntegerConverterTest {

    private final MongoReadConfig.LenientStringToIntegerConverter converter =
            new MongoReadConfig.LenientStringToIntegerConverter();

    @Test
    void extractsLeadingDigitsFromDirtyNumericString() {
        assertEquals(1987, converter.convert("1987e"));
        assertEquals(1987, converter.convert("1987è"));
        assertEquals(-42, converter.convert("-42oops"));
    }

    @Test
    void returnsNullForBlankOrNonNumericValues() {
        assertNull(converter.convert(null));
        assertNull(converter.convert(""));
        assertNull(converter.convert("   "));
        assertNull(converter.convert("unknown"));
    }
}
