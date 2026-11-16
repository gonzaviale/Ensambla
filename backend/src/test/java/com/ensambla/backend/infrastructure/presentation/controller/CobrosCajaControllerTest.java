package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.useCase.caja.CobroCajaItem;
import com.ensambla.backend.domain.useCase.caja.cerrarCaja.CerrarCaja;
import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVenta;
import com.ensambla.backend.domain.useCase.caja.consultarCaja.ConsultarCaja;
import com.ensambla.backend.domain.useCase.caja.listarVentas.ListarVentasCaja;
import com.ensambla.backend.domain.useCase.caja.listarVentas.ListarVentasCajaResponse;
import com.ensambla.backend.domain.useCase.caja.obtenerCierre.ObtenerCierre;
import com.ensambla.backend.domain.useCase.caja.registrarCobro.RegistrarCobro;
import com.ensambla.backend.domain.useCase.caja.registrarCobro.RegistrarCobroRequest;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.caja.CajaPresentationMapper;
import com.ensambla.backend.support.CajaTestData;
import com.ensambla.backend.support.CobroCajaTestData;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CobrosCajaControllerTest {

    @Mock
    private RegistrarCobro registrar;
    @Mock
    private ListarVentasCaja listar;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcDePrueba.standalone(new CajaController(mock(CobrarVenta.class), new CajaPresentationMapper(),
                mock(ConsultarCaja.class), mock(CerrarCaja.class), mock(ObtenerCierre.class), listar, registrar))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    @DisplayName("El pago posterior conserva al usuario de la sesión y devuelve su comprobante")
    void registrarShouldReturnReceiptAndMapAuthenticatedUser() throws Exception {
        when(registrar.execute(any())).thenReturn(CobroCajaItem.de(CobroCajaTestData.cobro()));

        mockMvc.perform(post("/api/caja/cobros").principal(authentication()).contentType("application/json").content(cuerpo()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.importe").value(600))
                .andExpect(jsonPath("$.clienteId").value(CajaTestData.CLIENTE.toString()))
                .andExpect(jsonPath("$.clienteNombre").value("Ana López"))
                .andExpect(jsonPath("$.aplicaciones[0].ventaId").value(CajaTestData.OPERACION.toString()))
                .andExpect(jsonPath("$.aplicaciones[0].importe").value(600))
                .andExpect(jsonPath("$.aplicaciones[0].pagos[0].medio").value("TRANSFERENCIA"))
                .andExpect(jsonPath("$.aplicaciones[0].pagos[0].importe").value(600));

        ArgumentCaptor<RegistrarCobroRequest> captor = ArgumentCaptor.forClass(RegistrarCobroRequest.class);
        verify(registrar).execute(captor.capture());
        assertThat(captor.getValue()).isEqualTo(CobroCajaTestData.request());
    }

    @Test
    @DisplayName("Lista saldos actuales, cobros e importes originales de las ventas")
    void listarShouldReturnCurrentAccountsAndOriginalPayments() throws Exception {
        when(listar.execute(any())).thenReturn(ListarVentasCajaResponse.de(
                List.of(CobroCajaTestData.venta()), List.of(CobroCajaTestData.cobro())));

        mockMvc.perform(get("/api/caja/ventas").principal(authentication())
                        .param("proyectoId", CajaTestData.PROYECTO.toString())
                        .param("inventarioId", CajaTestData.INVENTARIO.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ventas[0].saldo").value(1200))
                .andExpect(jsonPath("$.ventas[0].pagosIniciales[0].importe").value(800))
                .andExpect(jsonPath("$.cobros[0].importe").value(600));
    }

    @Test
    @DisplayName("Rechaza un pago sin aplicaciones antes de llegar al caso de uso")
    void registrarShouldRejectEmptyApplications() throws Exception {
        String cuerpo = "{\"id\":\"" + CobroCajaTestData.COBRO + "\",\"proyectoId\":\"" + CajaTestData.PROYECTO
                + "\",\"inventarioId\":\"" + CajaTestData.INVENTARIO + "\",\"aplicaciones\":[]}";

        mockMvc.perform(post("/api/caja/cobros").contentType("application/json").content(cuerpo))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(registrar);
    }

    @Test
    @DisplayName("Un identificador inválido no llega a consultar las cuentas")
    void listarShouldRejectInvalidInventoryId() throws Exception {
        mockMvc.perform(get("/api/caja/ventas").param("proyectoId", CajaTestData.PROYECTO.toString())
                        .param("inventarioId", "invalido"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(listar);
    }

    @Test
    @DisplayName("Una venta de otro negocio responde 404 al intentar cobrarla")
    void registrarShouldHideForeignSale() throws Exception {
        when(registrar.execute(any())).thenThrow(new ResourceNotFoundException("Venta no encontrada"));

        mockMvc.perform(post("/api/caja/cobros").principal(authentication()).contentType("application/json").content(cuerpo()))
                .andExpect(status().isNotFound());
    }

    // --- helpers ---
    private UsernamePasswordAuthenticationToken authentication() {
        return UsernamePasswordAuthenticationToken.authenticated(CajaTestData.USUARIO.toString(), null, List.of());
    }

    private String cuerpo() {
        return """
                {"id":"%s","proyectoId":"%s","inventarioId":"%s","aplicaciones":[
                {"ventaId":"%s","pagos":[{"medio":"TRANSFERENCIA","importe":600}]}]}
                """.formatted(CobroCajaTestData.COBRO, CajaTestData.PROYECTO, CajaTestData.INVENTARIO, CajaTestData.OPERACION);
    }
}
