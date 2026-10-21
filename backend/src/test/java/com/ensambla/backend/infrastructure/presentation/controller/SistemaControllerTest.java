package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.dto.sistema.EliminarSistemaRequest;
import com.ensambla.backend.domain.dto.sistema.GuardarSistemaRequest;
import com.ensambla.backend.domain.dto.sistema.ListarSistemasRequest;
import com.ensambla.backend.domain.dto.sistema.SistemaItem;
import com.ensambla.backend.domain.dto.sistema.SistemaItem.BloqueItem;
import com.ensambla.backend.domain.dto.sistema.SistemaItem.CampoItem;
import com.ensambla.backend.domain.dto.sistema.SistemaItem.OpcionesItem;
import com.ensambla.backend.domain.dto.sistema.SistemaItem.SeccionItem;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.useCase.sistema.EliminarSistemaUseCase;
import com.ensambla.backend.domain.useCase.sistema.GuardarSistemaUseCase;
import com.ensambla.backend.domain.useCase.sistema.ListarSistemasUseCase;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.sistema.SistemaPresentationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SistemaControllerTest {

    private static final UUID USUARIO_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final String SISTEMA_ID = "d0000000-0000-4000-8000-000000000001";

    private final ListarSistemasUseCase listarSistemasUseCase = mock(ListarSistemasUseCase.class);
    private final GuardarSistemaUseCase guardarSistemaUseCase = mock(GuardarSistemaUseCase.class);
    private final EliminarSistemaUseCase eliminarSistemaUseCase = mock(EliminarSistemaUseCase.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new SistemaController(listarSistemasUseCase, guardarSistemaUseCase,
                eliminarSistemaUseCase, new SistemaPresentationMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET lista los sistemas con la forma que valida el front y omite lo que falta")
    void listarShouldReturnDocumentShape() throws Exception {
        when(listarSistemasUseCase.execute(any())).thenReturn(List.of(givenItem()));

        mockMvc.perform(get("/api/sistemas").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(SISTEMA_ID))
                .andExpect(jsonPath("$[0].name").value("Mi almacén"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-01-10T09:00:00Z"))
                .andExpect(jsonPath("$[0].template").value("shop"))
                .andExpect(jsonPath("$[0].layout").value("mosaic"))
                .andExpect(jsonPath("$[0].options.movements").value(true))
                .andExpect(jsonPath("$[0].demoAlmacen").value(false))
                .andExpect(jsonPath("$[0].sections[0].name").value("Vista general"))
                .andExpect(jsonPath("$[0].sections[0].blocks[0].type").value("inventory"))
                .andExpect(jsonPath("$[0].sections[0].blocks[0].stockDirection").doesNotExist())
                .andExpect(jsonPath("$[0].sections[0].blocks[0].fields[0].key").value("name"))
                .andExpect(jsonPath("$[0].sections[0].blocks[0].fields[0].required").value(true))
                .andExpect(jsonPath("$[0].sections[0].blocks[0].fields[0].options").doesNotExist())
                .andExpect(jsonPath("$[0].sections[0].blocks[0].fields[1].min").value(0))
                .andExpect(jsonPath("$[0].sections[0].blocks[0].rows[0].name").value("Harina"))
                .andExpect(jsonPath("$[0].sections[0].blocks[0].rows[0].price").doesNotExist());
        verify(listarSistemasUseCase).execute(new ListarSistemasRequest(USUARIO_ID));
    }

    @Test
    @DisplayName("PUT guarda el sistema entero con el id de la URL y el usuario de la sesión")
    void guardarShouldPassDocumentAndPathId() throws Exception {
        when(guardarSistemaUseCase.execute(any())).thenReturn(givenItem());

        mockMvc.perform(put("/api/sistemas/{id}", "e" + SISTEMA_ID).principal(principal)
                        .contentType("application/json").content(givenBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SISTEMA_ID));

        ArgumentCaptor<GuardarSistemaRequest> captor = ArgumentCaptor.forClass(GuardarSistemaRequest.class);
        verify(guardarSistemaUseCase).execute(captor.capture());
        GuardarSistemaRequest request = captor.getValue();
        assertThat(request.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(request.id()).isEqualTo("e" + SISTEMA_ID);
        SistemaItem sistema = request.sistema();
        assertThat(sistema.template()).isEqualTo("shop");
        assertThat(sistema.options()).isEqualTo(new OpcionesItem(true, false));
        assertThat(sistema.demoAlmacen()).isTrue();
        BloqueItem bloque = sistema.sections().getFirst().blocks().getFirst();
        assertThat(bloque.fields()).singleElement().satisfies(campo -> {
            assertThat(campo.key()).isEqualTo("name");
            assertThat(campo.label()).isEqualTo("Producto");
            assertThat(campo.required()).isTrue();
            assertThat(campo.min()).isEqualByComparingTo("0");
        });
        assertThat(bloque.rows()).singleElement().satisfies(fila ->
                assertThat(fila).containsEntry("name", "Harina").containsEntry("price", 1200));
    }

    @Test
    @DisplayName("PUT acepta un sistema sin opciones ni demo")
    void guardarShouldAcceptMissingOptionalData() throws Exception {
        when(guardarSistemaUseCase.execute(any())).thenReturn(givenItem());

        mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).principal(principal)
                        .contentType("application/json")
                        .content("{\"id\":\"" + SISTEMA_ID + "\",\"name\":\"Solo\",\"sections\":"
                                + "[{\"id\":\"s1\",\"name\":\"Vista\",\"blocks\":[]}]}"))
                .andExpect(status().isOk());

        ArgumentCaptor<GuardarSistemaRequest> captor = ArgumentCaptor.forClass(GuardarSistemaRequest.class);
        verify(guardarSistemaUseCase).execute(captor.capture());
        assertThat(captor.getValue().sistema().options()).isNull();
        assertThat(captor.getValue().sistema().demoAlmacen()).isFalse();
    }

    @Test
    @DisplayName("PUT rechaza un sistema sin secciones sin tocar el caso de uso")
    void guardarShouldRejectSistemaWithoutSections() throws Exception {
        mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).principal(principal)
                        .contentType("application/json")
                        .content("{\"id\":\"" + SISTEMA_ID + "\",\"name\":\"Solo\",\"sections\":[]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(guardarSistemaUseCase);
    }

    @Test
    @DisplayName("PUT de un sistema ajeno responde 404")
    void guardarShouldReturnNotFoundForForeignSistema() throws Exception {
        when(guardarSistemaUseCase.execute(any())).thenThrow(new ResourceNotFoundException("Sistema no encontrado"));

        mockMvc.perform(put("/api/sistemas/{id}", SISTEMA_ID).principal(principal)
                        .contentType("application/json").content(givenBody()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE responde 204 y delega con el id de la URL")
    void eliminarShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/sistemas/{id}", "e" + SISTEMA_ID).principal(principal))
                .andExpect(status().isNoContent());

        verify(eliminarSistemaUseCase).execute(new EliminarSistemaRequest(USUARIO_ID, "e" + SISTEMA_ID));
    }

    // --- arrange ---
    private String givenBody() {
        return """
                {"id":"%s","name":"Mi almacén","createdAt":"2026-01-10T09:00:00Z","updatedAt":"2026-10-21T10:00:00Z",
                 "template":"shop","layout":"mosaic","options":{"movements":true,"expenses":false},"demoAlmacen":true,
                 "sections":[{"id":"s1","name":"Vista general","blocks":[
                   {"id":"b1","type":"inventory","name":"Productos","description":"","color":"orange","width":"half",
                    "fields":[{"key":"name","label":"Producto","type":"text","required":true,"min":0}],
                    "rows":[{"id":"r1","name":"Harina","price":1200}],"content":""}]}]}
                """.formatted(SISTEMA_ID);
    }

    private SistemaItem givenItem() {
        BloqueItem inventario = new BloqueItem("b1", "inventory", "Productos", "", "orange", "half",
                List.of(new CampoItem("name", "Producto", "text", true, null, null, null),
                        new CampoItem("price", "Precio", "currency", false, null, BigDecimal.ZERO, null)),
                List.of(Map.of("id", "r1", "name", "Harina")), "", false, null);
        return new SistemaItem(SISTEMA_ID, "Mi almacén", "2026-01-10T09:00:00Z", "2026-10-21T10:00:00Z",
                List.of(new SeccionItem("s1", "Vista general", List.of(inventario))), "shop",
                new OpcionesItem(true, false), "mosaic", false);
    }
}
