package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.dto.aviso.AvisoItem;
import com.ensambla.backend.domain.dto.aviso.ContadorAvisos;
import com.ensambla.backend.domain.dto.aviso.ListarAvisosRequest;
import com.ensambla.backend.domain.dto.aviso.ResolverAvisoRequest;
import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.enums.EstadoAviso;
import com.ensambla.backend.domain.model.enums.GrupoAviso;
import com.ensambla.backend.domain.model.enums.TipoAviso;
import com.ensambla.backend.domain.useCase.aviso.AplicarAvisoUseCase;
import com.ensambla.backend.domain.useCase.aviso.ContarAvisosUseCase;
import com.ensambla.backend.domain.useCase.aviso.DescartarAvisoUseCase;
import com.ensambla.backend.domain.useCase.aviso.ListarAvisosUseCase;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.aviso.AvisoPresentationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AvisoControllerTest {

    private static final UUID USUARIO_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID AVISO_ID = UUID.fromString("e0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    private final ListarAvisosUseCase listarAvisosUseCase = mock(ListarAvisosUseCase.class);
    private final ContarAvisosUseCase contarAvisosUseCase = mock(ContarAvisosUseCase.class);
    private final AplicarAvisoUseCase aplicarAvisoUseCase = mock(AplicarAvisoUseCase.class);
    private final DescartarAvisoUseCase descartarAvisoUseCase = mock(DescartarAvisoUseCase.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new AvisoController(listarAvisosUseCase, contarAvisosUseCase, aplicarAvisoUseCase,
                descartarAvisoUseCase, new AvisoPresentationMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET lista los avisos del negocio con todos sus datos")
    void listarShouldReturnMappedAvisos() throws Exception {
        when(listarAvisosUseCase.execute(new ListarAvisosRequest(USUARIO_ID, null)))
                .thenReturn(List.of(givenItem(EstadoAviso.PENDIENTE)));

        mockMvc.perform(get("/api/avisos").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(AVISO_ID.toString()))
                .andExpect(jsonPath("$[0].tipo").value("STOCK"))
                .andExpect(jsonPath("$[0].grupo").value("STOCK"))
                .andExpect(jsonPath("$[0].prioridad").value(3))
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$[0].titulo").value("Te quedaste sin yerba"))
                .andExpect(jsonPath("$[0].motivos[0]").value("motivo"))
                .andExpect(jsonPath("$[0].acciones[0].tipo").value("ABRIR"))
                .andExpect(jsonPath("$[0].datos.cantidad").value(0));
    }

    @Test
    @DisplayName("GET con estado lista sólo los avisos de ese estado")
    void listarShouldPassEstadoFilter() throws Exception {
        when(listarAvisosUseCase.execute(new ListarAvisosRequest(USUARIO_ID, EstadoAviso.DESCARTADO)))
                .thenReturn(List.of(givenItem(EstadoAviso.DESCARTADO)));

        mockMvc.perform(get("/api/avisos").param("estado", "DESCARTADO").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("DESCARTADO"));
    }

    @Test
    @DisplayName("GET con un estado que no existe devuelve 400")
    void listarShouldRejectUnknownEstado() throws Exception {
        mockMvc.perform(get("/api/avisos").param("estado", "INVENTADO").principal(principal))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET contador devuelve los pendientes y los urgentes")
    void contarShouldReturnCounter() throws Exception {
        when(contarAvisosUseCase.execute(USUARIO_ID)).thenReturn(new ContadorAvisos(4, 1));

        mockMvc.perform(get("/api/avisos/contador").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendientes").value(4))
                .andExpect(jsonPath("$.urgentes").value(1));
    }

    @Test
    @DisplayName("POST aplicar devuelve el aviso aplicado")
    void aplicarShouldReturnAppliedAviso() throws Exception {
        when(aplicarAvisoUseCase.execute(new ResolverAvisoRequest(USUARIO_ID, AVISO_ID)))
                .thenReturn(givenItem(EstadoAviso.APLICADO));

        mockMvc.perform(post("/api/avisos/{id}/aplicar", AVISO_ID).principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APLICADO"));
    }

    @Test
    @DisplayName("POST aplicar un aviso ya resuelto devuelve 400")
    void aplicarShouldReturnBadRequestWhenAvisoWasResolved() throws Exception {
        when(aplicarAvisoUseCase.execute(new ResolverAvisoRequest(USUARIO_ID, AVISO_ID)))
                .thenThrow(new BusinessRuleException("El aviso ya fue resuelto"));

        mockMvc.perform(post("/api/avisos/{id}/aplicar", AVISO_ID).principal(principal))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El aviso ya fue resuelto"));
    }

    @Test
    @DisplayName("POST descartar devuelve el aviso descartado")
    void descartarShouldReturnDiscardedAviso() throws Exception {
        when(descartarAvisoUseCase.execute(new ResolverAvisoRequest(USUARIO_ID, AVISO_ID)))
                .thenReturn(givenItem(EstadoAviso.DESCARTADO));

        mockMvc.perform(post("/api/avisos/{id}/descartar", AVISO_ID).principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DESCARTADO"));
        verify(descartarAvisoUseCase).execute(new ResolverAvisoRequest(USUARIO_ID, AVISO_ID));
    }

    @Test
    @DisplayName("POST descartar un aviso ajeno devuelve 404")
    void descartarShouldReturnNotFoundForForeignAviso() throws Exception {
        when(descartarAvisoUseCase.execute(new ResolverAvisoRequest(USUARIO_ID, AVISO_ID)))
                .thenThrow(new ResourceNotFoundException("El aviso no existe"));

        mockMvc.perform(post("/api/avisos/{id}/descartar", AVISO_ID).principal(principal))
                .andExpect(status().isNotFound());
    }

    private static AvisoItem givenItem(EstadoAviso estado) {
        return new AvisoItem(AVISO_ID, null, TipoAviso.STOCK, GrupoAviso.STOCK, (short) 3, estado, "registro:1",
                "Te quedaste sin yerba", "No te quedan unidades", List.of("motivo"),
                List.of(Map.of("tipo", "ABRIR")), Map.of("cantidad", 0), AHORA, null);
    }
}
