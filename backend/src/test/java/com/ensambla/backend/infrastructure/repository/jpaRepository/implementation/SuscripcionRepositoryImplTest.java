package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ISuscripcionJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.SuscripcionRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PlanTestData.suscripcion;
import static com.ensambla.backend.support.PlanTestData.suscripcionEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuscripcionRepositoryImplTest {

    private final ISuscripcionJpaRepository suscripcionJpaRepository = mock(ISuscripcionJpaRepository.class);
    private final SuscripcionRepositoryImpl suscripcionRepository = new SuscripcionRepositoryImpl(
            suscripcionJpaRepository, new SuscripcionRepositoryMapper());

    private Optional<Suscripcion> encontrada;
    private Suscripcion guardada;

    @Test
    @DisplayName("Recupera y mapea suscripcion desde persistencia")
    void recuperaYMapeaLaSuscripcionDesdePersistencia() {
        dadoQueElNegocioTieneUnaSuscripcion();

        cuandoSeBuscaLaSuscripcionDelNegocio();

        entoncesSeEncuentraLaSuscripcionConTodosSusCampos();
    }

    @Test
    @DisplayName("Conserva el resultado vacío cuando no existe suscripcion")
    void conservaElResultadoVacioCuandoNoExisteSuscripcion() {
        dadoQueElNegocioNoTieneSuscripcion();

        cuandoSeBuscaLaSuscripcionDelNegocio();

        entoncesNoSeEncuentraNingunaSuscripcion();
    }

    @Test
    @DisplayName("Guarda todos los campos y devuelve la suscripción persistida")
    void guardaTodosLosCamposYDevuelveLaSuscripcionPersistida() {
        dadoQueGuardarDevuelveLaEntidad();

        cuandoSeGuardaLaSuscripcion();

        entoncesSeDevuelveLaSuscripcionConTodosSusCampos(guardada);
        entoncesSeGuardoLaEntidadConTodosSusCampos();
    }

    // --- dado ---
    private void dadoQueElNegocioTieneUnaSuscripcion() {
        when(suscripcionJpaRepository.findByNegocioId(NEGOCIO_ID.valor())).thenReturn(Optional.of(suscripcionEntity()));
    }

    private void dadoQueElNegocioNoTieneSuscripcion() {
        when(suscripcionJpaRepository.findByNegocioId(NEGOCIO_ID.valor())).thenReturn(Optional.empty());
    }

    private void dadoQueGuardarDevuelveLaEntidad() {
        when(suscripcionJpaRepository.saveAndFlush(refEq(suscripcionEntity()))).thenReturn(suscripcionEntity());
    }

    // --- cuando ---
    private void cuandoSeBuscaLaSuscripcionDelNegocio() {
        encontrada = suscripcionRepository.findByNegocioId(NEGOCIO_ID);
    }

    private void cuandoSeGuardaLaSuscripcion() {
        guardada = suscripcionRepository.save(suscripcion());
    }

    // --- entonces ---
    private void entoncesSeEncuentraLaSuscripcionConTodosSusCampos() {
        assertThat(encontrada).isPresent();
        entoncesSeDevuelveLaSuscripcionConTodosSusCampos(encontrada.orElseThrow());
        verify(suscripcionJpaRepository).findByNegocioId(NEGOCIO_ID.valor());
    }

    private void entoncesNoSeEncuentraNingunaSuscripcion() {
        assertThat(encontrada).isEmpty();
    }

    private void entoncesSeGuardoLaEntidadConTodosSusCampos() {
        verify(suscripcionJpaRepository).saveAndFlush(refEq(suscripcionEntity()));
    }

    private void entoncesSeDevuelveLaSuscripcionConTodosSusCampos(Suscripcion result) {
        Suscripcion esperado = suscripcion();
        assertThat(result.getId()).isEqualTo(esperado.getId());
        assertThat(result.getNegocioId()).isEqualTo(esperado.getNegocioId());
        assertThat(result.getPlanId()).isEqualTo(esperado.getPlanId());
        assertThat(result.getEstado()).isEqualTo(esperado.getEstado());
        assertThat(result.getInicioEn()).isEqualTo(esperado.getInicioEn());
        assertThat(result.getFinEn()).isEqualTo(esperado.getFinEn());
        assertThat(result.getCreadoEn()).isEqualTo(esperado.getCreadoEn());
        assertThat(result.getActualizadoEn()).isEqualTo(esperado.getActualizadoEn());
    }
}
