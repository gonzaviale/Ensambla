package com.ensambla.backend.domain.repository.zona;

import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.model.zona.ZonaId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ZonaRepositoryTest {

    private static final BigDecimal LATITUD = new BigDecimal("-34.670500");
    private static final BigDecimal LONGITUD = new BigDecimal("-58.562500");

    private final ZonaRepository zonaRepository = mock(ZonaRepository.class, CALLS_REAL_METHODS);

    @Test
    @DisplayName("Resuelve la zona entre todas las conocidas")
    void resolverShouldChooseAmongAllZones() {
        Zona sanJusto = givenZona("San Justo", "-34.670300", "-58.562800", 1500);
        Zona caba = givenZona("CABA", "-34.603700", "-58.381600", 1500);
        when(zonaRepository.findAll()).thenReturn(List.of(caba, sanJusto));

        Optional<Zona> result = zonaRepository.resolver(LATITUD, LONGITUD);

        assertThat(result).containsSame(sanJusto);
    }

    @Test
    @DisplayName("Obtiene la zona por id")
    void findByIdSiTieneShouldReturnZoneById() {
        Zona zona = givenZona("San Justo", "-34.670300", "-58.562800", 1500);
        when(zonaRepository.findById(zona.getId())).thenReturn(Optional.of(zona));

        assertThat(zonaRepository.findByIdSiTiene(zona.getId())).containsSame(zona);
    }

    @Test
    @DisplayName("Sin zona asignada no consulta nada")
    void findByIdSiTieneShouldReturnEmptyForNullId() {
        assertThat(zonaRepository.findByIdSiTiene(null)).isEmpty();

        verify(zonaRepository).findByIdSiTiene(null);
    }

    // --- arrange ---
    private Zona givenZona(String nombre, String latitud, String longitud, int radioMetros) {
        return Zona.builder().id(ZonaId.nuevo()).nombre(nombre).latitud(new BigDecimal(latitud))
                .longitud(new BigDecimal(longitud)).radioMetros(radioMetros).build();
    }
}
