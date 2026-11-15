package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.registro.RegistroId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.MiniObjetivoTestData.APORTE;
import static com.ensambla.backend.support.MiniObjetivoTestData.META_MINI;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.VALOR_INICIAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidato;
import static org.assertj.core.api.Assertions.assertThat;

class CandidatoMiniObjetivoTest {

    private static final RegistroId OTRO_PRODUCTO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");

    @Test
    @DisplayName("Con el texto de la IA cambia el título y la justificación, lo marca y no toca los números")
    void conTextoDeIaShouldReplaceOnlyTheText() {
        CandidatoMiniObjetivo result = candidato().conTextoDeIa(new TextoMiniObjetivo("Un título", "Una razón"));

        assertThat(result.titulo()).isEqualTo("Un título");
        assertThat(result.justificacion()).isEqualTo("Una razón");
        assertThat(result.textoConIa()).isTrue();
        assertThat(result.tipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(result.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(result.referencia()).isEqualTo(PRODUCTO);
        assertThat(result.valorInicial()).isEqualTo(VALOR_INICIAL);
        assertThat(result.meta()).isEqualTo(META_MINI);
        assertThat(result.aporteEstimado()).isEqualTo(APORTE);
    }

    @Test
    @DisplayName("Es el mismo cuando coinciden el tipo, el producto y la referencia")
    void esElMismoShouldBeTrueWhenTypeProductAndReferenceMatch() {
        assertThat(candidato().esElMismo(TipoMiniObjetivo.SUBIR_PRECIO, PRODUCTO_ID, PRODUCTO)).isTrue();
    }

    @Test
    @DisplayName("No es el mismo si cambia el tipo")
    void esElMismoShouldBeFalseWhenTypeDiffers() {
        assertThat(candidato().esElMismo(TipoMiniObjetivo.PONER_A_LA_VISTA, PRODUCTO_ID, PRODUCTO)).isFalse();
    }

    @Test
    @DisplayName("No es el mismo si cambia el producto")
    void esElMismoShouldBeFalseWhenProductDiffers() {
        assertThat(candidato().esElMismo(TipoMiniObjetivo.SUBIR_PRECIO, OTRO_PRODUCTO_ID, PRODUCTO)).isFalse();
    }

    @Test
    @DisplayName("No es el mismo si cambia la referencia")
    void esElMismoShouldBeFalseWhenReferenceDiffers() {
        assertThat(candidato().esElMismo(TipoMiniObjetivo.SUBIR_PRECIO, PRODUCTO_ID, "Otra yerba")).isFalse();
    }
}
