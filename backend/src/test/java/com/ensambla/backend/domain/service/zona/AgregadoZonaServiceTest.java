package com.ensambla.backend.domain.service.zona;

import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgregadoZonaServiceTest {

    private static final UUID ZONA_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");

    private final AgregadoZonaRepository agregadoZonaRepository = mock(AgregadoZonaRepository.class);
    private final AgregadoZonaService agregadoZonaService = new AgregadoZonaService(agregadoZonaRepository);

    @Test
    @DisplayName("Analizar arma el nombre, la cantidad de comercios y los productos agregados de la zona")
    void analizarShouldBuildZoneAnalysis() {
        AgregadoZonaProducto yerba = new AgregadoZonaProducto("Yerba 1kg", 8, 6, new BigDecimal("2800.00"),
                new BigDecimal("3100.00"), new BigDecimal("3400.00"), new BigDecimal("12.50"));
        when(agregadoZonaRepository.contarComerciosQueComparten(ZONA_ID)).thenReturn(10L);
        when(agregadoZonaRepository.findByZonaId(ZONA_ID)).thenReturn(List.of(yerba));

        AnalisisZona result = agregadoZonaService.analizar(
                Zona.builder().id(ZONA_ID).nombre("San Justo - UNLaM").build());

        assertThat(result).isEqualTo(new AnalisisZona("San Justo - UNLaM", 10, List.of(yerba)));
    }

    @Test
    @DisplayName("Sin zona el análisis queda vacío")
    void sinZonaShouldBeEmpty() {
        assertThat(AnalisisZona.sinZona()).isEqualTo(new AnalisisZona("", 0, List.of()));
    }
}
