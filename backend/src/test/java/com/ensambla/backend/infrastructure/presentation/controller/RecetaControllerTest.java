package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.useCase.receta.RecetaItem;
import com.ensambla.backend.domain.useCase.receta.guardarReceta.GuardarReceta;
import com.ensambla.backend.domain.useCase.receta.guardarReceta.GuardarRecetaRequest;
import com.ensambla.backend.domain.useCase.receta.guardarReceta.LineaRecetaRequest;
import com.ensambla.backend.domain.useCase.receta.obtenerReceta.ObtenerReceta;
import com.ensambla.backend.domain.useCase.receta.obtenerReceta.ObtenerRecetaRequest;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.receta.RecetaPresentationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecetaControllerTest {

    private static final UUID USUARIO_ID = UUID.fromString("b9000000-0000-4000-8000-000000000001");
    private static final UUID PRODUCTO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID HARINA_ID = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    private static final UUID PRECIO_ID = UUID.fromString("c0000000-0000-4000-8000-000000000002");
    private static final UUID BLOQUE_INSUMOS_ID = UUID.fromString("b0000000-0000-4000-8000-000000000002");
    private static final RecetaItem ITEM = new RecetaItem(
            BLOQUE_INSUMOS_ID,
            PRECIO_ID,
            List.of(new RecetaItem.LineaItem(
                    HARINA_ID, "Harina", new BigDecimal("0.5"), "kg", new BigDecimal("1200"), new BigDecimal("600"))),
            new BigDecimal("600"),
            false);

    private final Principal sesion = new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);

    private ObtenerReceta obtenerReceta;
    private GuardarReceta guardarReceta;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        obtenerReceta = mock(ObtenerReceta.class);
        guardarReceta = mock(GuardarReceta.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RecetaController(
                        obtenerReceta, guardarReceta, new RecetaPresentationMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET devuelve la receta con el costo y pasa el usuario de la sesión")
    void obtenerShouldReturnRecetaAndPassSessionUser() throws Exception {
        when(obtenerReceta.execute(any(ObtenerRecetaRequest.class))).thenReturn(ITEM);

        mockMvc.perform(get("/api/registros/{id}/receta", PRODUCTO_ID).principal(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bloqueInsumosId").value(BLOQUE_INSUMOS_ID.toString()))
                .andExpect(jsonPath("$.campoPrecioId").value(PRECIO_ID.toString()))
                .andExpect(jsonPath("$.costo").value(600))
                .andExpect(jsonPath("$.incompleta").value(false))
                .andExpect(jsonPath("$.lineas[0].insumo").value("Harina"))
                .andExpect(jsonPath("$.lineas[0].cantidad").value(0.5))
                .andExpect(jsonPath("$.lineas[0].precioUnitario").value(1200))
                .andExpect(jsonPath("$.lineas[0].subtotal").value(600));
        verify(obtenerReceta).execute(new ObtenerRecetaRequest(USUARIO_ID, PRODUCTO_ID));
    }

    @Test
    @DisplayName("GET de un producto ajeno responde 404")
    void obtenerShouldReturn404ForForeignProduct() throws Exception {
        when(obtenerReceta.execute(any(ObtenerRecetaRequest.class)))
                .thenThrow(new ResourceNotFoundException("El registro no existe"));

        mockMvc.perform(get("/api/registros/{id}/receta", PRODUCTO_ID).principal(sesion))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT guarda la receta con las líneas y devuelve el costo")
    void guardarShouldPassLinesAndReturnCost() throws Exception {
        when(guardarReceta.execute(any(GuardarRecetaRequest.class))).thenReturn(ITEM);
        String cuerpo = "{\"campoPrecioId\":\"" + PRECIO_ID + "\",\"lineas\":[{\"insumoId\":\"" + HARINA_ID
                + "\",\"cantidad\":0.5,\"unidad\":\"kg\"}]}";

        mockMvc.perform(put("/api/registros/{id}/receta", PRODUCTO_ID)
                        .principal(sesion).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costo").value(600));
        verify(guardarReceta).execute(new GuardarRecetaRequest(
                USUARIO_ID, PRODUCTO_ID, PRECIO_ID,
                List.of(new LineaRecetaRequest(HARINA_ID, new BigDecimal("0.5"), "kg"))));
    }

    @Test
    @DisplayName("PUT sin líneas quita la receta")
    void guardarWithoutLinesShouldRemoveTheReceta() throws Exception {
        when(guardarReceta.execute(any(GuardarRecetaRequest.class))).thenReturn(ITEM);

        mockMvc.perform(put("/api/registros/{id}/receta", PRODUCTO_ID)
                        .principal(sesion).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        verify(guardarReceta).execute(new GuardarRecetaRequest(USUARIO_ID, PRODUCTO_ID, null, List.of()));
    }

    @Test
    @DisplayName("PUT con datos inválidos responde 400 con el mensaje de cada línea")
    void guardarShouldReturn400WithLineErrors() throws Exception {
        when(guardarReceta.execute(any(GuardarRecetaRequest.class)))
                .thenThrow(new RegistroInvalidoException(Map.of("lineas[0]", "Escribí la cantidad")));

        mockMvc.perform(put("/api/registros/{id}/receta", PRODUCTO_ID)
                        .principal(sesion).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['lineas[0]']").value("Escribí la cantidad"));
    }
}
