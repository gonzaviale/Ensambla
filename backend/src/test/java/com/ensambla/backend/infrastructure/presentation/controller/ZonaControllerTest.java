package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.dto.zona.AnalizarZonaRequest;
import com.ensambla.backend.domain.dto.zona.ObtenerZonaRequest;
import com.ensambla.backend.domain.dto.zona.ZonaItem;
import com.ensambla.backend.domain.dto.zona.ZonaItem.ProductoItem;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.useCase.zona.AnalizarZonaUseCase;
import com.ensambla.backend.domain.useCase.zona.ObtenerZonaUseCase;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.zona.ZonaPresentationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ZonaControllerTest {

    private static final UUID USUARIO_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");

    private final ObtenerZonaUseCase obtenerZonaUseCase = mock(ObtenerZonaUseCase.class);
    private final AnalizarZonaUseCase analizarZonaUseCase = mock(AnalizarZonaUseCase.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new ZonaController(obtenerZonaUseCase, analizarZonaUseCase, new ZonaPresentationMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET agregados devuelve la zona con la forma que espera el front")
    void agregadosShouldReturnZoneShape() throws Exception {
        when(obtenerZonaUseCase.execute(any())).thenReturn(givenZona());

        mockMvc.perform(get("/api/zona/agregados").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("San Justo - UNLaM"))
                .andExpect(jsonPath("$.comercios").value(10))
                .andExpect(jsonPath("$.productos[0].nombre").value("Yerba 1kg"))
                .andExpect(jsonPath("$.productos[0].comercios").value(8))
                .andExpect(jsonPath("$.productos[0].precio.minimo").value(2800.0))
                .andExpect(jsonPath("$.productos[0].precio.promedio").value(3100.0))
                .andExpect(jsonPath("$.productos[0].precio.maximo").value(3400.0))
                .andExpect(jsonPath("$.productos[0].ventas.comercios").value(6))
                .andExpect(jsonPath("$.productos[0].ventas.unidadesPorSemana").value(12.5));
        verify(obtenerZonaUseCase).execute(new ObtenerZonaRequest(USUARIO_ID));
    }

    @Test
    @DisplayName("POST análisis delega el lugar escrito")
    void analizarShouldDelegatePlace() throws Exception {
        when(analizarZonaUseCase.execute(any())).thenReturn(givenZona());

        mockMvc.perform(post("/api/zona/analisis").principal(principal).contentType("application/json")
                        .content("{\"lugar\":\"San Justo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("San Justo - UNLaM"));
        verify(analizarZonaUseCase).execute(new AnalizarZonaRequest("San Justo", null, null));
    }

    @Test
    @DisplayName("POST análisis delega el punto del mapa")
    void analizarShouldDelegatePoint() throws Exception {
        when(analizarZonaUseCase.execute(any())).thenReturn(givenZona());

        mockMvc.perform(post("/api/zona/analisis").principal(principal).contentType("application/json")
                        .content("{\"latitud\":-34.6703,\"longitud\":-58.5628}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("San Justo - UNLaM"));
        verify(analizarZonaUseCase).execute(new AnalizarZonaRequest(null, new BigDecimal("-34.6703"),
                new BigDecimal("-58.5628")));
    }

    @Test
    @DisplayName("POST análisis devuelve la zona sin productos cuando no hay datos suficientes")
    void analizarShouldReturnEmptyProducts() throws Exception {
        when(analizarZonaUseCase.execute(any())).thenReturn(new ZonaItem("Villa Crespo", 2, List.of()));

        mockMvc.perform(post("/api/zona/analisis").principal(principal).contentType("application/json")
                        .content("{\"lugar\":\"Villa Crespo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comercios").value(2))
                .andExpect(jsonPath("$.productos").isEmpty());
    }

    @Test
    @DisplayName("POST análisis rechaza un pedido sin lugar ni punto sin tocar el caso de uso")
    void analizarShouldRejectWhenNothingIsIndicated() throws Exception {
        mockMvc.perform(post("/api/zona/analisis").principal(principal).contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(analizarZonaUseCase);
    }

    @Test
    @DisplayName("POST análisis rechaza una latitud fuera de rango sin tocar el caso de uso")
    void analizarShouldRejectLatitudeOutOfRange() throws Exception {
        mockMvc.perform(post("/api/zona/analisis").principal(principal).contentType("application/json")
                        .content("{\"latitud\":95,\"longitud\":-58.5628}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(analizarZonaUseCase);
    }

    @Test
    @DisplayName("POST análisis rechaza una longitud fuera de rango sin tocar el caso de uso")
    void analizarShouldRejectLongitudeOutOfRange() throws Exception {
        mockMvc.perform(post("/api/zona/analisis").principal(principal).contentType("application/json")
                        .content("{\"latitud\":-34.6703,\"longitud\":-200}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(analizarZonaUseCase);
    }

    @Test
    @DisplayName("POST análisis responde 404 cuando no hay datos de esa zona")
    void analizarShouldReturnNotFoundWithoutData() throws Exception {
        when(analizarZonaUseCase.execute(any()))
                .thenThrow(new ResourceNotFoundException("Todavía no tenemos datos de esa zona"));

        mockMvc.perform(post("/api/zona/analisis").principal(principal).contentType("application/json")
                        .content("{\"lugar\":\"Palermo\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST análisis rechaza un lugar demasiado corto sin tocar el caso de uso")
    void analizarShouldRejectShortPlace() throws Exception {
        mockMvc.perform(post("/api/zona/analisis").principal(principal).contentType("application/json")
                        .content("{\"lugar\":\"a\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(analizarZonaUseCase);
    }

    // --- arrange ---
    private ZonaItem givenZona() {
        return new ZonaItem("San Justo - UNLaM", 10, List.of(new ProductoItem("Yerba 1kg", 8,
                new BigDecimal("2800.00"), new BigDecimal("3100.00"), new BigDecimal("3400.00"), 6,
                new BigDecimal("12.50"))));
    }
}
