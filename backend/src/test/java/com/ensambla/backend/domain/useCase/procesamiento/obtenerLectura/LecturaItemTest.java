package com.ensambla.backend.domain.useCase.procesamiento.obtenerLectura;

import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lote.EstadoLote;
import com.ensambla.backend.domain.model.procesamiento.EstadoLectura;
import com.ensambla.backend.domain.model.procesamiento.PropuestaLectura;
import com.ensambla.backend.support.ProcesamientoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LecturaItemTest {

    @Test
    @DisplayName("Una lectura sin propuesta (todavía en proceso o con error) no trae propuesta")
    void deShouldHaveNoProposalWhenMissing() {
        LecturaItem item = LecturaItem.de(new EstadoLectura(
                ProcesamientoTestData.LOTE_ID, EstadoLote.ERROR, "Falló", 2, null));

        assertThat(item.propuesta()).isNull();
        assertThat(item.estado()).isEqualTo("ERROR");
        assertThat(item.motivoError()).isEqualTo("Falló");
    }

    @Test
    @DisplayName("Devuelve la propuesta con el mapeo y los campos nuevos detectados")
    void deShouldIncludeMappingAndNewFields() {
        CampoPropuesto telefono = new CampoPropuesto("telefono", "Teléfono", TipoCampo.TEXTO, List.of());
        PropuestaLectura propuesta = new PropuestaLectura(ProcesamientoTestData.lectura(),
                Map.of("monto", ProcesamientoTestData.CAMPO_ID), List.of(telefono));

        PropuestaItem item = LecturaItem.de(new EstadoLectura(
                ProcesamientoTestData.LOTE_ID, EstadoLote.LISTO, null, 1, propuesta)).propuesta();

        assertThat(item.mapeoCampos()).containsEntry("monto", ProcesamientoTestData.CAMPO_ID);
        assertThat(item.camposNuevos()).containsExactly(
                new PropuestaItem.CampoItem("telefono", "Teléfono", "TEXTO", List.of()));
        assertThat(item.campos()).extracting(PropuestaItem.CampoItem::clave).containsExactly("monto");
        assertThat(item.filas()).hasSize(1);
        assertThat(item.consumo().versionPrompt()).isEqualTo("v1");
    }

    @Test
    @DisplayName("Sin campos nuevos devuelve la lista vacía")
    void deShouldReturnEmptyNewFieldsWhenNone() {
        PropuestaItem item = LecturaItem.de(new EstadoLectura(ProcesamientoTestData.LOTE_ID, EstadoLote.LISTO,
                null, 1, ProcesamientoTestData.propuesta())).propuesta();

        assertThat(item.camposNuevos()).isEmpty();
    }
}
