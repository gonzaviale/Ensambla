package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.caja.CobroQr;
import com.ensambla.backend.infrastructure.repository.entity.CobroQrEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICobroQrJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.CobroQrRepositoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.ensambla.backend.support.CobrosQrDePrueba.COBRO_QR_ID;
import static com.ensambla.backend.support.CobrosQrDePrueba.unCobroQrCobrado;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CobroQrRepositoryImplTest {

    @Mock
    private ICobroQrJpaRepository jpa;

    private final CobroQrRepositoryMapper mapper = new CobroQrRepositoryMapper();
    private CobroQrRepositoryImpl repositorio;
    private final CobroQr cobro = unCobroQrCobrado();

    private CobroQr guardado;
    private Optional<CobroQr> encontrado;

    @BeforeEach
    void dadoUnRepositorioDeCobrosConQr() {
        repositorio = new CobroQrRepositoryImpl(jpa, mapper);
    }

    @Test
    @DisplayName("Guarda el cobro y lo devuelve como lo dejó la base")
    void guardaElCobroYLoDevuelveComoLoDejoLaBase() {
        dadoQueGuardarDevuelveLaEntidadRecibida();

        cuandoSeGuardaElCobro();

        entoncesSeGuardoLaEntidadDelCobro();
        entoncesSeDevuelveElMismoCobro();
    }

    @Test
    @DisplayName("Busca el cobro sólo dentro del negocio de la sesión")
    void buscaElCobroSoloDentroDelNegocioDeLaSesion() {
        dadoUnCobroGuardadoEnLaBase();

        cuandoSeBuscaElCobroPorIdYNegocio();

        entoncesSeEncuentraElCobro();
    }

    @Test
    @DisplayName("Un cobro que la base no tiene se busca como ausente")
    void unCobroQueLaBaseNoTieneSeBuscaComoAusente() {
        dadoQueLaBaseNoTieneElCobro();

        cuandoSeBuscaElCobroPorIdYNegocio();

        entoncesNoSeEncuentraNada();
    }

    // --- dado ---
    private void dadoQueGuardarDevuelveLaEntidadRecibida() {
        when(jpa.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private void dadoUnCobroGuardadoEnLaBase() {
        when(jpa.findByIdAndNegocioId(COBRO_QR_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(Optional.of(mapper.toEntity(cobro)));
    }

    private void dadoQueLaBaseNoTieneElCobro() {
        when(jpa.findByIdAndNegocioId(COBRO_QR_ID.valor(), NEGOCIO_ID.valor())).thenReturn(Optional.empty());
    }

    // --- cuando ---
    private void cuandoSeGuardaElCobro() {
        guardado = repositorio.save(cobro);
    }

    private void cuandoSeBuscaElCobroPorIdYNegocio() {
        encontrado = repositorio.findByIdAndNegocioId(COBRO_QR_ID, NEGOCIO_ID);
    }

    // --- entonces ---
    private void entoncesSeGuardoLaEntidadDelCobro() {
        ArgumentCaptor<CobroQrEntity> captor = ArgumentCaptor.forClass(CobroQrEntity.class);
        verify(jpa).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(COBRO_QR_ID.valor());
        assertThat(captor.getValue().getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
    }

    private void entoncesSeDevuelveElMismoCobro() {
        assertThat(guardado).isEqualTo(cobro);
    }

    private void entoncesSeEncuentraElCobro() {
        assertThat(encontrado).contains(cobro);
    }

    private void entoncesNoSeEncuentraNada() {
        assertThat(encontrado).isEmpty();
    }
}
