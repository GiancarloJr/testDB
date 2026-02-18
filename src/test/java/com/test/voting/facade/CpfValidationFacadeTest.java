package com.test.voting.facade;

import com.test.voting.dto.CpfValidationResponse;
import com.test.voting.model.enums.VoteStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CpfValidationFacadeTest {

    private final CpfValidationFacade facade = new CpfValidationFacade();

    @Test
    void shouldReturnResponseForValidCpf() {
        CpfValidationResponse response = facade.validateCpf("12345678909");

        assertThat(response).isNotNull();
        assertThat(response.status()).isIn(VoteStatus.ABLE_TO_VOTE, VoteStatus.UNABLE_TO_VOTE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000000", "11111111111", "22222222222", "99999999999"})
    void shouldReturnNullForAllSameDigits(String cpf) {
        assertThat(facade.validateCpf(cpf)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567890", "123456789012", "abcdefghijk", "123.456.789-09"})
    void shouldReturnNullForInvalidFormat(String cpf) {
        assertThat(facade.validateCpf(cpf)).isNull();
    }

    @Test
    void shouldReturnNullForInvalidCheckDigit() {
        assertThat(facade.validateCpf("12345678900")).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"59186232134", "01582728119"})
    void shouldReturnResponseForKnownValidCpfs(String cpf) {
        CpfValidationResponse response = facade.validateCpf(cpf);

        assertThat(response).isNotNull();
        assertThat(response.status()).isIn(VoteStatus.ABLE_TO_VOTE, VoteStatus.UNABLE_TO_VOTE);
    }
}
