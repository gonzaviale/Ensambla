package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.support.ProcesamientoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PropuestaLecturaTest {

    @Test
    @DisplayName("Una propuesta sin lista de campos nuevos (lectura guardada antes) la lee como vacía")
    void constructorShouldTreatNullNewFieldsAsEmpty() {
        PropuestaLectura propuesta = new PropuestaLectura(ProcesamientoTestData.lectura(), Map.of(), null);

        assertThat(propuesta.camposNuevos()).isEmpty();
    }

    @Test
    @DisplayName("Conserva los campos nuevos que se le pasan")
    void constructorShouldKeepNewFields() {
        CampoPropuesto telefono = new CampoPropuesto("telefono", "Teléfono", TipoCampo.TEXTO, List.of());

        PropuestaLectura propuesta = new PropuestaLectura(
                ProcesamientoTestData.lectura(), Map.of(), List.of(telefono));

        assertThat(propuesta.camposNuevos()).containsExactly(telefono);
    }

    @Test
    @DisplayName("El constructor corto deja los campos nuevos vacíos")
    void constructorShouldDefaultToNoNewFields() {
        PropuestaLectura propuesta = new PropuestaLectura(ProcesamientoTestData.lectura(), Map.of());

        assertThat(propuesta.camposNuevos()).isEmpty();
    }

    @Test
    @DisplayName("Se guarda y se lee como JSON, y una lectura guardada sin campos nuevos se sigue pudiendo leer")
    void jsonShouldRoundTripAndAcceptStoredProposalWithoutNewFields() {
        ObjectMapper objectMapper = JsonMapper.builder().build();
        CampoPropuesto telefono = new CampoPropuesto("telefono", "Teléfono", TipoCampo.TEXTO, List.of());
        PropuestaLectura propuesta = new PropuestaLectura(ProcesamientoTestData.lectura(),
                Map.of("monto", ProcesamientoTestData.CAMPO_ID), List.of(telefono));

        String json = objectMapper.writeValueAsString(propuesta);
        JsonNode guardadaAntes = objectMapper.readTree(json);
        ((ObjectNode) guardadaAntes).remove("camposNuevos");

        assertThat(objectMapper.readValue(json, PropuestaLectura.class)).isEqualTo(propuesta);
        assertThat(objectMapper.readValue(guardadaAntes.toString(), PropuestaLectura.class).camposNuevos()).isEmpty();
    }
}
