package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.metrica.AgrupacionMetrica;
import com.ensambla.backend.domain.model.metrica.AutoriaMetrica;
import com.ensambla.backend.domain.model.metrica.CalculoMetrica;
import com.ensambla.backend.domain.model.metrica.DeDondeSale;
import com.ensambla.backend.domain.model.metrica.DefinicionMetrica;
import com.ensambla.backend.domain.model.metrica.FormatoMetrica;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.model.metrica.PeriodoMetrica;
import com.ensambla.backend.domain.model.metrica.ResultadoMetrica;
import com.ensambla.backend.domain.model.metrica.ResultadoMetrica.Fila;
import com.ensambla.backend.domain.useCase.metrica.DefinicionItem;
import com.ensambla.backend.domain.useCase.metrica.MetricaItem;
import com.ensambla.backend.domain.useCase.metrica.ResultadoItem;
import com.ensambla.backend.domain.useCase.metrica.SugeridaItem;
import com.ensambla.backend.domain.useCase.metrica.crearMetrica.CrearMetrica;
import com.ensambla.backend.domain.useCase.metrica.crearMetrica.CrearMetricaRequest;
import com.ensambla.backend.domain.useCase.metrica.eliminarMetrica.EliminarMetrica;
import com.ensambla.backend.domain.useCase.metrica.eliminarMetrica.EliminarMetricaRequest;
import com.ensambla.backend.domain.useCase.metrica.listarMetricas.ListarMetricas;
import com.ensambla.backend.domain.useCase.metrica.listarMetricas.ListarMetricasRequest;
import com.ensambla.backend.domain.useCase.metrica.listarSugeridas.ListarSugeridas;
import com.ensambla.backend.domain.useCase.metrica.listarSugeridas.ListarSugeridasRequest;
import com.ensambla.backend.domain.useCase.metrica.previsualizarMetrica.PrevisualizarMetrica;
import com.ensambla.backend.domain.useCase.metrica.previsualizarMetrica.PrevisualizarMetricaRequest;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.metrica.MetricaPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.ensambla.backend.support.MetricaTestData.METRICA_ID;
import static com.ensambla.backend.support.MetricaTestData.PROYECTO_ID;
import static com.ensambla.backend.support.MetricaTestData.USUARIO_ID;
import static com.ensambla.backend.support.MetricaTestData.metrica;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MetricaControllerTest {

    private static final String RUTA = "/api/proyectos/{proyectoId}/metricas";
    private static final String CUERPO = "{\"nombre\":\"Ventas del mes\",\"fuente\":\"VENTAS\",\"calculo\":\"SUMA\","
            + "\"agrupar\":\"PRODUCTO\",\"periodo\":\"MES\",\"soloFiado\":true";

    private final ListarMetricas listarMetricas = mock(ListarMetricas.class);
    private final ListarSugeridas listarSugeridas = mock(ListarSugeridas.class);
    private final PrevisualizarMetrica previsualizarMetrica = mock(PrevisualizarMetrica.class);
    private final CrearMetrica crearMetrica = mock(CrearMetrica.class);
    private final EliminarMetrica eliminarMetrica = mock(EliminarMetrica.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MetricaController controller = new MetricaController(
                listarMetricas,
                listarSugeridas,
                previsualizarMetrica,
                crearMetrica,
                eliminarMetrica,
                new MetricaPresentationMapper());
        mockMvc = MockMvcDePrueba.standalone(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET lista las métricas del cuaderno con su receta, su número y de dónde sale")
    void listarShouldReturnMappedMetrics() throws Exception {
        when(listarMetricas.execute(any())).thenReturn(List.of(givenItem()));

        ResultActions result = mockMvc.perform(get(RUTA, PROYECTO_ID).principal(principal));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(METRICA_ID.toString()))
                .andExpect(jsonPath("$[0].orden").value(2))
                .andExpect(jsonPath("$[0].autoria").value("SUGERIDA"))
                .andExpect(jsonPath("$[0].definicion.nombre").value("Ventas del mes"))
                .andExpect(jsonPath("$[0].definicion.fuente").value("VENTAS"))
                .andExpect(jsonPath("$[0].definicion.soloFiado").value(true))
                .andExpect(jsonPath("$[0].resultado.formato").value("MONEDA"))
                .andExpect(jsonPath("$[0].resultado.valor").value(750.0))
                .andExpect(jsonPath("$[0].resultado.filas[0].etiqueta").value("Pan"))
                .andExpect(jsonPath("$[0].resultado.deDondeSale.filasUsadas").value(3))
                .andExpect(jsonPath("$[0].resultado.deDondeSale.desde").value("2026-11-05"));
    }

    @Test
    @DisplayName("GET delega en el caso de uso con el usuario de la sesión y el cuaderno de la ruta")
    void listarShouldPassSessionUserAndProject() throws Exception {
        when(listarMetricas.execute(any())).thenReturn(List.of());

        ResultActions result = mockMvc.perform(get(RUTA, PROYECTO_ID).principal(principal));

        result.andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        verify(listarMetricas).execute(new ListarMetricasRequest(USUARIO_ID, PROYECTO_ID));
    }

    @Test
    @DisplayName("GET de un cuaderno ajeno devuelve el 404 estándar")
    void listarShouldReturn404ForForeignProject() throws Exception {
        when(listarMetricas.execute(any())).thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));

        ResultActions result = mockMvc.perform(get(RUTA, PROYECTO_ID).principal(principal));

        result.andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET con un id de cuaderno que no es UUID devuelve 400 sin llegar al caso de uso")
    void listarShouldReturn400WhenProjectIdIsNotUuid() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/proyectos/invalido/metricas").principal(principal));

        result.andExpect(status().isBadRequest());
        verifyNoInteractions(listarMetricas);
    }

    @Test
    @DisplayName("GET sugeridas devuelve las del rubro con lo que darían hoy")
    void sugeridasShouldReturnMappedSuggestions() throws Exception {
        ResultadoItem sinNumero = ResultadoItem.de(ResultadoMetrica.sinValor(
                FormatoMetrica.NUMERO, "No hay ventas en este período.",
                new DeDondeSale(FuenteMetrica.VENTAS, 0, 0, null, null)));
        when(listarSugeridas.execute(any())).thenReturn(List.of(new SugeridaItem(
                DefinicionItem.de(definicion()), sinNumero)));

        ResultActions result = mockMvc.perform(get(RUTA + "/sugeridas", PROYECTO_ID).principal(principal));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].definicion.nombre").value("Ventas del mes"))
                .andExpect(jsonPath("$[0].resultado.valor").doesNotExist())
                .andExpect(jsonPath("$[0].resultado.motivo").value("No hay ventas en este período."));
        verify(listarSugeridas).execute(new ListarSugeridasRequest(USUARIO_ID, PROYECTO_ID));
    }

    @Test
    @DisplayName("POST previsualizar calcula la receta sin guardarla")
    void previsualizarShouldReturnTheResult() throws Exception {
        when(previsualizarMetrica.execute(any())).thenReturn(ResultadoItem.de(resultado()));

        ResultActions result = mockMvc.perform(post(RUTA + "/previsualizar", PROYECTO_ID).principal(principal)
                .contentType("application/json")
                .content(CUERPO + "}"));

        result.andExpect(status().isOk()).andExpect(jsonPath("$.valor").value(750.0));
        verify(previsualizarMetrica).execute(new PrevisualizarMetricaRequest(USUARIO_ID, PROYECTO_ID,
                "Ventas del mes", FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.PRODUCTO,
                PeriodoMetrica.MES, true));
    }

    @Test
    @DisplayName("POST previsualizar rechaza una receta que no se puede calcular con el 400 estándar")
    void previsualizarShouldReturn400ForInvalidRecipe() throws Exception {
        when(previsualizarMetrica.execute(any())).thenThrow(new BusinessRuleException("Los gastos no tienen unidades"));

        ResultActions result = mockMvc.perform(post(RUTA + "/previsualizar", PROYECTO_ID).principal(principal)
                .contentType("application/json")
                .content(CUERPO + "}"));

        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST crea la métrica y devuelve 201 con su ubicación")
    void crearShouldReturn201WithLocation() throws Exception {
        when(crearMetrica.execute(any())).thenReturn(givenItem());

        ResultActions result = whenPostCrear(CUERPO + ",\"autoria\":\"SUGERIDA\"}");

        result.andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/proyectos/" + PROYECTO_ID + "/metricas/" + METRICA_ID))
                .andExpect(jsonPath("$.id").value(METRICA_ID.toString()));
    }

    @Test
    @DisplayName("POST pasa la receta, la autoría y el usuario de la sesión al caso de uso")
    void crearShouldPassRequestToUseCase() throws Exception {
        when(crearMetrica.execute(any())).thenReturn(givenItem());

        whenPostCrear(CUERPO + ",\"autoria\":\"PROPIA\"}");

        verify(crearMetrica).execute(new CrearMetricaRequest(USUARIO_ID, PROYECTO_ID, "Ventas del mes",
                FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.PRODUCTO, PeriodoMetrica.MES, true,
                AutoriaMetrica.PROPIA));
    }

    @Test
    @DisplayName("POST rechaza un body sin nombre, sin fuente, sin cálculo o sin autoría")
    void crearShouldRejectMissingRequiredFields() throws Exception {
        ResultActions result = whenPostCrear("{\"soloFiado\":false}");

        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nombre").exists())
                .andExpect(jsonPath("$.errors.fuente").exists())
                .andExpect(jsonPath("$.errors.calculo").exists())
                .andExpect(jsonPath("$.errors.autoria").exists());
        verifyNoInteractions(crearMetrica);
    }

    @Test
    @DisplayName("POST rechaza un nombre de más de 80 caracteres")
    void crearShouldRejectTooLongName() throws Exception {
        ResultActions result = whenPostCrear("{\"nombre\":\"" + "a".repeat(81)
                + "\",\"fuente\":\"VENTAS\",\"calculo\":\"SUMA\",\"autoria\":\"PROPIA\"}");

        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.nombre").exists());
        verifyNoInteractions(crearMetrica);
    }

    @Test
    @DisplayName("DELETE devuelve 204 y delega con el usuario de la sesión, el cuaderno y la métrica de la ruta")
    void eliminarShouldReturn204() throws Exception {
        ResultActions result = mockMvc.perform(delete(RUTA + "/{id}", PROYECTO_ID, METRICA_ID).principal(principal));

        result.andExpect(status().isNoContent());
        verify(eliminarMetrica).execute(new EliminarMetricaRequest(USUARIO_ID, PROYECTO_ID, METRICA_ID));
    }

    @Test
    @DisplayName("DELETE de una métrica ajena o inexistente devuelve el 404 estándar")
    void eliminarShouldReturn404WhenNotOwned() throws Exception {
        doThrow(new ResourceNotFoundException("Métrica no encontrada")).when(eliminarMetrica).execute(any());

        ResultActions result = mockMvc.perform(delete(RUTA + "/{id}", PROYECTO_ID, METRICA_ID).principal(principal));

        result.andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE con un id que no es UUID devuelve 400 sin llegar al caso de uso")
    void eliminarShouldReturn400WhenIdIsNotUuid() throws Exception {
        ResultActions result = mockMvc.perform(delete(RUTA + "/invalido", PROYECTO_ID).principal(principal));

        result.andExpect(status().isBadRequest());
        verifyNoInteractions(eliminarMetrica);
    }

    // --- arrange ---
    private MetricaItem givenItem() {
        return MetricaItem.de(metrica(definicion(), 2), resultado());
    }

    // --- act ---
    private ResultActions whenPostCrear(String body) throws Exception {
        return mockMvc.perform(post(RUTA, PROYECTO_ID).principal(principal)
                .contentType("application/json")
                .content(body));
    }

    // --- helpers ---
    private static DefinicionMetrica definicion() {
        return new DefinicionMetrica("Ventas del mes", FuenteMetrica.VENTAS, CalculoMetrica.SUMA,
                AgrupacionMetrica.PRODUCTO, PeriodoMetrica.MES, true);
    }

    private static ResultadoMetrica resultado() {
        return ResultadoMetrica.conValor(FormatoMetrica.MONEDA, new BigDecimal("750"),
                List.of(new Fila("Pan", new BigDecimal("750"))),
                new DeDondeSale(FuenteMetrica.VENTAS, 3, 1, LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 11)));
    }
}
