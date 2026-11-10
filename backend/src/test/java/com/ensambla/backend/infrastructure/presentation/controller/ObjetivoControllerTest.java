package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.objetivo.TipoPasoObjetivo;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasos;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosRequest;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosResponse;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosResponse.PasoItem;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosResponse.PrecioZonaItem;
import com.ensambla.backend.infrastructure.presentation.mapper.objetivo.ObjetivoPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ObjetivoControllerTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");

    private final ObtenerPasos obtenerPasos = mock(ObtenerPasos.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjetivoController controller = new ObjetivoController(obtenerPasos, new ObjetivoPresentationMapper());
        mockMvc = MockMvcDePrueba.standalone(controller).build();
    }

    @Test
    @DisplayName("GET pasos devuelve los productos reales con el rango de zona")
    void pasosShouldReturnRealProductsWithZoneRange() throws Exception {
        when(obtenerPasos.execute(any())).thenReturn(response());

        mockMvc.perform(get("/api/objetivos/pasos").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("SUBIR_PRECIO"))
                .andExpect(jsonPath("$[0].nombre").value("Yerba"))
                .andExpect(jsonPath("$[0].precio").value(3600))
                .andExpect(jsonPath("$[0].costo").value(2700))
                .andExpect(jsonPath("$[0].stock").value(4))
                .andExpect(jsonPath("$[0].vendidas30Dias").value(60))
                .andExpect(jsonPath("$[0].precioZona.minimo").value(3500))
                .andExpect(jsonPath("$[0].precioZona.promedio").value(3900))
                .andExpect(jsonPath("$[0].precioZona.maximo").value(4300));
        verify(obtenerPasos).execute(new ObtenerPasosRequest(USUARIO_ID));
    }

    @Test
    @DisplayName("GET pasos devuelve una lista vacía cuando no hay productos para recomendar")
    void pasosShouldReturnEmptyListWithoutCandidates() throws Exception {
        when(obtenerPasos.execute(any())).thenReturn(new ObtenerPasosResponse(List.of()));

        mockMvc.perform(get("/api/objetivos/pasos").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    private ObtenerPasosResponse response() {
        return new ObtenerPasosResponse(List.of(new PasoItem(
                TipoPasoObjetivo.SUBIR_PRECIO,
                "Yerba",
                decimal("3600"),
                decimal("2700"),
                decimal("4"),
                decimal("60"),
                new PrecioZonaItem(decimal("3500"), decimal("3900"), decimal("4300")))));
    }

    private BigDecimal decimal(String valor) {
        return new BigDecimal(valor);
    }
}
