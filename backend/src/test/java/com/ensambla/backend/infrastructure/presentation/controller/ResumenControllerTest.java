package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.reporte.PeriodoMetricas;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.reporte.AvisoEconomico;
import com.ensambla.backend.domain.useCase.reporte.exportarResumen.ExportarResumenNegocio;
import com.ensambla.backend.domain.useCase.reporte.exportarResumen.ExportarResumenRequest;
import com.ensambla.backend.domain.useCase.reporte.exportarResumen.ExportarResumenResponse;
import com.ensambla.backend.domain.useCase.reporte.obtenerAvisoEconomico.ObtenerAvisoEconomico;
import com.ensambla.backend.domain.useCase.reporte.obtenerAvisoEconomico.ObtenerAvisoEconomicoRequest;
import com.ensambla.backend.domain.useCase.reporte.obtenerAvisoEconomico.ObtenerAvisoEconomicoResponse;
import com.ensambla.backend.domain.useCase.reporte.obtenerMetricas.ObtenerMetricas;
import com.ensambla.backend.domain.useCase.reporte.obtenerMetricas.ObtenerMetricasRequest;
import com.ensambla.backend.domain.useCase.reporte.obtenerMetricas.ObtenerMetricasResponse;
import com.ensambla.backend.domain.useCase.reporte.obtenerResumenInicio.ObtenerResumenInicio;
import com.ensambla.backend.domain.useCase.reporte.obtenerResumenInicio.ObtenerResumenInicioRequest;
import com.ensambla.backend.domain.useCase.reporte.obtenerResumenInicio.ObtenerResumenInicioResponse;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResumenControllerTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final ProyectoId PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000001");

    private final Principal sesion = new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);

    private ExportarResumenNegocio exportarResumenNegocio;
    private ObtenerResumenInicio obtenerResumenInicio;
    private ObtenerMetricas obtenerMetricas;
    private ObtenerAvisoEconomico obtenerAvisoEconomico;
    private MockMvc mockMvc;

    private ResultActions respuesta;
    private ResultActions segundaRespuesta;
    private byte[] contenido;

    @BeforeEach
    void setUp() {
        exportarResumenNegocio = mock(ExportarResumenNegocio.class);
        obtenerResumenInicio = mock(ObtenerResumenInicio.class);
        obtenerMetricas = mock(ObtenerMetricas.class);
        obtenerAvisoEconomico = mock(ObtenerAvisoEconomico.class);
        mockMvc = MockMvcDePrueba.standalone(new ResumenController(
                        exportarResumenNegocio, obtenerResumenInicio, obtenerMetricas, obtenerAvisoEconomico))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET devuelve el PDF como archivo adjunto y pasa el período con el usuario de la sesión")
    void gETDevuelveElPDFComoArchivoAdjuntoYPasaElPeriodoConElUsuarioDeLaSesion() throws Exception {
        dadoElEscenarioDeGETDevuelveElPDFComoArchivoAdjuntoYPasaElPeriodoConElUsuarioDeLaSesion();

        cuandoSeEjecutaGETDevuelveElPDFComoArchivoAdjuntoYPasaElPeriodoConElUsuarioDeLaSesion();

        entoncesSeVerificaGETDevuelveElPDFComoArchivoAdjuntoYPasaElPeriodoConElUsuarioDeLaSesion();
    }

    @Test
    @DisplayName("GET sin fechas las pasa vacías, para que el caso de uso use los últimos 30 días")
    void gETSinFechasLasPasaVaciasParaQueElCasoDeUsoUseLosUltimos30Dias() throws Exception {
        dadoElEscenarioDeGETSinFechasLasPasaVaciasParaQueElCasoDeUsoUseLosUltimos30Dias();

        cuandoSeEjecutaGETSinFechasLasPasaVaciasParaQueElCasoDeUsoUseLosUltimos30Dias();

        entoncesSeVerificaGETSinFechasLasPasaVaciasParaQueElCasoDeUsoUseLosUltimos30Dias();
    }

    @Test
    @DisplayName("GET con un período inválido o una fecha mal escrita responde 400")
    void gETConUnPeriodoInvalidoOUnaFechaMalEscritaResponde400() throws Exception {
        dadoElEscenarioDeGETConUnPeriodoInvalidoOUnaFechaMalEscritaResponde400();

        cuandoSeEjecutaGETConUnPeriodoInvalidoOUnaFechaMalEscritaResponde400();

        entoncesSeVerificaGETConUnPeriodoInvalidoOUnaFechaMalEscritaResponde400();
    }

    @Test
    @DisplayName("GET inicio devuelve las métricas del sistema y pasa el usuario de la sesión")
    void gETInicioDevuelveLasMetricasDelSistemaYPasaElUsuarioDeLaSesion() throws Exception {
        dadoElEscenarioDeGETInicioDevuelveLasMetricasDelSistemaYPasaElUsuarioDeLaSesion();

        cuandoSeEjecutaGETInicioDevuelveLasMetricasDelSistemaYPasaElUsuarioDeLaSesion();

        entoncesSeVerificaGETInicioDevuelveLasMetricasDelSistemaYPasaElUsuarioDeLaSesion();
    }

    @Test
    @DisplayName("GET métricas sin período pide la semana y devuelve los números del sistema")
    void gETMetricasSinPeriodoPideLaSemanaYDevuelveLosNumerosDelSistema() throws Exception {
        dadoElEscenarioDeGETMetricasSinPeriodoPideLaSemanaYDevuelveLosNumerosDelSistema();

        cuandoSeEjecutaGETMetricasSinPeriodoPideLaSemanaYDevuelveLosNumerosDelSistema();

        entoncesSeVerificaGETMetricasSinPeriodoPideLaSemanaYDevuelveLosNumerosDelSistema();
    }

    @Test
    @DisplayName("GET métricas pasa el período que se pidió")
    void gETMetricasPasaElPeriodoQueSePidio() throws Exception {
        dadoElEscenarioDeGETMetricasPasaElPeriodoQueSePidio();

        cuandoSeEjecutaGETMetricasPasaElPeriodoQueSePidio();

        entoncesSeVerificaGETMetricasPasaElPeriodoQueSePidio();
    }

    @Test
    @DisplayName("GET métricas con un período que no existe responde 400 sin llegar al caso de uso")
    void gETMetricasConUnPeriodoQueNoExisteResponde400SinLlegarAlCasoDeUso() throws Exception {
        cuandoSeEjecutaGETMetricasConUnPeriodoQueNoExisteResponde400SinLlegarAlCasoDeUso();

        entoncesSeVerificaGETMetricasConUnPeriodoQueNoExisteResponde400SinLlegarAlCasoDeUso();
    }

    @Test
    @DisplayName("GET aviso económico devuelve el aviso de la semana y pasa el usuario de la sesión")
    void gETAvisoEconomicoDevuelveElAvisoDeLaSemanaYPasaElUsuarioDeLaSesion() throws Exception {
        dadoElEscenarioDeGETAvisoEconomicoDevuelveElAvisoDeLaSemanaYPasaElUsuarioDeLaSesion();

        cuandoSeEjecutaGETAvisoEconomicoDevuelveElAvisoDeLaSemanaYPasaElUsuarioDeLaSesion();

        entoncesSeVerificaGETAvisoEconomicoDevuelveElAvisoDeLaSemanaYPasaElUsuarioDeLaSesion();
    }

    @Test
    @DisplayName("GET aviso económico devuelve el aviso vacío cuando no hay con qué comparar")
    void gETAvisoEconomicoDevuelveElAvisoVacioCuandoNoHayConQueComparar() throws Exception {
        dadoElEscenarioDeGETAvisoEconomicoDevuelveElAvisoVacioCuandoNoHayConQueComparar();

        cuandoSeEjecutaGETAvisoEconomicoDevuelveElAvisoVacioCuandoNoHayConQueComparar();

        entoncesSeVerificaGETAvisoEconomicoDevuelveElAvisoVacioCuandoNoHayConQueComparar();
    }

    @Test
    @DisplayName("GET aviso económico con un id inválido responde 400 sin llegar al caso de uso")
    void gETAvisoEconomicoConUnIdInvalidoResponde400SinLlegarAlCasoDeUso() throws Exception {
        cuandoSeEjecutaGETAvisoEconomicoConUnIdInvalidoResponde400SinLlegarAlCasoDeUso();

        entoncesSeVerificaGETAvisoEconomicoConUnIdInvalidoResponde400SinLlegarAlCasoDeUso();
    }

    // --- helpers ---
    private static ObtenerAvisoEconomicoResponse respuestaDeAviso() {
        return new ObtenerAvisoEconomicoResponse(new ObtenerAvisoEconomicoResponse.Aviso(
                "BAJA", "MEJORA", "Rentabilidad", "Las ventas subieron 12 % y la ganancia subió 5 %",
                "Tu negocio está mejorando.", new BigDecimal("12"), new BigDecimal("5"), List.of("Yerba"),
                List.of(new AvisoEconomico.Cambio("Yerba", new BigDecimal("500"))), "ALTA"));
    }

    private static ObtenerMetricasResponse respuestaDeMetricas() {
        return new ObtenerMetricasResponse(
                "Almacén de Ana",
                PeriodoMetricas.SEMANA,
                LocalDate.parse("2026-10-13"),
                LocalDate.parse("2026-10-19"),
                true,
                new ObtenerMetricasResponse.Ventas(new BigDecimal("6800"), new BigDecimal("2"), 1, 0,
                        new BigDecimal("971.43"), new BigDecimal("3400"), null),
                List.of(),
                List.of(),
                List.of(),
                new ObtenerMetricasResponse.Stock(0, 0, List.of(), BigDecimal.ZERO, BigDecimal.ZERO),
                new ObtenerMetricasResponse.Clientes(List.of(), BigDecimal.ZERO, 0),
                null,
                null,
                new ObtenerMetricasResponse.Avisos(0, List.of(), 0, 0, 0, BigDecimal.ZERO));
    }

    private static ObtenerResumenInicioResponse respuestaDeInicio() {
        return new ObtenerResumenInicioResponse(
                "Almacén de Ana",
                LocalDate.parse("2026-10-19"),
                true,
                true,
                new ObtenerResumenInicioResponse.Ventas(ventasDelPeriodo(), ventasDelPeriodo(), ventasDelPeriodo()),
                List.of(),
                new ObtenerResumenInicioResponse.UltimoDiaConVentas(LocalDate.parse("2026-10-19"),
                        new BigDecimal("6800")),
                null,
                new ObtenerResumenInicioResponse.Faltantes(0, List.of()),
                List.of());
    }

    private static ObtenerResumenInicioResponse.VentasDelPeriodo ventasDelPeriodo() {
        return new ObtenerResumenInicioResponse.VentasDelPeriodo(
                new BigDecimal("6800"), new BigDecimal("2"), 1, 0, new BigDecimal("3400"), BigDecimal.ZERO, 0);
    }

    // --- dado ---

    private void dadoElEscenarioDeGETDevuelveElPDFComoArchivoAdjuntoYPasaElPeriodoConElUsuarioDeLaSesion() throws Exception {
        contenido = new byte[] {'%', 'P', 'D', 'F'};
        when(exportarResumenNegocio.execute(any(ExportarResumenRequest.class)))
                .thenReturn(new ExportarResumenResponse(contenido, "resumen-almacen-20261019.pdf"));
    }

    private void dadoElEscenarioDeGETSinFechasLasPasaVaciasParaQueElCasoDeUsoUseLosUltimos30Dias() throws Exception {
        when(exportarResumenNegocio.execute(any(ExportarResumenRequest.class)))
                .thenReturn(new ExportarResumenResponse(new byte[0], "resumen-20261019.pdf"));
    }

    private void dadoElEscenarioDeGETConUnPeriodoInvalidoOUnaFechaMalEscritaResponde400() throws Exception {
        when(exportarResumenNegocio.execute(any(ExportarResumenRequest.class)))
                .thenThrow(new BusinessRuleException("La fecha desde no puede ser posterior a la fecha hasta"));
    }

    private void dadoElEscenarioDeGETInicioDevuelveLasMetricasDelSistemaYPasaElUsuarioDeLaSesion() throws Exception {
        when(obtenerResumenInicio.execute(any(ObtenerResumenInicioRequest.class))).thenReturn(respuestaDeInicio());
    }

    private void dadoElEscenarioDeGETMetricasSinPeriodoPideLaSemanaYDevuelveLosNumerosDelSistema() throws Exception {
        when(obtenerMetricas.execute(any(ObtenerMetricasRequest.class))).thenReturn(respuestaDeMetricas());
    }

    private void dadoElEscenarioDeGETMetricasPasaElPeriodoQueSePidio() throws Exception {
        when(obtenerMetricas.execute(any(ObtenerMetricasRequest.class))).thenReturn(respuestaDeMetricas());
    }

    private void dadoElEscenarioDeGETAvisoEconomicoDevuelveElAvisoDeLaSemanaYPasaElUsuarioDeLaSesion() throws Exception {
        when(obtenerAvisoEconomico.execute(any(ObtenerAvisoEconomicoRequest.class))).thenReturn(respuestaDeAviso());
    }

    private void dadoElEscenarioDeGETAvisoEconomicoDevuelveElAvisoVacioCuandoNoHayConQueComparar() throws Exception {
        when(obtenerAvisoEconomico.execute(any(ObtenerAvisoEconomicoRequest.class)))
                .thenReturn(new ObtenerAvisoEconomicoResponse(null));
    }

    // --- cuando ---

    private void cuandoSeEjecutaGETDevuelveElPDFComoArchivoAdjuntoYPasaElPeriodoConElUsuarioDeLaSesion() throws Exception {
        respuesta = mockMvc.perform(get("/api/proyectos/{id}/resumen/pdf", PROYECTO_ID)
                        .param("desde", "2026-10-01").param("hasta", "2026-10-07").principal(sesion));
    }

    private void cuandoSeEjecutaGETSinFechasLasPasaVaciasParaQueElCasoDeUsoUseLosUltimos30Dias() throws Exception {
        respuesta = mockMvc.perform(get("/api/proyectos/{id}/resumen/pdf", PROYECTO_ID).principal(sesion));
    }

    private void cuandoSeEjecutaGETConUnPeriodoInvalidoOUnaFechaMalEscritaResponde400() throws Exception {
        respuesta = mockMvc.perform(get("/api/proyectos/{id}/resumen/pdf", PROYECTO_ID).principal(sesion));
        segundaRespuesta = mockMvc.perform(get("/api/proyectos/{id}/resumen/pdf", PROYECTO_ID).param("desde", "ayer").principal(sesion));
    }

    private void cuandoSeEjecutaGETInicioDevuelveLasMetricasDelSistemaYPasaElUsuarioDeLaSesion() throws Exception {
        respuesta = mockMvc.perform(get("/api/proyectos/{id}/resumen/inicio", PROYECTO_ID).principal(sesion));
    }

    private void cuandoSeEjecutaGETMetricasSinPeriodoPideLaSemanaYDevuelveLosNumerosDelSistema() throws Exception {
        respuesta = mockMvc.perform(get("/api/proyectos/{id}/resumen/metricas", PROYECTO_ID).principal(sesion));
    }

    private void cuandoSeEjecutaGETMetricasPasaElPeriodoQueSePidio() throws Exception {
        respuesta = mockMvc.perform(get("/api/proyectos/{id}/resumen/metricas", PROYECTO_ID).param("periodo", "MES")
                        .principal(sesion));
    }

    private void cuandoSeEjecutaGETMetricasConUnPeriodoQueNoExisteResponde400SinLlegarAlCasoDeUso() throws Exception {
        respuesta = mockMvc.perform(get("/api/proyectos/{id}/resumen/metricas", PROYECTO_ID).param("periodo", "ANIO")
                        .principal(sesion));
    }

    private void cuandoSeEjecutaGETAvisoEconomicoDevuelveElAvisoDeLaSemanaYPasaElUsuarioDeLaSesion() throws Exception {
        respuesta = mockMvc.perform(get("/api/proyectos/{id}/resumen/aviso-economico", PROYECTO_ID).principal(sesion));
    }

    private void cuandoSeEjecutaGETAvisoEconomicoDevuelveElAvisoVacioCuandoNoHayConQueComparar() throws Exception {
        respuesta = mockMvc.perform(get("/api/proyectos/{id}/resumen/aviso-economico", PROYECTO_ID).principal(sesion));
    }

    private void cuandoSeEjecutaGETAvisoEconomicoConUnIdInvalidoResponde400SinLlegarAlCasoDeUso() throws Exception {
        respuesta = mockMvc.perform(get("/api/proyectos/{id}/resumen/aviso-economico", "id-invalido").principal(sesion));
    }

    // --- entonces ---

    private void entoncesSeVerificaGETDevuelveElPDFComoArchivoAdjuntoYPasaElPeriodoConElUsuarioDeLaSesion() throws Exception {
        respuesta
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"resumen-almacen-20261019.pdf\""))
                .andExpect(content().bytes(contenido));
        verify(exportarResumenNegocio).execute(new ExportarResumenRequest(
                USUARIO_ID, PROYECTO_ID, LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-07")));
    }

    private void entoncesSeVerificaGETSinFechasLasPasaVaciasParaQueElCasoDeUsoUseLosUltimos30Dias() throws Exception {
        respuesta
                .andExpect(status().isOk());
        verify(exportarResumenNegocio).execute(new ExportarResumenRequest(USUARIO_ID, PROYECTO_ID, null, null));
    }

    private void entoncesSeVerificaGETConUnPeriodoInvalidoOUnaFechaMalEscritaResponde400() throws Exception {
        respuesta
                .andExpect(status().isBadRequest());
        segundaRespuesta
                .andExpect(status().isBadRequest());
    }

    private void entoncesSeVerificaGETInicioDevuelveLasMetricasDelSistemaYPasaElUsuarioDeLaSesion() throws Exception {
        respuesta
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sistema").value("Almacén de Ana"))
                .andExpect(jsonPath("$.hoy").value("2026-10-19"))
                .andExpect(jsonPath("$.hayVentas").value(true))
                .andExpect(jsonPath("$.ventas.hoy.ingresos").value(6800))
                .andExpect(jsonPath("$.faltantes.total").value(0))
                .andExpect(jsonPath("$.parados").isEmpty());
        verify(obtenerResumenInicio).execute(new ObtenerResumenInicioRequest(USUARIO_ID, PROYECTO_ID));
    }

    private void entoncesSeVerificaGETMetricasSinPeriodoPideLaSemanaYDevuelveLosNumerosDelSistema() throws Exception {
        respuesta
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo").value("SEMANA"))
                .andExpect(jsonPath("$.ventas.ingresos").value(6800));
        verify(obtenerMetricas).execute(new ObtenerMetricasRequest(USUARIO_ID, PROYECTO_ID, PeriodoMetricas.SEMANA));
    }

    private void entoncesSeVerificaGETMetricasPasaElPeriodoQueSePidio() throws Exception {
        respuesta
                .andExpect(status().isOk());
        verify(obtenerMetricas).execute(new ObtenerMetricasRequest(USUARIO_ID, PROYECTO_ID, PeriodoMetricas.MES));
    }

    private void entoncesSeVerificaGETMetricasConUnPeriodoQueNoExisteResponde400SinLlegarAlCasoDeUso() throws Exception {
        respuesta
                .andExpect(status().isBadRequest());
        verifyNoInteractions(obtenerMetricas);
    }

    private void entoncesSeVerificaGETAvisoEconomicoDevuelveElAvisoDeLaSemanaYPasaElUsuarioDeLaSesion() throws Exception {
        respuesta
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aviso.prioridad").value("BAJA"))
                .andExpect(jsonPath("$.aviso.tendencia").value("MEJORA"))
                .andExpect(jsonPath("$.aviso.variacionVentas").value(12))
                .andExpect(jsonPath("$.aviso.variacionResultadoBruto").value(5))
                .andExpect(jsonPath("$.aviso.productos[0]").value("Yerba"));
        verify(obtenerAvisoEconomico).execute(new ObtenerAvisoEconomicoRequest(USUARIO_ID, PROYECTO_ID));
    }

    private void entoncesSeVerificaGETAvisoEconomicoDevuelveElAvisoVacioCuandoNoHayConQueComparar() throws Exception {
        respuesta
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aviso").isEmpty());
    }

    private void entoncesSeVerificaGETAvisoEconomicoConUnIdInvalidoResponde400SinLlegarAlCasoDeUso() throws Exception {
        respuesta
                .andExpect(status().isBadRequest());
        verifyNoInteractions(obtenerAvisoEconomico);
    }
}
