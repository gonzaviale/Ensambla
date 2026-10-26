package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.useCase.plan.consultarPlanes.ConsultarPlanes;
import com.ensambla.backend.domain.useCase.plan.iniciarPrueba.IniciarPrueba;
import com.ensambla.backend.domain.useCase.plan.listarCatalogoPlanes.ListarCatalogoPlanes;
import com.ensambla.backend.domain.useCase.plan.listarCatalogoPlanes.ListarCatalogoPlanesResponse;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.plan.PlanPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.ensambla.backend.support.PlanTestData.plan;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogoPlanesControllerTest {

    private final ListarCatalogoPlanes listarCatalogoPlanes = mock(ListarCatalogoPlanes.class);
    private final MockMvc mockMvc = MockMvcDePrueba.standalone(new PlanController(mock(IniciarPrueba.class),
            new PlanPresentationMapper(), mock(ConsultarPlanes.class), listarCatalogoPlanes))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    @DisplayName("Entrega el catálogo sin suscripción, uso ni identificadores de negocio")
    void listarCatalogoShouldReturnOnlyCommercialFields() throws Exception {
        givenCatalogo();

        mockMvc.perform(get("/api/planes/catalogo"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$[0].codigo").value("GRATUITO"))
                .andExpect(jsonPath("$[0].id").doesNotExist())
                .andExpect(jsonPath("$[0].negocioId").doesNotExist())
                .andExpect(jsonPath("$[0].suscripcion").doesNotExist())
                .andExpect(jsonPath("$[0].uso").doesNotExist());

        verify(listarCatalogoPlanes).execute();
    }

    @Test
    @DisplayName("Responde 404 estándar si falta el catálogo")
    void listarCatalogoShouldReturn404WhenMissing() throws Exception {
        when(listarCatalogoPlanes.execute()).thenThrow(new ResourceNotFoundException("Plan no encontrado"));

        mockMvc.perform(get("/api/planes/catalogo"))
                .andExpect(status().isNotFound());
    }

    // --- arrange ---
    private void givenCatalogo() {
        when(listarCatalogoPlanes.execute()).thenReturn(ListarCatalogoPlanesResponse.de(List.of(plan())));
    }
}
