package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TipoMiniObjetivoTest {

    @ParameterizedTest
    @CsvSource({
            "SUBIR_PRECIO, SUBE, true",
            "MOVER_PARADO, SUBE, true",
            "PONER_A_LA_VISTA, SUBE, true",
            "RECORTAR_GASTO, BAJA, false",
            "MEJORAR_MARGEN, SUBE, false",
            "VENTAS_SEMANALES, SUBE, false"})
    @DisplayName("Cada tipo sabe hacia dónde va su métrica y si apunta a un producto")
    void tipoShouldKnowItsDirectionAndWhetherItIsAboutAProduct(
            TipoMiniObjetivo tipo, SentidoMiniObjetivo sentido, boolean deProducto) {
        assertThat(tipo.sentido()).isEqualTo(sentido);
        assertThat(tipo.esDeProducto()).isEqualTo(deProducto);
    }
}
