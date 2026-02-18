package com.test.voting.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CpfUtils {

    public static String formatCpf(String cpf) {
        if (cpf == null) {
            return null;
        }
        return cpf.replaceAll("[^0-9]", "");
    }
}
