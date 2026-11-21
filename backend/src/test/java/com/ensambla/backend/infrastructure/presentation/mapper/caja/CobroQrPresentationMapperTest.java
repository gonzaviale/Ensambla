package com.ensambla.backend.infrastructure.presentation.mapper.caja;

import com.ensambla.backend.domain.useCase.caja.CobroQrItem;
import com.ensambla.backend.domain.useCase.caja.cancelarCobroQr.CancelarCobroQrRequest;
import com.ensambla.backend.domain.useCase.caja.generarCobroQr.GenerarCobroQrRequest;
import com.ensambla.backend.infrastructure.presentation.dto.caja.CobroQrWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.caja.GenerarCobroQrWebRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.CobrosQrDePrueba.AHORA;
import static com.ensambla.backend.support.CobrosQrDePrueba.COBRO_QR_ID;
import static com.ensambla.backend.support.CobrosQrDePrueba.IMPORTE;
import static com.ensambla.backend.support.CobrosQrDePrueba.elMomentoDelVencimiento;
import static com.ensambla.backend.support.CobrosQrDePrueba.unCobroQrPendiente;
import static com.ensambla.backend.support.UsuarioTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class CobroQrPresentationMapperTest {

    private final CobroQrPresentationMapper cobroQrPresentationMapper = new CobroQrPresentationMapper();

    private GenerarCobroQrRequest pedidoDeGenerar;
    private CancelarCobroQrRequest pedidoDeCancelar;
    private CobroQrWebResponse respuesta;

    @Test
    @DisplayName("Arma el pedido de generar con el usuario de la sesión, el id y el importe")
    void armaElPedidoDeGenerarConElUsuarioDeLaSesionElIdYElImporte() {
        cuandoSeArmaElPedidoDeGenerar();

        entoncesElPedidoDeGenerarTieneUsuarioIdEImporte();
    }

    @Test
    @DisplayName("Arma el pedido de cancelar con el usuario de la sesión y el id")
    void armaElPedidoDeCancelarConElUsuarioDeLaSesionYElId() {
        cuandoSeArmaElPedidoDeCancelar();

        entoncesElPedidoDeCancelarTieneUsuarioEId();
    }

    @Test
    @DisplayName("Responde con todos los datos del cobro")
    void respondeConTodosLosDatosDelCobro() {
        cuandoSeArmaLaRespuestaDeUnCobroPendiente();

        entoncesLaRespuestaTieneTodosLosDatosDelCobro();
    }

    // --- cuando ---
    private void cuandoSeArmaElPedidoDeGenerar() {
        pedidoDeGenerar = cobroQrPresentationMapper.toGenerarRequest(USUARIO_ID,
                new GenerarCobroQrWebRequest(COBRO_QR_ID, IMPORTE));
    }

    private void cuandoSeArmaElPedidoDeCancelar() {
        pedidoDeCancelar = cobroQrPresentationMapper.toCancelarRequest(USUARIO_ID, COBRO_QR_ID);
    }

    private void cuandoSeArmaLaRespuestaDeUnCobroPendiente() {
        respuesta = cobroQrPresentationMapper.toResponse(CobroQrItem.de(unCobroQrPendiente(), AHORA));
    }

    // --- entonces ---
    private void entoncesElPedidoDeGenerarTieneUsuarioIdEImporte() {
        assertThat(pedidoDeGenerar.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(pedidoDeGenerar.id()).isEqualTo(COBRO_QR_ID);
        assertThat(pedidoDeGenerar.importe()).isEqualByComparingTo(IMPORTE);
    }

    private void entoncesElPedidoDeCancelarTieneUsuarioEId() {
        assertThat(pedidoDeCancelar.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(pedidoDeCancelar.id()).isEqualTo(COBRO_QR_ID);
    }

    private void entoncesLaRespuestaTieneTodosLosDatosDelCobro() {
        assertThat(respuesta.id()).isEqualTo(COBRO_QR_ID);
        assertThat(respuesta.importe()).isEqualByComparingTo(IMPORTE);
        assertThat(respuesta.alias()).isEqualTo("mi.alias.mp");
        assertThat(respuesta.titular()).isEqualTo("Ana Pérez");
        assertThat(respuesta.estado()).isEqualTo("PENDIENTE");
        assertThat(respuesta.venceEn()).isEqualTo(elMomentoDelVencimiento());
    }
}
