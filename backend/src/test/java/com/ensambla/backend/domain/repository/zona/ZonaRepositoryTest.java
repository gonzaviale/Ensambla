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

    private Zona sanJusto;
    private Optional<Zona> resuelta;

    @Test
    @DisplayName("Resuelve la zona entre todas las conocidas")
    void resuelveLaZonaEntreTodasLasConocidas() {
        dadoQueSeConocenSanJustoYCaba();

        cuandoSeResuelveLaZonaDelPunto();

        entoncesSeResuelveSanJusto();
    }

    @Test
    @DisplayName("Obtiene la zona por id")
    void obtieneLaZonaPorId() {
        dadoQueSeConoceSanJusto();

        cuandoSeBuscaLaZonaPorId(sanJusto.getId());

        entoncesSeResuelveSanJusto();
    }

    @Test
    @DisplayName("Sin zona asignada no consulta nada")
    void sinZonaAsignadaNoConsultaNada() {
        cuandoSeBuscaLaZonaPorId(null);

        entoncesNoSeResuelveNingunaZona();
    }

    // --- dado ---

    private void dadoQueSeConocenSanJustoYCaba() {
        sanJusto = unaZona("San Justo", "-34.670300", "-58.562800", 1500);
        Zona caba = unaZona("CABA", "-34.603700", "-58.381600", 1500);
        when(zonaRepository.findAll()).thenReturn(List.of(caba, sanJusto));
    }

    private void dadoQueSeConoceSanJusto() {
        sanJusto = unaZona("San Justo", "-34.670300", "-58.562800", 1500);
        when(zonaRepository.findById(sanJusto.getId())).thenReturn(Optional.of(sanJusto));
    }

    // --- cuando ---

    private void cuandoSeResuelveLaZonaDelPunto() {
        resuelta = zonaRepository.resolver(LATITUD, LONGITUD);
    }

    private void cuandoSeBuscaLaZonaPorId(ZonaId id) {
        resuelta = zonaRepository.findByIdSiTiene(id);
    }

    // --- entonces ---

    private void entoncesSeResuelveSanJusto() {
        assertThat(resuelta).containsSame(sanJusto);
    }

    private void entoncesNoSeResuelveNingunaZona() {
        assertThat(resuelta).isEmpty();
        verify(zonaRepository).findByIdSiTiene(null);
    }

    // --- datos ---

    private Zona unaZona(String nombre, String latitud, String longitud, int radioMetros) {
        return Zona.builder().id(ZonaId.nuevo()).nombre(nombre).latitud(new BigDecimal(latitud))
                .longitud(new BigDecimal(longitud)).radioMetros(radioMetros).build();
    }
}
