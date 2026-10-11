package com.ensambla.backend.domain.model.plantilla;

import com.ensambla.backend.domain.model.enums.TipoCampo;

import java.util.List;
import java.util.Objects;

public record EstructuraPlantilla(List<BloquePlantilla> bloques) {

    public record BloquePlantilla(String nombre, List<CampoPlantilla> campos) {
    }

    public record CampoPlantilla(
            String nombre,
            TipoCampo tipo,
            Boolean obligatorio,
            String valorInicial,
            List<String> opciones
    ) {

        public CampoPlantilla {
            obligatorio = Objects.requireNonNullElse(obligatorio, false);
            opciones = List.copyOf(Objects.requireNonNullElse(opciones, List.of()));
        }
    }
}

