package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.procesamiento.BloqueDeImagen.CampoDeImagen;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class BloqueDeImagenTest {

    @Test
    @DisplayName("No se arma un bloque sin datos para leer")
    void deShouldRejectABlockWithoutFields() {
        assertThatThrownBy(() -> BloqueDeImagen.de("Ventas", List.of()))
                .isInstanceOf(BusinessRuleException.class).hasMessage("El bloque no tiene datos para leer");
    }

    @Test
    @DisplayName("Un campo sin opciones informadas no tiene opciones y cada campo recibe su propio id")
    void campoShouldDefaultToNoOptionsAndHaveItsOwnId() {
        CampoDeImagen sinOpciones = CampoDeImagen.nuevo("a", "A", TipoCampo.TEXTO, null);
        CampoDeImagen otro = CampoDeImagen.nuevo("a", "A", TipoCampo.TEXTO, List.of("x"));

        assertThat(sinOpciones.opciones()).isEmpty();
        assertThat(otro.opciones()).containsExactly("x");
        assertThat(sinOpciones.id()).isNotNull().isNotEqualTo(otro.id());
    }

    @Test
    @DisplayName("Para la IA el bloque va con el mismo id y sus campos por nombre, tipo y opciones")
    void paraElLectorShouldDescribeTheBlockWithoutTheKeys() {
        BloqueDeImagen bloque = bloque();

        BloqueConocido conocido = bloque.paraElLector();

        assertThat(conocido.id()).isEqualTo(bloque.id());
        assertThat(conocido.nombre()).isEqualTo("Ventas");
        assertThat(conocido.campos()).extracting("nombre", "tipo", "opciones")
                .containsExactly(tuple("Monto", TipoCampo.NUMERO, List.of()),
                        tuple("Estado", TipoCampo.OPCION, List.of("Pagó", "Debe")));
    }

    @Test
    @DisplayName("Para la propuesta el bloque lleva el id de cada campo")
    void paraLaPropuestaShouldCarryTheFieldIds() {
        BloqueDeImagen bloque = bloque();

        BloqueLectura lectura = bloque.paraLaPropuesta();

        assertThat(lectura.id()).isEqualTo(bloque.id());
        assertThat(lectura.nombre()).isEqualTo("Ventas");
        assertThat(lectura.campos()).extracting(CampoConocido::id)
                .containsExactly(bloque.campos().get(0).id(), bloque.campos().get(1).id());
        assertThat(lectura.campos()).extracting(CampoConocido::nombre).containsExactly("Monto", "Estado");
    }

    @Test
    @DisplayName("La clave de quien llama se encuentra por el id del campo")
    void clavesPorCampoShouldMapEachIdToItsKey() {
        BloqueDeImagen bloque = bloque();

        Map<CampoId, String> claves = bloque.clavesPorCampo();

        assertThat(claves).containsEntry(bloque.campos().get(0).id(), "importe")
                .containsEntry(bloque.campos().get(1).id(), "estado").hasSize(2);
    }

    // --- arrange ---
    private BloqueDeImagen bloque() {
        return BloqueDeImagen.de("Ventas", List.of(
                CampoDeImagen.nuevo("importe", "Monto", TipoCampo.NUMERO, List.of()),
                CampoDeImagen.nuevo("estado", "Estado", TipoCampo.OPCION, List.of("Pagó", "Debe"))));
    }
}
