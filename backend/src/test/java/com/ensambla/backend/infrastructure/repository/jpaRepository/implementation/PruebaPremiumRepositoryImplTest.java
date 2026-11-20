package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.infrastructure.repository.entity.PruebaPremiumEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IPruebaPremiumJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.PruebaPremiumRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PruebaPremiumRepositoryImplTest {

    private final IPruebaPremiumJpaRepository pruebaPremiumJpaRepository = mock(IPruebaPremiumJpaRepository.class);
    private final PruebaPremiumRepositoryImpl pruebaPremiumRepository = new PruebaPremiumRepositoryImpl(
            pruebaPremiumJpaRepository, new PruebaPremiumRepositoryMapper());

    private Optional<PruebaPremium> encontrada;
    private PruebaPremium guardada;
    private List<NegocioId> vencidos;

    @Test
    @DisplayName("Recupera la prueba exclusivamente por el negocio solicitado")
    void recuperaLaPruebaExclusivamentePorElNegocioSolicitado() {
        dadoQueElNegocioTieneUnaPrueba();

        cuandoSeBuscaLaPruebaDelNegocio();

        entoncesSeEncuentraLaPruebaDeSieteDias();
    }

    @Test
    @DisplayName("Un negocio sin historial devuelve un resultado vacío")
    void unNegocioSinHistorialDevuelveUnResultadoVacio() {
        dadoQueElNegocioNoTienePrueba();

        cuandoSeBuscaLaPruebaDelNegocio();

        entoncesNoSeEncuentraNingunaPrueba();
    }

    @Test
    @DisplayName("Guarda el período y su contador sin reemplazarlos por fechas actuales")
    void guardaElPeriodoYSuContadorSinReemplazarlosPorFechasActuales() {
        dadoQueGuardarDevuelveLaPrueba();

        cuandoSeGuardaLaPrueba();

        entoncesSeConservanElPeriodoYElContador();
    }

    @Test
    @DisplayName("Delega la búsqueda de candidatos vencidos al instante solicitado")
    void delegaLaBusquedaDeCandidatosVencidosAlInstanteSolicitado() {
        when(pruebaPremiumJpaRepository.findNegociosVencidos(INSTANTE)).thenReturn(List.of(NEGOCIO_ID.valor()));

        cuandoSeBuscanLosNegociosVencidos();

        entoncesSeListaElNegocioVencido();
    }

    // --- dado ---
    private void dadoQueElNegocioTieneUnaPrueba() {
        when(pruebaPremiumJpaRepository.findById(NEGOCIO_ID.valor())).thenReturn(Optional.of(unaEntidad()));
    }

    private void dadoQueElNegocioNoTienePrueba() {
        when(pruebaPremiumJpaRepository.findById(NEGOCIO_ID.valor())).thenReturn(Optional.empty());
    }

    private void dadoQueGuardarDevuelveLaPrueba() {
        when(pruebaPremiumJpaRepository.saveAndFlush(any())).thenReturn(unaEntidad());
    }

    private PruebaPremiumEntity unaEntidad() {
        return PruebaPremiumEntity.builder()
                .negocioId(NEGOCIO_ID.valor())
                .inicioEn(INSTANTE)
                .finEn(INSTANTE.plusDays(7))
                .paginasIa(4)
                .build();
    }

    // --- cuando ---
    private void cuandoSeBuscaLaPruebaDelNegocio() {
        encontrada = pruebaPremiumRepository.findByNegocioId(NEGOCIO_ID);
    }

    private void cuandoSeGuardaLaPrueba() {
        guardada = pruebaPremiumRepository.save(new PruebaPremium(NEGOCIO_ID, INSTANTE, INSTANTE.plusDays(7), 4));
    }

    private void cuandoSeBuscanLosNegociosVencidos() {
        vencidos = pruebaPremiumRepository.findNegociosVencidos(INSTANTE);
    }

    // --- entonces ---
    private void entoncesSeEncuentraLaPruebaDeSieteDias() {
        assertThat(encontrada).contains(new PruebaPremium(NEGOCIO_ID, INSTANTE, INSTANTE.plusDays(7), 4));
        verify(pruebaPremiumJpaRepository).findById(NEGOCIO_ID.valor());
    }

    private void entoncesNoSeEncuentraNingunaPrueba() {
        assertThat(encontrada).isEmpty();
    }

    private void entoncesSeConservanElPeriodoYElContador() {
        assertThat(guardada.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardada.inicioEn()).isEqualTo(INSTANTE);
        assertThat(guardada.finEn()).isEqualTo(INSTANTE.plusDays(7));
        assertThat(guardada.paginasIa()).isEqualTo(4);
    }

    private void entoncesSeListaElNegocioVencido() {
        assertThat(vencidos).containsExactly(NEGOCIO_ID);
    }
}
