package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.VistaZona;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.domain.useCase.zona.ZonaItem.ProductoItem;
import com.ensambla.backend.domain.useCase.zona.ZonaItem;
import com.ensambla.backend.domain.useCase.zona.analizarZona.AnalizarZona;
import com.ensambla.backend.domain.useCase.zona.analizarZona.AnalizarZonaRequest;
import com.ensambla.backend.domain.useCase.zona.obtenerZona.ObtenerZona;
import com.ensambla.backend.domain.useCase.zona.obtenerZona.ObtenerZonaRequest;
import com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir.ExplorarDondeAbrir;
import com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir.ExplorarDondeAbrirRequest;
import com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir.ExplorarDondeAbrirResponse;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.zona.ZonaPresentationMapper;
import com.ensambla.backend.infrastructure.presentation.mapper.zona.DondeAbrirPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");

    private final ObtenerZona obtenerZona = mock(ObtenerZona.class);
    private final AnalizarZona analizarZona = mock(AnalizarZona.class);
    private final ExplorarDondeAbrir explorarDondeAbrir = mock(ExplorarDondeAbrir.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new ZonaController(obtenerZona, analizarZona, explorarDondeAbrir,
                new ZonaPresentationMapper(), new DondeAbrirPresentationMapper());
        mockMvc = MockMvcDePrueba.standalone(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET agregados devuelve la zona con la forma que espera el front")
    void agregadosShouldReturnZoneShape() throws Exception {
        when(obtenerZona.execute(any())).thenReturn(givenZona());

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
        verify(obtenerZona).execute(new ObtenerZonaRequest(USUARIO_ID, VistaZona.PRECIOS));
    }

    @Test
    @DisplayName("GET agregados con vista=cerca pide la pantalla Qué se vende cerca")
    void agregadosShouldDelegateNearbyView() throws Exception {
        when(obtenerZona.execute(any())).thenReturn(givenZona());

        mockMvc.perform(get("/api/zona/agregados").param("vista", "cerca").principal(principal))
                .andExpect(status().isOk());
        verify(obtenerZona).execute(new ObtenerZonaRequest(USUARIO_ID, VistaZona.CERCA));
    }

    @Test
    @DisplayName("GET agregados rechaza una vista desconocida sin tocar el caso de uso")
    void agregadosShouldRejectUnknownView() throws Exception {
        mockMvc.perform(get("/api/zona/agregados").param("vista", "otra").principal(principal))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(obtenerZona);
    }

    @Test
    @DisplayName("POST análisis delega el lugar escrito")
    void analizarShouldDelegatePlace() throws Exception {
        when(analizarZona.execute(any())).thenReturn(givenZona());

        mockMvc.perform(post("/api/zona/analisis").principal(principal).contentType("application/json")
                        .content("{\"lugar\":\"San Justo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("San Justo - UNLaM"));
        verify(analizarZona).execute(new AnalizarZonaRequest("San Justo", null, null, USUARIO_ID));
    }

    @Test
    @DisplayName("POST análisis delega el punto del mapa")
    void analizarShouldDelegatePoint() throws Exception {
        when(analizarZona.execute(any())).thenReturn(givenZona());

        mockMvc.perform(post("/api/zona/analisis").principal(principal).contentType("application/json")
                        .content("{\"latitud\":-34.6703,\"longitud\":-58.5628}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("San Justo - UNLaM"));
        verify(analizarZona).execute(new AnalizarZonaRequest(null, new BigDecimal("-34.6703"),
                new BigDecimal("-58.5628"), USUARIO_ID));
    }

    @Test
    @DisplayName("POST análisis devuelve la zona sin productos cuando no hay datos suficientes")
    void analizarShouldReturnEmptyProducts() throws Exception {
        when(analizarZona.execute(any())).thenReturn(new ZonaItem("Villa Crespo", 2, List.of()));

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
        verifyNoInteractions(analizarZona);
    }

    @Test
    @DisplayName("POST análisis rechaza una latitud fuera de rango sin tocar el caso de uso")
    void analizarShouldRejectLatitudeOutOfRange() throws Exception {
        mockMvc.perform(post("/api/zona/analisis").principal(principal).contentType("application/json")
                        .content("{\"latitud\":95,\"longitud\":-58.5628}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(analizarZona);
    }

    @Test
    @DisplayName("POST análisis rechaza una longitud fuera de rango sin tocar el caso de uso")
    void analizarShouldRejectLongitudeOutOfRange() throws Exception {
        mockMvc.perform(post("/api/zona/analisis").principal(principal).contentType("application/json")
                        .content("{\"latitud\":-34.6703,\"longitud\":-200}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(analizarZona);
    }

    @Test
    @DisplayName("POST análisis responde 404 cuando no hay datos de esa zona")
    void analizarShouldReturnNotFoundWithoutData() throws Exception {
        when(analizarZona.execute(any()))
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
        verifyNoInteractions(analizarZona);
    }

    @Test
    @DisplayName("POST dónde abrir toma el usuario de la sesión y devuelve el ranking")
    void dondeAbrirShouldDelegateAuthenticatedBusiness() throws Exception {
        when(explorarDondeAbrir.execute(any())).thenReturn(givenDondeAbrir());

        mockMvc.perform(post("/api/zona/donde-abrir").principal(principal).contentType("application/json")
                        .content("""
                                {"rubro":"ALMACEN","publico":"VECINOS","importancia":{"ESCUELAS":2},
                                 "limites":{"distanciaMaxKm":4},
                                 "alquileres":[{"zonaId":"c0000000-0000-4000-8000-000000000001","valor":500000}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluadas").value(1))
                .andExpect(jsonPath("$.ranking[0].nombre").value("San Justo - UNLaM"));
        verify(explorarDondeAbrir).execute(new ExplorarDondeAbrirRequest(
                USUARIO_ID, RubroNegocio.ALMACEN, com.ensambla.backend.domain.model.zona.PublicoDondeAbrir.VECINOS,
                Map.of(com.ensambla.backend.domain.model.zona.FactorDondeAbrir.ESCUELAS, 2),
                new ExplorarDondeAbrirRequest.Limites(new BigDecimal("4"), null, null, null, null, null),
                Map.of(ZonaId.de("c0000000-0000-4000-8000-000000000001"), new BigDecimal("500000"))));
    }

    @Test
    @DisplayName("POST dónde abrir valida rubro y público antes de ejecutar")
    void dondeAbrirShouldValidateRequiredFields() throws Exception {
        mockMvc.perform(post("/api/zona/donde-abrir").principal(principal).contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(explorarDondeAbrir);
    }

    // --- arrange ---
    private ZonaItem givenZona() {
        return new ZonaItem("San Justo - UNLaM", 10, List.of(new ProductoItem("Yerba 1kg", 8,
                new BigDecimal("2800.00"), new BigDecimal("3100.00"), new BigDecimal("3400.00"), 6,
                new BigDecimal("12.50"))));
    }

    private ExplorarDondeAbrirResponse givenDondeAbrir() {
        var negocio = new ExplorarDondeAbrirResponse.NegocioActual(
                "Almacén", RubroNegocio.ALMACEN, "Arieta 1", new BigDecimal("-34.67"),
                new BigDecimal("-58.56"), null);
        var zona = new ExplorarDondeAbrirResponse.ZonaEvaluada(
                ZonaId.de("c0000000-0000-4000-8000-000000000001"), "San Justo - UNLaM", "San Justo",
                "La Matanza", new BigDecimal("-34.67"), new BigDecimal("-58.56"), BigDecimal.ONE,
                new BigDecimal("8.5"), "BUENA", List.of(), List.of("Buen acceso"), List.of("Confirmar en calle"),
                List.of());
        return new ExplorarDondeAbrirResponse(negocio, 1, List.of(zona), List.of(), List.of());
    }
}
