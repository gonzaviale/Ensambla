package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.useCase.plantilla.listarPlantillas.ListarPlantillas;
import com.ensambla.backend.domain.useCase.plantilla.listarPlantillas.ListarPlantillasResponse;
import com.ensambla.backend.domain.useCase.plantilla.listarPlantillas.PlantillaItem;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.plantilla.PlantillaPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlantillaControllerTest {

    private ResultActions result;

    private static final PlantillaId PLANTILLA_ID = PlantillaId.de("c0000000-0000-4000-8000-000000000001");

    private ListarPlantillas listarPlantillas;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        listarPlantillas = mock(ListarPlantillas.class);
        mockMvc = MockMvcDePrueba.standalone(
                        new PlantillaController(listarPlantillas, new PlantillaPresentationMapper()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    @DisplayName("Lista las plantillas activas con su estructura")
    void listaLasPlantillasActivasConSuEstructura() throws Exception {
        dadoElEscenarioDeListaLasPlantillasActivasConSuEstructura();

        cuandoSeEjecutaListaLasPlantillasActivasConSuEstructura();

        entoncesSeVerificaListaLasPlantillasActivasConSuEstructura();
    }

    @Test
    @DisplayName("Devuelve una lista vacía cuando no hay plantillas")
    void devuelveUnaListaVaciaCuandoNoHayPlantillas() throws Exception {
        dadoElEscenarioDeDevuelveUnaListaVaciaCuandoNoHayPlantillas();

        cuandoSeEjecutaDevuelveUnaListaVaciaCuandoNoHayPlantillas();

        entoncesSeVerificaDevuelveUnaListaVaciaCuandoNoHayPlantillas();
    }

    // --- dado ---

    private void dadoElEscenarioDeListaLasPlantillasActivasConSuEstructura() throws Exception {
        when(listarPlantillas.execute()).thenReturn(new ListarPlantillasResponse(List.of(
                        new PlantillaItem(PLANTILLA_ID, "Inventario", "Controlá tu stock", "box",
                                Map.of("bloques", List.of())))));
    }

    private void dadoElEscenarioDeDevuelveUnaListaVaciaCuandoNoHayPlantillas() throws Exception {
        when(listarPlantillas.execute()).thenReturn(new ListarPlantillasResponse(List.of()));
    }

    // --- cuando ---

    private void cuandoSeEjecutaListaLasPlantillasActivasConSuEstructura() throws Exception {
        result = mockMvc.perform(get("/api/plantillas"));
    }

    private void cuandoSeEjecutaDevuelveUnaListaVaciaCuandoNoHayPlantillas() throws Exception {
        result = mockMvc.perform(get("/api/plantillas"));
    }

    // --- entonces ---

    private void entoncesSeVerificaListaLasPlantillasActivasConSuEstructura() throws Exception {
        result.andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].id").value(PLANTILLA_ID.toString()))
                        .andExpect(jsonPath("$[0].nombre").value("Inventario"))
                        .andExpect(jsonPath("$[0].icono").value("box"))
                        .andExpect(jsonPath("$[0].estructura.bloques").isArray());
    }

    private void entoncesSeVerificaDevuelveUnaListaVaciaCuandoNoHayPlantillas() throws Exception {
        result.andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }
}
