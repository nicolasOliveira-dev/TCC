package com.example.EduGrid.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class BooleanMatrixConverter implements AttributeConverter<Boolean[][][], String> {

    @Override
    public String convertToDatabaseColumn(Boolean[][][] matrix) {
        if (matrix == null) {
            return null;
        }

        StringBuilder value = new StringBuilder();
        for (int turno = 0; turno < matrix.length; turno++) {
            if (turno > 0) {
                value.append('|');
            }
            for (int periodo = 0; periodo < matrix[turno].length; periodo++) {
                if (periodo > 0) {
                    value.append(';');
                }
                for (Boolean disponivel : matrix[turno][periodo]) {
                    value.append(disponivel == null ? '-' : disponivel ? '1' : '0');
                }
            }
        }
        return value.toString();
    }

    @Override
    public Boolean[][][] convertToEntityAttribute(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        String[] turnos = value.split("\\|", -1);
        Boolean[][][] matrix = new Boolean[turnos.length][][];
        for (int turno = 0; turno < turnos.length; turno++) {
            String[] periodos = turnos[turno].split(";", -1);
            matrix[turno] = new Boolean[periodos.length][];
            for (int periodo = 0; periodo < periodos.length; periodo++) {
                matrix[turno][periodo] = new Boolean[periodos[periodo].length()];
                for (int dia = 0; dia < periodos[periodo].length(); dia++) {
                    matrix[turno][periodo][dia] = switch (periodos[periodo].charAt(dia)) {
                        case '1' -> Boolean.TRUE;
                        case '0' -> Boolean.FALSE;
                        case '-' -> null;
                        default -> throw new IllegalArgumentException("Disponibilidade horária persistida inválida.");
                    };
                }
            }
        }
        return matrix;
    }
}
