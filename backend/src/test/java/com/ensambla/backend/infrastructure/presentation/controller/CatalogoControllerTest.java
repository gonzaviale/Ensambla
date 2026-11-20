package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.CatalogoNoDisponibleException;
import com.ensambla.backend.domain.useCase.catalogo.buscarProducto.BuscarProducto;
import com.ensambla.backend.domain.useCase.catalogo.buscarProducto.BuscarProductoRequest;
import com.ensambla.backend.domain.useCase.catalogo.buscarProducto.BuscarProductoResponse;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.catalogo.CatalogoPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogoControllerTest {

    private final BuscarProducto buscarProducto = mock(BuscarProducto.class);
    private final MockMvc mockMvc = MockMvcDePrueba.standalone(
            new CatalogoController(buscarProducto, new CatalogoPresentationMapper()))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    private ResultActions respuesta;

    @Test
    @DisplayName("El endpoint entrega la ficha normalizada sin exponer el proveedor HTTP al navegador")
    void elEndpointEntregaLaFichaNormalizadaSinExponerElProveedorHttpAlNavegador() throws Exception {
        dadoUnProductoEncontradoEnSepa();

        cuandoSeBuscaElProducto("sepa");

        entoncesSeEntregaLaFichaNormalizada();
    }

    @Test
    @DisplayName("Un producto ausente devuelve una respuesta vacía sin error")
    void unProductoAusenteDevuelveUnaRespuestaVaciaSinError() throws Exception {
        dadoUnProductoAusenteEnSepa();

        cuandoSeBuscaElProducto("sepa");

        entoncesLaRespuestaVieneVaciaSinError();
    }

    @Test
    @DisplayName("Una caída devuelve 503 con un mensaje seguro para continuar la carga manual")
    void unaCaidaDevuelve503ConUnMensajeSeguroParaContinuarLaCargaManual() throws Exception {
        dadoUnCatalogoCaido();

        cuandoSeBuscaElProducto("openfoodfacts");

        entoncesRespondeNoDisponibleConUnMensajeSeguro();
    }

    // --- dado ---

    private void dadoUnProductoEncontradoEnSepa() {
        when(buscarProducto.execute(new BuscarProductoRequest("sepa", "77903754")))
                .thenReturn(new BuscarProductoResponse(new BuscarProductoResponse.ProductoItem(
                        "77903754", "Alfajor", "Marca", "40 g", "https://images.openfoodfacts.org/foto.jpg")));
    }

    private void dadoUnProductoAusenteEnSepa() {
        when(buscarProducto.execute(new BuscarProductoRequest("sepa", "77903754")))
                .thenReturn(new BuscarProductoResponse(null));
    }

    private void dadoUnCatalogoCaido() {
        when(buscarProducto.execute(new BuscarProductoRequest("openfoodfacts", "77903754")))
                .thenThrow(new CatalogoNoDisponibleException());
    }

    // --- cuando ---

    private void cuandoSeBuscaElProducto(String fuente) throws Exception {
        respuesta = mockMvc.perform(get("/api/catalogos/" + fuente + "/77903754"));
    }

    // --- entonces ---

    private void entoncesSeEntregaLaFichaNormalizada() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.producto.codigo").value("77903754"))
                .andExpect(jsonPath("$.producto.nombre").value("Alfajor"))
                .andExpect(jsonPath("$.producto.marca").value("Marca"))
                .andExpect(jsonPath("$.producto.presentacion").value("40 g"))
                .andExpect(jsonPath("$.producto.imagen").value("https://images.openfoodfacts.org/foto.jpg"));
    }

    private void entoncesLaRespuestaVieneVaciaSinError() throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$.producto").doesNotExist());
    }

    private void entoncesRespondeNoDisponibleConUnMensajeSeguro() throws Exception {
        respuesta.andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(
                        "El catálogo no está disponible en este momento. Podés completar el producto a mano."));
    }
}
