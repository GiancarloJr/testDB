package com.test.voting.facade;

import com.test.voting.dto.CpfValidationResponse;
import com.test.voting.model.enums.VoteStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.regex.Pattern;

@Component
@Slf4j
public class CpfValidationFacade {

    private final Random random = new Random();
    private static final Pattern CPF_LENGTH = Pattern.compile("\\d{11}");
    private static final Pattern ALL_SAME = Pattern.compile("(\\d)\\1{10}");

    public CpfValidationResponse validateCpf(String cpf) {
        log.debug("Validating CPF: {}", cpf);

        if (!CPF_LENGTH.matcher(cpf).matches()) return null;
        if (ALL_SAME.matcher(cpf).matches()) return null;

        if (!isValidCpf(cpf)) {
            log.warn("CPF with invalid check digit: {}", cpf);
            return null;
        }

        boolean unable = random.nextInt(100) < 5;
        VoteStatus status = unable ? VoteStatus.UNABLE_TO_VOTE : VoteStatus.ABLE_TO_VOTE;
        
        log.debug("CPF {} validation result: {}", cpf, status);

        if (status == VoteStatus.UNABLE_TO_VOTE) {
            log.debug("CPF {} is unable to vote.", cpf);
        }

        return new CpfValidationResponse(status);
    }

    private boolean isValidCpf(String cpf) {
        try {
            int sum = 0;
            for (int i = 0; i < 9; i++) {
                sum += Character.getNumericValue(cpf.charAt(i)) * (10 - i);
            }
            int firstDigit = 11 - (sum % 11);
            if (firstDigit >= 10) firstDigit = 0;

            if (firstDigit != Character.getNumericValue(cpf.charAt(9))) {
                return false;
            }

            sum = 0;
            for (int i = 0; i < 10; i++) {
                sum += Character.getNumericValue(cpf.charAt(i)) * (11 - i);
            }
            int secondDigit = 11 - (sum % 11);
            if (secondDigit >= 10) secondDigit = 0;

            return secondDigit == Character.getNumericValue(cpf.charAt(10));
        } catch (Exception e) {
            return false;
        }
    }
}
