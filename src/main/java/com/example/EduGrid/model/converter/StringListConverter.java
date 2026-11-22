package com.example.EduGrid.model.converter;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> values) {
        return values == null ? null : String.join(",", values);
    }

    @Override
    public List<String> convertToEntityAttribute(String value) {
        return value == null || value.isEmpty() ? List.of() : Arrays.asList(value.split(",", -1));
    }
}
