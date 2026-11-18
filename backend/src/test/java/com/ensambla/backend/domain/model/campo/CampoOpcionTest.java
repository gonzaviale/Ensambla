package com.ensambla.backend.domain.model.campo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CampoOpcionTest extends CampoDeCualquierTipoTest {

    @Override
    protected TipoCampo tipoEsperado() {
        return TipoCampo.OPCION;
    }

    @Override
    protected List<Object> valoresQueAcepta() {
        return List.of("Minorista", "Mayorista");
    }

    @Override
    protected List<Object> valoresQueRechaza() {
        return List.of("Otra", true);
    }

    @Override
    protected String motivoDelRechazo() {
        return "Elegí una de las opciones";
    }

    @Test
    @DisplayName("Conoce las opciones entre las que se elige")
    void conoceLasOpcionesEntreLasQueSeElige() {
        entoncesLasOpcionesSonMinoristaYMayorista();
    }

    private void entoncesLasOpcionesSonMinoristaYMayorista() {
        assertThat(unCampo().getOpciones()).containsExactly("Minorista", "Mayorista");
    }

    @Override
    protected List<String> textosLeidosQueAcepta() {
        return List.of("Minorista");
    }

    @Override
    protected List<String> textosLeidosQueRechaza() {
        return List.of("Otra");
    }
}
