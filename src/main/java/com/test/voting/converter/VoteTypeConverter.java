package com.test.voting.converter;

import com.test.voting.model.enums.VoteType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.stream.Stream;

@Converter
public class VoteTypeConverter implements AttributeConverter<VoteType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(VoteType attribute) {
        return attribute.getCode();
    }

    @Override
    public VoteType convertToEntityAttribute(Integer code) {
        return Stream.of(VoteType.values())
                .filter(status -> status.getCode() == code)
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
