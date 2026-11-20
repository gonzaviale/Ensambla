package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.useCase.caja.CobroQrItem;
import com.ensambla.backend.domain.useCase.caja.cancelarCobroQr.CancelarCobroQr;
import com.ensambla.backend.domain.useCase.caja.cancelarCobroQr.CancelarCobroQrRequest;
import com.ensambla.backend.domain.useCase.caja.generarCobroQr.GenerarCobroQr;
import com.ensambla.backend.domain.useCase.caja.generarCobroQr.GenerarCobroQrRequest;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.caja.CobroQrPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static com.ensambla.backend.support.CobrosQrDePrueba.AHORA;
import static com.ensambla.backend.support.CobrosQrDePrueba.COBRO_QR_ID;
import static com.ensambla.backend.support.CobrosQrDePrueba.IMPORTE;
import static com.ensambla.backend.support.CobrosQrDePrueba.unCobroQrCancelado;
import static com.ensambla.backend.support.CobrosQrDePrueba.unCobroQrPendiente;
import static com.ensambla.backend.support.UsuarioTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CobroQrControllerTest {

    @Mock
    private GenerarCobroQr generarCobroQr;
    @Mock
    private CancelarCobroQr cancelarCobroQr;

    private MockMvc mockMvc;
    private ResultActions respuesta;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcDePrueba.standalone(new CobroQrController(generarCobroQr, cancelarCobroQr,
                new CobroQrPresentationMapper())).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    @DisplayName("Generar el cobro responde con el QR pendiente y pasa el usuario de la sesión")
    void generarElCobroRespondeConElQrPendienteYPasaElUsuarioDeLaSesion() throws Exception {
        dadoQueSeGeneraUnCobroPendiente();

        cuandoSeGeneraElCobro(cuerpoDeGenerar());

        entoncesSeResponde200ConElCobro("PENDIENTE");
        entoncesElCasoDeUsoDeGenerarRecibeElUsuarioElIdYElImporte();
    }

    @Test
    @DisplayName("Un importe en cero se rechaza sin llegar al caso de uso")
    void unImporteEnCeroSeRechazaSinLlegarAlCasoDeUso() throws Exception {
        cuandoSeGeneraElCobro("{\"id\":\"" + COBRO_QR_ID + "\",\"importe\":0}");

        entoncesSeRechazaSinLlegarAlCasoDeUsoDeGenerar();
    }

    @Test
    @DisplayName("Un cobro sin id se rechaza sin llegar al caso de uso")
    void unCobroSinIdSeRechazaSinLlegarAlCasoDeUso() throws Exception {
        cuandoSeGeneraElCobro("{\"importe\":2500.50}");

        entoncesSeRechazaSinLlegarAlCasoDeUsoDeGenerar();
    }

    @Test
    @DisplayName("Cancelar el cobro responde con el cobro cancelado y pasa su id")
    void cancelarElCobroRespondeConElCobroCanceladoYPasaSuId() throws Exception {
        dadoQueSeCancelaElCobro();

        cuandoSeCancelaElCobro(COBRO_QR_ID.toString());

        entoncesSeResponde200ConElCobro("CANCELADO");
        entoncesElCasoDeUsoDeCancelarRecibeElUsuarioYElId();
    }

    @Test
    @DisplayName("Un id que no es un UUID se rechaza sin llegar al caso de uso de cancelar")
    void unIdQueNoEsUnUuidSeRechazaSinLlegarAlCasoDeUsoDeCancelar() throws Exception {
        cuandoSeCancelaElCobro("id-invalido");

        entoncesSeRechazaSinLlegarAlCasoDeUsoDeCancelar();
    }

    @Test
    @DisplayName("Cancelar un cobro de otro negocio responde 404")
    void cancelarUnCobroDeOtroNegocioResponde404() throws Exception {
        dadoUnCobroDeOtroNegocio();

        cuandoSeCancelaElCobro(COBRO_QR_ID.toString());

        entoncesRespondeNoEncontrado();
    }

    // --- dado ---
    private void dadoQueSeGeneraUnCobroPendiente() {
        when(generarCobroQr.execute(any())).thenReturn(CobroQrItem.de(unCobroQrPendiente(), AHORA));
    }

    private void dadoQueSeCancelaElCobro() {
        when(cancelarCobroQr.execute(any())).thenReturn(CobroQrItem.de(unCobroQrCancelado(), AHORA));
    }

    private void dadoUnCobroDeOtroNegocio() {
        when(cancelarCobroQr.execute(any())).thenThrow(new ResourceNotFoundException("Cobro con QR no encontrado"));
    }

    // --- cuando ---
    private void cuandoSeGeneraElCobro(String cuerpo) throws Exception {
        respuesta = mockMvc.perform(post("/api/caja/cobros-qr").principal(sesion())
                .contentType("application/json").content(cuerpo));
    }

    private void cuandoSeCancelaElCobro(String id) throws Exception {
        respuesta = mockMvc.perform(post("/api/caja/cobros-qr/" + id + "/cancelar").principal(sesion()));
    }

    // --- entonces ---
    private void entoncesSeResponde200ConElCobro(String estado) throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(COBRO_QR_ID.toString()))
                .andExpect(jsonPath("$.importe").value(2500.50))
                .andExpect(jsonPath("$.alias").value("mi.alias.mp"))
                .andExpect(jsonPath("$.titular").value("Ana Pérez"))
                .andExpect(jsonPath("$.estado").value(estado))
                .andExpect(jsonPath("$.venceEn").exists());
    }

    private void entoncesElCasoDeUsoDeGenerarRecibeElUsuarioElIdYElImporte() {
        ArgumentCaptor<GenerarCobroQrRequest> pedido = ArgumentCaptor.forClass(GenerarCobroQrRequest.class);
        verify(generarCobroQr).execute(pedido.capture());
        assertThat(pedido.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(pedido.getValue().id()).isEqualTo(COBRO_QR_ID);
        assertThat(pedido.getValue().importe()).isEqualByComparingTo(IMPORTE);
    }

    private void entoncesElCasoDeUsoDeCancelarRecibeElUsuarioYElId() {
        ArgumentCaptor<CancelarCobroQrRequest> pedido = ArgumentCaptor.forClass(CancelarCobroQrRequest.class);
        verify(cancelarCobroQr).execute(pedido.capture());
        assertThat(pedido.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(pedido.getValue().id()).isEqualTo(COBRO_QR_ID);
    }

    private void entoncesSeRechazaSinLlegarAlCasoDeUsoDeGenerar() throws Exception {
        respuesta.andExpect(status().isBadRequest());
        verifyNoInteractions(generarCobroQr);
    }

    private void entoncesSeRechazaSinLlegarAlCasoDeUsoDeCancelar() throws Exception {
        respuesta.andExpect(status().isBadRequest());
        verifyNoInteractions(cancelarCobroQr);
    }

    private void entoncesRespondeNoEncontrado() throws Exception {
        respuesta.andExpect(status().isNotFound());
    }

    // --- datos ---
    private UsernamePasswordAuthenticationToken sesion() {
        return UsernamePasswordAuthenticationToken.authenticated(USUARIO_ID.toString(), null, List.of());
    }

    private String cuerpoDeGenerar() {
        return "{\"id\":\"" + COBRO_QR_ID + "\",\"importe\":2500.50}";
    }
}
