package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BloqueEntradasTest {

    private static final CampoId CANTIDAD_ID = CampoId.de("f0000000-0000-4000-8000-000000000042");

    private Registro registro;
    private BloqueEntradas entradas;
    private Object valorDeCantidad;
    private Object valorDeFecha;

    @Test
    @DisplayName("Lee el valor de una fila por la clave del campo, o nada si la tabla no tiene ese campo")
    void leeElValorDeUnaFilaPorLaClaveDelCampoONadaSiLaTablaNoTieneEseCampo() {
        dadoUnaFilaConCantidadEnUnaTablaSinCampoDeFecha();

        cuandoSeLeenLaCantidadYLaFecha();

        entoncesSeLeeLaCantidadYNadaDeLaFecha();
    }

    // --- dado ---
    private void dadoUnaFilaConCantidadEnUnaTablaSinCampoDeFecha() {
        Campo cantidad = Campo.builder().id(CANTIDAD_ID).clave("quantity").build();
        registro = Registro.builder().id(RegistroId.nuevo()).valores(Map.of(CANTIDAD_ID.toString(), 5)).build();
        entradas = new BloqueEntradas(Bloque.builder().build(), Map.of("quantity", cantidad), List.of(registro));
    }

    // --- cuando ---
    private void cuandoSeLeenLaCantidadYLaFecha() {
        valorDeCantidad = entradas.valorDe(registro, "quantity");
        valorDeFecha = entradas.valorDe(registro, "date");
    }

    // --- entonces ---
    private void entoncesSeLeeLaCantidadYNadaDeLaFecha() {
        assertThat(valorDeCantidad).isEqualTo(5);
        assertThat(valorDeFecha).isNull();
    }
}
