package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.registro.RegistroItem;
import com.ensambla.backend.domain.useCase.registro.TotalesItem;
import com.ensambla.backend.domain.useCase.reporte.GenerarReporteRequest;
import com.ensambla.backend.domain.useCase.reporte.exportarReporte.ExportarReporte;
import com.ensambla.backend.domain.useCase.reporte.exportarReporte.ExportarReporteResponse;
import com.ensambla.backend.domain.useCase.reporte.generarReporte.GenerarReporte;
import com.ensambla.backend.domain.useCase.reporte.generarReporte.GenerarReporteResponse;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.reporte.ReportePresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReporteControllerTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final CampoId CAMPO_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-13T12:00:00Z");

    private final Principal sesion = new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);

    private GenerarReporte generarReporte;
    private ExportarReporte exportarReporte;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        generarReporte = mock(GenerarReporte.class);
        exportarReporte = mock(ExportarReporte.class);
        mockMvc = MockMvcDePrueba.standalone(new ReporteController(
                        generarReporte, exportarReporte, new ReportePresentationMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET devuelve las filas, los totales y pasa los filtros con el usuario de la sesión")
    void generarShouldReturnRowsAndPassFilters() throws Exception {
        when(generarReporte.execute(any(GenerarReporteRequest.class))).thenReturn(new GenerarReporteResponse(
                List.of(new RegistroItem(REGISTRO_ID, BLOQUE_ID, Map.of("k", "v"), null, AHORA, AHORA)),
                new TotalesItem(7, Map.of(CAMPO_ID, new BigDecimal("12.5"))),
                true,
                50));

        mockMvc.perform(get("/api/bloques/{id}/reporte", BLOQUE_ID)
                        .param("q", "pan").param("desde", "2026-10-01").param("hasta", "2026-10-31")
                        .param("campoId", CAMPO_ID.toString()).param("valor", "true")
                        .principal(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filas[0].id").value(REGISTRO_ID.toString()))
                .andExpect(jsonPath("$.filas[0].valores.k").value("v"))
                .andExpect(jsonPath("$.totales.cantidad").value(7))
                .andExpect(jsonPath("$.totales.sumas['" + CAMPO_ID + "']").value(12.5))
                .andExpect(jsonPath("$.truncado").value(true))
                .andExpect(jsonPath("$.maximoFilas").value(50));
        verify(generarReporte).execute(new GenerarReporteRequest(
                USUARIO_ID, BLOQUE_ID, "pan", LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"),
                CAMPO_ID, "true"));
    }

    @Test
    @DisplayName("GET sin filtros pasa todo vacío")
    void generarShouldPassNullFiltersWhenThereAreNone() throws Exception {
        when(generarReporte.execute(any(GenerarReporteRequest.class))).thenReturn(
                new GenerarReporteResponse(List.of(), new TotalesItem(0, Map.of()), false, 50));

        mockMvc.perform(get("/api/bloques/{id}/reporte", BLOQUE_ID).principal(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filas").isEmpty());
        verify(generarReporte).execute(
                new GenerarReporteRequest(USUARIO_ID, BLOQUE_ID, null, null, null, null, null));
    }

    @Test
    @DisplayName("GET con un filtro inválido responde 400 y una fecha mal escrita también")
    void generarShouldReturn400ForInvalidFilters() throws Exception {
        when(generarReporte.execute(any(GenerarReporteRequest.class)))
                .thenThrow(new BusinessRuleException("No se puede filtrar por ese campo"));

        mockMvc.perform(get("/api/bloques/{id}/reporte", BLOQUE_ID).principal(sesion))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/bloques/{id}/reporte", BLOQUE_ID).param("desde", "ayer").principal(sesion))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET de la descarga devuelve el XLSX con su nombre y pasa los mismos filtros")
    void descargarShouldReturnTheSpreadsheetAsAttachment() throws Exception {
        byte[] contenido = {1, 2, 3};
        when(exportarReporte.execute(any(GenerarReporteRequest.class)))
                .thenReturn(new ExportarReporteResponse(contenido, "reporte-pedidos-20261013.xlsx"));

        mockMvc.perform(get("/api/bloques/{id}/reporte/descarga", BLOQUE_ID)
                        .param("desde", "2026-10-01").principal(sesion))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"reporte-pedidos-20261013.xlsx\""))
                .andExpect(content().bytes(contenido));
        verify(exportarReporte).execute(new GenerarReporteRequest(
                USUARIO_ID, BLOQUE_ID, null, LocalDate.parse("2026-10-01"), null, null, null));
    }
}
