package com.ensambla.backend.domain.model.marca;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarcaModeloTest {

    private static final String ID_TEXTO = "c0000000-0000-4000-8000-000000000001";

    private KitDeMarcaId id;

    private Tipografia tipografia;

    private TonoDeMarca tono;

    @Test
    @DisplayName("El id del kit se crea desde un UUID, desde su texto o nuevo, y se escribe como el UUID")
    void kitDeMarcaIdShouldBuildFromEveryOrigin() {
        whenSeCreaElIdDesdeTexto();

        thenElIdSeEscribeComoSuUuid();
        thenElIdDesdeUuidEsElMismo();
        thenUnIdNuevoEsDistinto();
    }

    @Test
    @DisplayName("El id del kit exige un valor")
    void kitDeMarcaIdShouldRejectNull() {
        assertThatThrownBy(() -> KitDeMarcaId.de((UUID) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Cada tipografía conoce el nombre de su familia")
    void tipografiaShouldKnowItsFamily() {
        whenSeElige(Tipografia.PLAYFAIR_DISPLAY);

        assertThat(tipografia.familia()).isEqualTo("Playfair Display");
    }

    @Test
    @DisplayName("Cada tono explica cómo se escribe")
    void tonoShouldExplainHowToWrite() {
        whenSeEligeElTono(TonoDeMarca.PROFESIONAL);

        assertThat(tono.indicacion()).contains("formal");
    }

    private void whenSeCreaElIdDesdeTexto() {
        id = KitDeMarcaId.de(ID_TEXTO);
    }

    private void whenSeElige(Tipografia elegida) {
        tipografia = elegida;
    }

    private void whenSeEligeElTono(TonoDeMarca elegido) {
        tono = elegido;
    }

    private void thenElIdSeEscribeComoSuUuid() {
        assertThat(id).hasToString(ID_TEXTO);
    }

    private void thenElIdDesdeUuidEsElMismo() {
        assertThat(KitDeMarcaId.de(UUID.fromString(ID_TEXTO))).isEqualTo(id);
    }

    private void thenUnIdNuevoEsDistinto() {
        assertThat(KitDeMarcaId.nuevo()).isNotEqualTo(id);
    }
}
