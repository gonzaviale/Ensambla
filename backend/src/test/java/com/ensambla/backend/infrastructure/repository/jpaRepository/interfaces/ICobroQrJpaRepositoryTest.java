package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.caja.ConfirmacionDeCobro;
import com.ensambla.backend.domain.model.caja.EstadoCobroQr;
import com.ensambla.backend.infrastructure.repository.entity.CobroQrEntity;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ICobroQrJpaRepositoryTest extends PostgresDatosTestSupport {

    @Autowired
    private ICobroQrJpaRepository cobros;

    private UUID negocioA;
    private UUID negocioB;
    private CobroQrEntity cobro;
    private Optional<CobroQrEntity> encontrado;
    private Throwable rechazo;

    @BeforeEach
    void dadoDosNegocios() {
        negocioA = givenNegocio("a@example.com").getId();
        negocioB = givenNegocio("b@example.com").getId();
    }

    @Test
    @DisplayName("Un cobro guardado se encuentra por su id dentro de su negocio con todos sus datos")
    void unCobroGuardadoSeEncuentraPorSuIdDentroDeSuNegocioConTodosSusDatos() {
        dadoUnCobroPendienteGuardadoEnElNegocioA();

        cuandoSeBuscaElCobroEnElNegocio(negocioA);

        entoncesSeEncuentraElCobroPendienteConSusDatos();
    }

    @Test
    @DisplayName("Un negocio ajeno no encuentra el cobro")
    void unNegocioAjenoNoEncuentraElCobro() {
        dadoUnCobroPendienteGuardadoEnElNegocioA();

        cuandoSeBuscaElCobroEnElNegocio(negocioB);

        entoncesNoSeEncuentraNada();
    }

    @Test
    @DisplayName("Un cobro confirmado guarda su fecha y su marca manual")
    void unCobroConfirmadoGuardaSuFechaYSuMarcaManual() {
        dadoUnCobroPendienteGuardadoEnElNegocioA();

        cuandoSeConfirmaYSeGuardaElCobro();

        entoncesElCobroGuardadoEstaCobradoPorConfirmacionManual();
    }

    @Test
    @DisplayName("No se guarda un importe en cero")
    void noSeGuardaUnImporteEnCero() {
        dadoUnCobroDelNegocioA();

        cuandoSeIntentaGuardarConImporte(BigDecimal.ZERO);

        entoncesLaBaseRechazaPorIntegridad();
    }

    @Test
    @DisplayName("No se guarda un cobro cobrado sin su confirmación")
    void noSeGuardaUnCobroCobradoSinSuConfirmacion() {
        dadoUnCobroDelNegocioA();

        cuandoSeIntentaGuardarConEstado(EstadoCobroQr.COBRADO);

        entoncesLaBaseRechazaPorIntegridad();
    }

    @Test
    @DisplayName("No se guarda un cobro cancelado con una confirmación")
    void noSeGuardaUnCobroCanceladoConUnaConfirmacion() {
        dadoUnCobroDelNegocioA();
        dadoQueTieneUnaConfirmacionManual();

        cuandoSeIntentaGuardarConEstado(EstadoCobroQr.CANCELADO);

        entoncesLaBaseRechazaPorIntegridad();
    }

    @Test
    @DisplayName("Se guarda el vencimiento conciliado por Mercado Pago sin una confirmación")
    void seGuardaElVencimientoConciliadoPorMercadoPagoSinUnaConfirmacion() {
        dadoUnCobroDelNegocioA();

        cuandoSeGuardaConEstado(EstadoCobroQr.VENCIDO);

        entoncesSeGuardaSinConfirmacion(EstadoCobroQr.VENCIDO);
    }

    // --- dado ---
    private void dadoUnCobroDelNegocioA() {
        cobro = CobroQrEntity.builder()
                .id(UUID.randomUUID())
                .negocioId(negocioA)
                .alias("mi.alias.mp")
                .titular("Ana Pérez")
                .importe(new BigDecimal("2500.50"))
                .estado(EstadoCobroQr.PENDIENTE)
                .creadoEn(AHORA)
                .venceEn(AHORA.plusMinutes(15))
                .actualizadoEn(AHORA)
                .build();
    }

    private void dadoUnCobroPendienteGuardadoEnElNegocioA() {
        dadoUnCobroDelNegocioA();
        cobros.saveAndFlush(cobro);
    }

    private void dadoQueTieneUnaConfirmacionManual() {
        cobro.setConfirmadoEn(AHORA.plusMinutes(5));
        cobro.setConfirmacion(ConfirmacionDeCobro.MANUAL);
    }

    // --- cuando ---
    private void cuandoSeBuscaElCobroEnElNegocio(UUID negocioId) {
        encontrado = cobros.findByIdAndNegocioId(cobro.getId(), negocioId);
    }

    private void cuandoSeConfirmaYSeGuardaElCobro() {
        dadoQueTieneUnaConfirmacionManual();
        cobro.setEstado(EstadoCobroQr.COBRADO);
        cobros.saveAndFlush(cobro);
        encontrado = cobros.findByIdAndNegocioId(cobro.getId(), negocioA);
    }

    private void cuandoSeIntentaGuardarConImporte(BigDecimal importe) {
        cobro.setImporte(importe);
        rechazo = catchThrowable(() -> cobros.saveAndFlush(cobro));
    }

    private void cuandoSeIntentaGuardarConEstado(EstadoCobroQr estado) {
        cobro.setEstado(estado);
        rechazo = catchThrowable(() -> cobros.saveAndFlush(cobro));
    }

    private void cuandoSeGuardaConEstado(EstadoCobroQr estado) {
        cobro.setEstado(estado);
        cobros.saveAndFlush(cobro);
        encontrado = cobros.findByIdAndNegocioId(cobro.getId(), negocioA);
    }

    // --- entonces ---
    private void entoncesSeEncuentraElCobroPendienteConSusDatos() {
        assertThat(encontrado).hasValueSatisfying(guardado -> {
            assertThat(guardado.getAlias()).isEqualTo("mi.alias.mp");
            assertThat(guardado.getTitular()).isEqualTo("Ana Pérez");
            assertThat(guardado.getImporte()).isEqualByComparingTo("2500.50");
            assertThat(guardado.getEstado()).isEqualTo(EstadoCobroQr.PENDIENTE);
            assertThat(guardado.getVenceEn()).isEqualTo(AHORA.plusMinutes(15));
            assertThat(guardado.getConfirmadoEn()).isNull();
        });
    }

    private void entoncesNoSeEncuentraNada() {
        assertThat(encontrado).isEmpty();
    }

    private void entoncesElCobroGuardadoEstaCobradoPorConfirmacionManual() {
        assertThat(encontrado).hasValueSatisfying(guardado -> {
            assertThat(guardado.getEstado()).isEqualTo(EstadoCobroQr.COBRADO);
            assertThat(guardado.getConfirmacion()).isEqualTo(ConfirmacionDeCobro.MANUAL);
            assertThat(guardado.getConfirmadoEn()).isEqualTo(AHORA.plusMinutes(5));
        });
    }

    private void entoncesSeGuardaSinConfirmacion(EstadoCobroQr estado) {
        assertThat(encontrado).hasValueSatisfying(guardado -> {
            assertThat(guardado.getEstado()).isEqualTo(estado);
            assertThat(guardado.getConfirmadoEn()).isNull();
            assertThat(guardado.getConfirmacion()).isNull();
        });
    }

    private void entoncesLaBaseRechazaPorIntegridad() {
        assertThat(rechazo).isInstanceOf(DataIntegrityViolationException.class);
    }
}
