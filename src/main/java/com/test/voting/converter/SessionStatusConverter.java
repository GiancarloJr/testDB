package com.test.voting.converter;

import com.test.voting.model.enums.SessionStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.stream.Stream;

@Converter
public class SessionStatusConverter implements AttributeConverter<SessionStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(SessionStatus attribute) {
        return attribute.getCode();
    }

    @Override
    public SessionStatus convertToEntityAttribute(Integer code) {
        return Stream.of(SessionStatus.values())
                .filter(status -> status.getCode() == code)
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }


}
