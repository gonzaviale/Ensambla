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

    private LecturaItem item;
    private PropuestaItem propuesta;

    @Test
    @DisplayName("Una lectura sin propuesta (todavía en proceso o con error) no trae propuesta")
    void unaLecturaSinPropuestaNoTraePropuesta() {
        cuandoSeArmaElItemDeUnaLecturaConError();

        entoncesElItemNoTraePropuestaYConservaElMotivoDelError();
    }

    @Test
    @DisplayName("Devuelve la propuesta con el mapeo y los campos nuevos detectados")
    void devuelveLaPropuestaConElMapeoYLosCamposNuevosDetectados() {
        cuandoSeArmaLaPropuestaConUnCampoNuevoDeTelefono();

        entoncesLaPropuestaTraeElMapeoYElCampoNuevo();
    }

    @Test
    @DisplayName("Sin campos nuevos devuelve la lista vacía")
    void sinCamposNuevosDevuelveLaListaVacia() {
        cuandoSeArmaLaPropuestaSinCamposNuevos();

        entoncesNoHayCamposNuevos();
    }

    // --- cuando ---
    private void cuandoSeArmaElItemDeUnaLecturaConError() {
        item = LecturaItem.de(new EstadoLectura(ProcesamientoTestData.LOTE_ID, EstadoLote.ERROR, "Falló", 2, null));
    }

    private void cuandoSeArmaLaPropuestaConUnCampoNuevoDeTelefono() {
        CampoPropuesto telefono = new CampoPropuesto("telefono", "Teléfono", TipoCampo.TEXTO, List.of());
        PropuestaLectura lectura = new PropuestaLectura(ProcesamientoTestData.lectura(),
                Map.of("monto", ProcesamientoTestData.CAMPO_ID), List.of(telefono));
        propuesta = LecturaItem.de(new EstadoLectura(
                ProcesamientoTestData.LOTE_ID, EstadoLote.LISTO, null, 1, lectura)).propuesta();
    }

    private void cuandoSeArmaLaPropuestaSinCamposNuevos() {
        propuesta = LecturaItem.de(new EstadoLectura(ProcesamientoTestData.LOTE_ID, EstadoLote.LISTO,
                null, 1, ProcesamientoTestData.propuesta())).propuesta();
    }

    // --- entonces ---
    private void entoncesElItemNoTraePropuestaYConservaElMotivoDelError() {
        assertThat(item.propuesta()).isNull();
        assertThat(item.estado()).isEqualTo("ERROR");
        assertThat(item.motivoError()).isEqualTo("Falló");
    }

    private void entoncesLaPropuestaTraeElMapeoYElCampoNuevo() {
        assertThat(propuesta.mapeoCampos()).containsEntry("monto", ProcesamientoTestData.CAMPO_ID);
        assertThat(propuesta.camposNuevos()).containsExactly(
                new PropuestaItem.CampoItem("telefono", "Teléfono", "TEXTO", List.of()));
        assertThat(propuesta.campos()).extracting(PropuestaItem.CampoItem::clave).containsExactly("monto");
        assertThat(propuesta.filas()).hasSize(1);
        assertThat(propuesta.consumo().versionPrompt()).isEqualTo("v1");
    }

    private void entoncesNoHayCamposNuevos() {
        assertThat(propuesta.camposNuevos()).isEmpty();
    }
}
