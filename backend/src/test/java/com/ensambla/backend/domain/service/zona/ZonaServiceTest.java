package com.ensambla.backend.domain.service.zona;

import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ZonaServiceTest {

    private static final BigDecimal LATITUD = new BigDecimal("-34.670500");
    private static final BigDecimal LONGITUD = new BigDecimal("-58.562500");

    private final ZonaRepository zonaRepository = mock(ZonaRepository.class);
    private final ZonaService zonaService = new ZonaService(zonaRepository);

    @Test
    @DisplayName("Resuelve la zona cuyo radio contiene el punto")
    void resolverShouldReturnZoneContainingPoint() {
        Zona sanJusto = givenZona("San Justo", "-34.670300", "-58.562800", 1500);
        Zona caba = givenZona("CABA", "-34.603700", "-58.381600", 1500);
        when(zonaRepository.findAll()).thenReturn(List.of(caba, sanJusto));

        Optional<Zona> result = zonaService.resolver(LATITUD, LONGITUD);

        assertThat(result).containsSame(sanJusto);
    }

    @Test
    @DisplayName("Elige la zona de centro más cercano cuando se superponen")
    void resolverShouldPickNearestWhenZonesOverlap() {
        Zona lejana = givenZona("Lejana", "-34.675000", "-58.562800", 2000);
        Zona cercana = givenZona("Cercana", "-34.670300", "-58.562800", 2000);
        when(zonaRepository.findAll()).thenReturn(List.of(lejana, cercana));

        Optional<Zona> result = zonaService.resolver(LATITUD, LONGITUD);

        assertThat(result).containsSame(cercana);
    }

    @Test
    @DisplayName("No resuelve zona si ninguna contiene el punto")
    void resolverShouldReturnEmptyWhenNoZoneContainsPoint() {
        when(zonaRepository.findAll()).thenReturn(List.of(givenZona("CABA", "-34.603700", "-58.381600", 1500)));

        assertThat(zonaService.resolver(LATITUD, LONGITUD)).isEmpty();
    }

    @Test
    @DisplayName("Obtiene la zona por id")
    void obtenerShouldReturnZoneById() {
        Zona zona = givenZona("San Justo", "-34.670300", "-58.562800", 1500);
        when(zonaRepository.findById(zona.getId())).thenReturn(Optional.of(zona));

        assertThat(zonaService.obtener(zona.getId())).containsSame(zona);
    }

    @Test
    @DisplayName("Sin zona asignada no consulta el repositorio")
    void obtenerShouldReturnEmptyForNullId() {
        assertThat(zonaService.obtener(null)).isEmpty();

        verifyNoInteractions(zonaRepository);
    }

    @Test
    @DisplayName("Busca por texto la primera zona cuyo nombre o localidad coincide")
    void buscarPorTextoShouldReturnFirstMatch() {
        Zona sanJusto = Zona.builder().id(UUID.randomUUID()).nombre("San Justo - UNLaM").localidad("San Justo").build();
        Zona caba = Zona.builder().id(UUID.randomUUID()).nombre("Villa Crespo").localidad("CABA").build();
        when(zonaRepository.findAll()).thenReturn(List.of(caba, sanJusto));

        assertThat(zonaService.buscarPorTexto("san justo")).containsSame(sanJusto);
        assertThat(zonaService.buscarPorTexto("Palermo")).isEmpty();
    }

    // --- arrange ---
    private Zona givenZona(String nombre, String latitud, String longitud, int radioMetros) {
        return Zona.builder().id(UUID.randomUUID()).nombre(nombre).latitud(new BigDecimal(latitud))
                .longitud(new BigDecimal(longitud)).radioMetros(radioMetros).build();
    }
}
