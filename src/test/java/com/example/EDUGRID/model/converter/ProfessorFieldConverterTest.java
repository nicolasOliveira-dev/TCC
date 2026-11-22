package com.example.EduGrid.model.converter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class ProfessorFieldConverterTest {

    @Test
    void converteDiasDePlanejamento() {
        StringListConverter converter = new StringListConverter();
        List<String> dias = List.of("Segunda-feira", "Quarta-feira");

        assertEquals(dias, converter.convertToEntityAttribute(converter.convertToDatabaseColumn(dias)));
    }

    @Test
    void converteMatrizDeDisponibilidadeDeTodosOsTurnos() {
        BooleanMatrixConverter converter = new BooleanMatrixConverter();
        Boolean[][][] disponibilidade = {
                {{true, false}, {false, true}},
                {{false, false}, {true, null}}
        };

        Boolean[][][] restaurada = converter.convertToEntityAttribute(
                converter.convertToDatabaseColumn(disponibilidade));

        assertArrayEquals(disponibilidade[0][0], restaurada[0][0]);
        assertArrayEquals(disponibilidade[0][1], restaurada[0][1]);
        assertArrayEquals(disponibilidade[1][0], restaurada[1][0]);
        assertArrayEquals(disponibilidade[1][1], restaurada[1][1]);
    }
}
