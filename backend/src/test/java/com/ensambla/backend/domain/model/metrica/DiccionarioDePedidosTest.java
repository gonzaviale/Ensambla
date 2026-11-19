package com.ensambla.backend.domain.model.metrica;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiccionarioDePedidosTest {

    private DiccionarioDePedidos diccionario;

    @Test
    @DisplayName("Un diccionario sin frases no reconoce nada, pero se puede consultar")
    void unDiccionarioSinFrasesNoReconoceNada() {
        cuandoSeArmaSinFrases();

        entoncesEstaVacio();
    }

    private void cuandoSeArmaSinFrases() {
        diccionario = new DiccionarioDePedidos(null, null, null, null, null, null, null);
    }

    private void entoncesEstaVacio() {
        assertThat(diccionario.fuentes()).isEmpty();
        assertThat(diccionario.calculos()).isEmpty();
        assertThat(diccionario.agrupaciones()).isEmpty();
        assertThat(diccionario.periodos()).isEmpty();
        assertThat(diccionario.fiado()).isEmpty();
        assertThat(diccionario.noSeCalculan()).isEmpty();
        assertThat(diccionario.frasesConocidas()).isEmpty();
    }
}
