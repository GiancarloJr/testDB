package com.test.voting.utils;

import org.junit.jupiter.api.Test;

import static com.test.voting.utils.CpfUtils.formatCpf;
import static org.assertj.core.api.Assertions.assertThat;

class CpfUtilsTest {

    @Test
    void shouldRemoveNonNumericCharacters() {
        assertThat(formatCpf("123.456.789-09")).isEqualTo("12345678909");
    }

    @Test
    void shouldReturnSameWhenAlreadyFormatted() {
        assertThat(formatCpf("12345678909")).isEqualTo("12345678909");
    }

    @Test
    void shouldRemoveSpacesAndSpecialChars() {
        assertThat(formatCpf(" 123 456 789 09 ")).isEqualTo("12345678909");
    }

    @Test
    void shouldReturnEmptyWhenOnlySpecialChars() {
        assertThat(formatCpf("...---")).isEmpty();
    }
}
