package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.dto.plan.PlanItem;
import com.ensambla.backend.domain.useCase.plan.IniciarPruebaUseCase;
import com.ensambla.backend.domain.useCase.plan.ConsultarPlanesUseCase;
import com.ensambla.backend.infrastructure.presentation.mapper.plan.PlanPresentationMapper;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.useCase.plan.ListarCatalogoPlanesUseCase;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.plan.CatalogoPlanesPresentationMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogoPlanesControllerTest {

    private final ListarCatalogoPlanesUseCase listarCatalogoPlanesUseCase = mock(ListarCatalogoPlanesUseCase.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new PlanController(
            mock(IniciarPruebaUseCase.class), new PlanPresentationMapper(), mock(ConsultarPlanesUseCase.class),
            listarCatalogoPlanesUseCase, new CatalogoPlanesPresentationMapper()))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    @DisplayName("Devuelve solo el catálogo sin sesión ni información de negocio")
    void listarShouldReturnPublicCatalog() throws Exception {
        givenCatalogo();

        mockMvc.perform(get("/api/planes/catalogo"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$[0].codigo").value("GRATUITO"))
                .andExpect(jsonPath("$[0].suscripcion").doesNotExist())
                .andExpect(jsonPath("$[0].uso").doesNotExist())
                .andExpect(jsonPath("$[0].negocioId").doesNotExist());

        verify(listarCatalogoPlanesUseCase).execute();
    }

    @Test
    @DisplayName("Un catálogo incompleto responde con el error estándar")
    void listarShouldReturn404WhenCatalogIsMissing() throws Exception {
        when(listarCatalogoPlanesUseCase.execute()).thenThrow(new ResourceNotFoundException("Plan no encontrado"));

        mockMvc.perform(get("/api/planes/catalogo"))
                .andExpect(status().isNotFound());
    }

    // --- arrange ---
    private void givenCatalogo() {
        when(listarCatalogoPlanesUseCase.execute()).thenReturn(List.of(new PlanItem("GRATUITO", "Gratuito",
                true, true, BigDecimal.ZERO, "ARS", 1, 1000L, 10, List.of())));
    }
}
