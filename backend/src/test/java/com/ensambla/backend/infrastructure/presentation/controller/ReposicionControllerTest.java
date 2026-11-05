package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.reposicion.armarPedidoProveedor.ArmarPedidoProveedor;
import com.ensambla.backend.domain.useCase.reposicion.armarPedidoProveedor.ArmarPedidoProveedorRequest;
import com.ensambla.backend.domain.useCase.reposicion.armarPedidoProveedor.ArmarPedidoProveedorRequest.LineaDePedido;
import com.ensambla.backend.domain.useCase.reposicion.armarPedidoProveedor.ArmarPedidoProveedorResponse;
import com.ensambla.backend.domain.useCase.reposicion.consultarReposicion.ConsultarReposicion;
import com.ensambla.backend.domain.useCase.reposicion.consultarReposicion.ConsultarReposicionRequest;
import com.ensambla.backend.domain.useCase.reposicion.consultarReposicion.ConsultarReposicionResponse;
import com.ensambla.backend.domain.useCase.reposicion.consultarReposicion.GrupoDeProveedorItem;
import com.ensambla.backend.domain.useCase.reposicion.consultarReposicion.ProductoFaltanteItem;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.reposicion.ReposicionPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

import static com.ensambla.backend.support.ReposicionDePrueba.PROYECTO_ID;
import static com.ensambla.backend.support.ReposicionDePrueba.YERBA_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReposicionControllerTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final String RUTA = "/api/proyectos/{id}/reposicion";
    private static final String RUTA_PDF = RUTA + "/pdf";
    private static final byte[] PDF = {'%', 'P', 'D', 'F'};

    private final Principal sesion = new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);

    private ConsultarReposicion consultarReposicion;
    private ArmarPedidoProveedor armarPedidoProveedor;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        consultarReposicion = mock(ConsultarReposicion.class);
        armarPedidoProveedor = mock(ArmarPedidoProveedor.class);
        mockMvc = MockMvcDePrueba.standalone(new ReposicionController(
                        consultarReposicion, armarPedidoProveedor, new ReposicionPresentationMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET devuelve los productos a reponer agrupados por proveedor")
    void consultarShouldReturnTheMissingProductsByGroup() throws Exception {
        givenUnGrupoConUnProducto();

        ResultActions result = whenConsulta();

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.grupos[0].proveedor").value("Molinos"))
                .andExpect(jsonPath("$.grupos[0].productos[0].id").value(YERBA_ID.toString()))
                .andExpect(jsonPath("$.grupos[0].productos[0].nombre").value("Yerba"))
                .andExpect(jsonPath("$.grupos[0].productos[0].cantidadAPedir").value(17));
        verify(consultarReposicion).execute(new ConsultarReposicionRequest(USUARIO_ID, PROYECTO_ID));
    }

    @Test
    @DisplayName("GET de un sistema de otro negocio responde 404")
    void consultarShouldReturn404ForAForeignProject() throws Exception {
        when(consultarReposicion.execute(any(ConsultarReposicionRequest.class)))
                .thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));

        ResultActions result = whenConsulta();

        result.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET responde 400 cuando el id del sistema no es un UUID")
    void consultarShouldReturn400WhenTheIdIsNotAUuid() throws Exception {
        ResultActions result = mockMvc.perform(get(RUTA, "id-invalido").principal(sesion));

        result.andExpect(status().isBadRequest());
        verifyNoInteractions(consultarReposicion);
    }

    @Test
    @DisplayName("POST devuelve el PDF como adjunto y pasa el proveedor y las cantidades al caso de uso")
    void descargarPedidoShouldReturnThePdfAsAttachment() throws Exception {
        when(armarPedidoProveedor.execute(any(ArmarPedidoProveedorRequest.class)))
                .thenReturn(new ArmarPedidoProveedorResponse(PDF, "pedido-molinos-20261019.pdf"));

        ResultActions result = whenPide(cuerpoDelPedido(YERBA_ID.toString(), "12"));

        result.andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"pedido-molinos-20261019.pdf\""))
                .andExpect(content().bytes(PDF));
        verify(armarPedidoProveedor).execute(new ArmarPedidoProveedorRequest(USUARIO_ID, PROYECTO_ID, "Molinos",
                List.of(new LineaDePedido(YERBA_ID, new BigDecimal("12")))));
    }

    @Test
    @DisplayName("POST responde 400 cuando el pedido no tiene productos")
    void descargarPedidoShouldReturn400WhenThereAreNoProducts() throws Exception {
        ResultActions result = whenPide("{\"proveedor\":\"Molinos\",\"productos\":[]}");

        result.andExpect(status().isBadRequest());
        verifyNoInteractions(armarPedidoProveedor);
    }

    @Test
    @DisplayName("POST responde 400 cuando una cantidad no es positiva")
    void descargarPedidoShouldReturn400WhenAQuantityIsNotPositive() throws Exception {
        ResultActions result = whenPide(cuerpoDelPedido(YERBA_ID.toString(), "0"));

        result.andExpect(status().isBadRequest());
        verifyNoInteractions(armarPedidoProveedor);
    }

    @Test
    @DisplayName("POST responde 400 cuando el caso de uso rechaza el pedido")
    void descargarPedidoShouldReturn400WhenTheUseCaseRejectsTheOrder() throws Exception {
        when(armarPedidoProveedor.execute(any(ArmarPedidoProveedorRequest.class)))
                .thenThrow(new BusinessRuleException("Uno de los productos ya no hace falta reponerlo"));

        ResultActions result = whenPide(cuerpoDelPedido(YERBA_ID.toString(), "12"));

        result.andExpect(status().isBadRequest());
    }

    // --- arrange ---
    private void givenUnGrupoConUnProducto() {
        ProductoFaltanteItem yerba = new ProductoFaltanteItem(
                YERBA_ID, "Yerba", new BigDecimal("3"), new BigDecimal("17"), "Quedan 3 (mínimo 10)");
        when(consultarReposicion.execute(any(ConsultarReposicionRequest.class))).thenReturn(
                new ConsultarReposicionResponse(List.of(new GrupoDeProveedorItem("Molinos", List.of(yerba)))));
    }

    private String cuerpoDelPedido(String productoId, String cantidad) {
        return "{\"proveedor\":\"Molinos\",\"productos\":[{\"productoId\":\"" + productoId
                + "\",\"cantidad\":" + cantidad + "}]}";
    }

    // --- act ---
    private ResultActions whenConsulta() throws Exception {
        return mockMvc.perform(get(RUTA, PROYECTO_ID).principal(sesion));
    }

    private ResultActions whenPide(String cuerpo) throws Exception {
        return mockMvc.perform(post(RUTA_PDF, PROYECTO_ID).principal(sesion)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }
}
