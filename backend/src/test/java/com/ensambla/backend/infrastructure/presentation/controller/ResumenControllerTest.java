package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.reporte.exportarResumen.ExportarResumenNegocio;
import com.ensambla.backend.domain.useCase.reporte.exportarResumen.ExportarResumenRequest;
import com.ensambla.backend.domain.useCase.reporte.exportarResumen.ExportarResumenResponse;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResumenControllerTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final ProyectoId PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000001");

    private final Principal sesion = new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);

    private ExportarResumenNegocio exportarResumenNegocio;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        exportarResumenNegocio = mock(ExportarResumenNegocio.class);
        mockMvc = MockMvcDePrueba.standalone(new ResumenController(exportarResumenNegocio))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET devuelve el PDF como archivo adjunto y pasa el período con el usuario de la sesión")
    void descargarShouldReturnThePdfAsAttachment() throws Exception {
        byte[] contenido = {'%', 'P', 'D', 'F'};
        when(exportarResumenNegocio.execute(any(ExportarResumenRequest.class)))
                .thenReturn(new ExportarResumenResponse(contenido, "resumen-almacen-20261019.pdf"));

        mockMvc.perform(get("/api/proyectos/{id}/resumen/pdf", PROYECTO_ID)
                        .param("desde", "2026-10-01").param("hasta", "2026-10-07").principal(sesion))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"resumen-almacen-20261019.pdf\""))
                .andExpect(content().bytes(contenido));
        verify(exportarResumenNegocio).execute(new ExportarResumenRequest(
                USUARIO_ID, PROYECTO_ID, LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-07")));
    }

    @Test
    @DisplayName("GET sin fechas las pasa vacías, para que el caso de uso use los últimos 30 días")
    void descargarShouldPassNullDatesWhenThereAreNone() throws Exception {
        when(exportarResumenNegocio.execute(any(ExportarResumenRequest.class)))
                .thenReturn(new ExportarResumenResponse(new byte[0], "resumen-20261019.pdf"));

        mockMvc.perform(get("/api/proyectos/{id}/resumen/pdf", PROYECTO_ID).principal(sesion))
                .andExpect(status().isOk());
        verify(exportarResumenNegocio).execute(new ExportarResumenRequest(USUARIO_ID, PROYECTO_ID, null, null));
    }

    @Test
    @DisplayName("GET con un período inválido o una fecha mal escrita responde 400")
    void descargarShouldReturn400ForInvalidPeriod() throws Exception {
        when(exportarResumenNegocio.execute(any(ExportarResumenRequest.class)))
                .thenThrow(new BusinessRuleException("La fecha desde no puede ser posterior a la fecha hasta"));

        mockMvc.perform(get("/api/proyectos/{id}/resumen/pdf", PROYECTO_ID).principal(sesion))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/proyectos/{id}/resumen/pdf", PROYECTO_ID).param("desde", "ayer").principal(sesion))
                .andExpect(status().isBadRequest());
    }
}
