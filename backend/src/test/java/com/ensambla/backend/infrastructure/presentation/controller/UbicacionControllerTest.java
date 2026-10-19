package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.dto.ubicacion.BuscarDireccionesRequest;
import com.ensambla.backend.domain.dto.ubicacion.BuscarDireccionesResponse;
import com.ensambla.backend.domain.dto.ubicacion.BuscarDireccionesResponse.DireccionItem;
import com.ensambla.backend.domain.dto.ubicacion.ListarRubrosResponse;
import com.ensambla.backend.domain.dto.ubicacion.ListarRubrosResponse.RubroItem;
import com.ensambla.backend.domain.exception.ServicioUbicacionException;
import com.ensambla.backend.domain.useCase.ubicacion.BuscarDireccionesUseCase;
import com.ensambla.backend.domain.useCase.ubicacion.ListarRubrosUseCase;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.ubicacion.UbicacionPresentationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UbicacionControllerTest {

    private static final String CONSULTA = "Varela 1903, San Justo";
    private static final BuscarDireccionesRequest REQUEST = new BuscarDireccionesRequest(CONSULTA);
    private BuscarDireccionesUseCase buscarDireccionesUseCase;
    private ListarRubrosUseCase listarRubrosUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        buscarDireccionesUseCase = mock(BuscarDireccionesUseCase.class);
        listarRubrosUseCase = mock(ListarRubrosUseCase.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UbicacionController(
                        buscarDireccionesUseCase, listarRubrosUseCase, new UbicacionPresentationMapper()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    @DisplayName("La búsqueda devuelve opciones completas y prohíbe la caché del navegador")
    void buscarShouldReturnDirectionsWithoutBrowserCache() throws Exception {
        when(buscarDireccionesUseCase.execute(REQUEST)).thenReturn(new BuscarDireccionesResponse(List.of(
                new DireccionItem(CONSULTA, new BigDecimal("-34.67"), new BigDecimal("-58.56")))));

        mockMvc.perform(post("/api/ubicacion/direcciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consulta\":\"" + CONSULTA + "\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.resultados[0].direccion").value(CONSULTA))
                .andExpect(jsonPath("$.resultados[0].latitud").value(-34.67))
                .andExpect(jsonPath("$.resultados[0].longitud").value(-58.56));

        verify(buscarDireccionesUseCase).execute(REQUEST);
    }

    @Test
    @DisplayName("La búsqueda sin coincidencias responde con una lista vacía")
    void buscarShouldReturnEmptyResults() throws Exception {
        when(buscarDireccionesUseCase.execute(REQUEST)).thenReturn(new BuscarDireccionesResponse(List.of()));

        mockMvc.perform(post("/api/ubicacion/direcciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consulta\":\"" + CONSULTA + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resultados").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"consulta\":null}", "{\"consulta\":\"    \"}", "{\"consulta\":\"abcd\"}"})
    @DisplayName("Una dirección inválida no llega al caso de uso")
    void buscarShouldRejectInvalidBody(String body) throws Exception {
        mockMvc.perform(post("/api/ubicacion/direcciones").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.consulta").exists());

        verifyNoInteractions(buscarDireccionesUseCase);
    }

    @Test
    @DisplayName("El proveedor caído responde 503 con el formato de error de la aplicación")
    void buscarShouldReturn503WhenProviderUnavailable() throws Exception {
        when(buscarDireccionesUseCase.execute(REQUEST)).thenThrow(new ServicioUbicacionException("Sin servicio"));

        mockMvc.perform(post("/api/ubicacion/direcciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consulta\":\"" + CONSULTA + "\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Sin servicio"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("El catálogo devuelve código estable y nombre legible")
    void listarRubrosShouldReturnCatalog() throws Exception {
        when(listarRubrosUseCase.execute()).thenReturn(new ListarRubrosResponse(List.of(
                new RubroItem("ALMACEN", "Almacén"), new RubroItem("OTRO", "Otro"))));

        mockMvc.perform(get("/api/ubicacion/rubros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rubros[0].codigo").value("ALMACEN"))
                .andExpect(jsonPath("$.rubros[0].nombre").value("Almacén"))
                .andExpect(jsonPath("$.rubros[1].codigo").value("OTRO"));
    }

    @Test
    @DisplayName("El listado respeta un catálogo vacío")
    void listarRubrosShouldReturnEmptyCatalog() throws Exception {
        when(listarRubrosUseCase.execute()).thenReturn(new ListarRubrosResponse(List.of()));

        mockMvc.perform(get("/api/ubicacion/rubros"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rubros").isEmpty());
    }
}
