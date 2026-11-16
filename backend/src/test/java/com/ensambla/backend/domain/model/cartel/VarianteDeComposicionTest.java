package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.marca.KitDeMarca;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.CartelesDePrueba.cumpleElContrasteMinimo;
import static com.ensambla.backend.support.CartelesDePrueba.kitCon;
import static com.ensambla.backend.support.CartelesDePrueba.listaDe;
import static com.ensambla.backend.support.CartelesDePrueba.promoDeAceite;
import static com.ensambla.backend.support.CartelesDePrueba.textosDe;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Lo que cumple cualquier composición; cada variante concreta solo dice cuál es. */
abstract class VarianteDeComposicionTest {

    private ComposicionDeCartel composicion;

    protected abstract VarianteDeComposicion variante();

    @Test
    @DisplayName("La composición recuerda su variante y su formato, y trae todos los textos del cartel")
    void componerShouldKeepVarianteAndAllTheTexts() {
        whenSeComponeLaPromo(kitCon("#00506A", "#EE8000"), FormatoDeCartel.A4);

        assertThat(composicion.variante()).isEqualTo(variante());
        assertThat(composicion.formato()).isEqualTo(FormatoDeCartel.A4);
        assertThat(textosDe(composicion)).extracting(TextoDeCartel::texto)
                .contains("Aceite a precio de vecino", "$2.000", "20 % menos hasta el 22/11", "Don Rufino");
    }

    @Test
    @DisplayName("Todo texto se lee con la paleta de Ensambla")
    void componerShouldReadWithEnsamblaPalette() {
        whenSeComponeLaPromo(kitCon("#00506A", "#EE8000"), FormatoDeCartel.A4);

        assertThat(cumpleElContrasteMinimo(composicion)).isTrue();
    }

    @Test
    @DisplayName("Todo texto se lee aunque los colores del negocio sean apagados")
    void componerShouldReadWithDullColors() {
        whenSeComponeLaPromo(kitCon("#808080", "#FFF176"), FormatoDeCartel.VERTICAL);

        assertThat(cumpleElContrasteMinimo(composicion)).isTrue();
    }

    @Test
    @DisplayName("Todo texto se lee aunque los colores del negocio sean muy claros")
    void componerShouldReadWithVeryLightColors() {
        whenSeComponeLaPromo(kitCon("#FFF176", "#E0F7FA"), FormatoDeCartel.CUADRADO);

        assertThat(cumpleElContrasteMinimo(composicion)).isTrue();
    }

    @Test
    @DisplayName("Ningún texto baja del tamaño mínimo ni se sale de la hoja")
    void componerShouldKeepTextsBigAndInsideTheSheet() {
        whenSeComponeLaPromo(kitCon("#00506A", "#EE8000"), FormatoDeCartel.A5);

        assertThat(textosDe(composicion)).allSatisfy(texto -> {
            assertThat(texto.tamanio()).isGreaterThanOrEqualTo(FormatoDeCartel.A5.tamanioMinimoDeTexto());
            assertThat(texto.x()).isBetween(0.0, (double) FormatoDeCartel.A5.ancho());
            assertThat(texto.y()).isBetween(0.0, (double) FormatoDeCartel.A5.alto());
        });
    }

    @Test
    @DisplayName("Sin bajada, detalle ni nombre del negocio solo aparecen los textos que hay")
    void componerShouldSkipMissingTexts() {
        whenSeComponeElContenido(new ContenidoDeCartel("", "Horarios", "", "", "", List.of("Lun a vie 9 a 19")));

        assertThat(textosDe(composicion)).extracting(TextoDeCartel::texto)
                .containsExactly("Horarios", "Lun a vie 9 a 19");
    }

    @Test
    @DisplayName("Una lista de diez productos entra en una hoja A4")
    void componerShouldFitTenLinesInA4() {
        whenSeComponeElContenidoEn(listaDe(10), FormatoDeCartel.A4);

        assertThat(textosDe(composicion)).hasSize(10 + 2);
        assertThat(cumpleElContrasteMinimo(composicion)).isTrue();
    }

    @Test
    @DisplayName("Una lista de diez productos no entra legible en el cuadrado y se rechaza")
    void componerShouldRejectTooManyLinesForTheFormat() {
        assertThatThrownBy(() -> whenSeComponeElContenidoEn(listaDe(10), FormatoDeCartel.CUADRADO))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("demasiadas líneas");
    }

    private void whenSeComponeLaPromo(KitDeMarca kit, FormatoDeCartel formato) {
        composicion = variante().componer(kit, formato, promoDeAceite());
    }

    private void whenSeComponeElContenido(ContenidoDeCartel contenido) {
        whenSeComponeElContenidoEn(contenido, FormatoDeCartel.A4);
    }

    private void whenSeComponeElContenidoEn(ContenidoDeCartel contenido, FormatoDeCartel formato) {
        composicion = variante().componer(kitCon("#00506A", "#EE8000"), formato, contenido);
    }
}
