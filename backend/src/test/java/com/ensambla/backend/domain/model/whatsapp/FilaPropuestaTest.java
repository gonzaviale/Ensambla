package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FilaPropuestaTest {

    private static final CampoId MONTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final CampoId PAGADO_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    private static final CampoId CONCEPTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000003");
    private static final CampoId SIN_VALOR_ID = CampoId.de("c0000000-0000-4000-8000-000000000004");
    private static final List<Campo> CAMPOS = List.of(campo(MONTO_ID, TipoCampo.MONEDA),
            campo(PAGADO_ID, TipoCampo.SI_NO), campo(CONCEPTO_ID, TipoCampo.TEXTO),
            campo(SIN_VALOR_ID, TipoCampo.TEXTO));

    @Test
    @DisplayName("Los números se convierten a número, con coma o con punto, y los sí/no a booleano")
    void tipadosShouldConvertNumbersAndBooleans() {
        FilaPropuesta fila = new FilaPropuesta(Map.of(MONTO_ID.toString(), " 5000,50 ",
                PAGADO_ID.toString(), "Sí", CONCEPTO_ID.toString(), " harina "), Set.of());

        Map<String, Object> tipados = fila.tipados(CAMPOS);

        assertThat(tipados).containsEntry(MONTO_ID.toString(), new BigDecimal("5000.50"))
                .containsEntry(PAGADO_ID.toString(), Boolean.TRUE).containsEntry(CONCEPTO_ID.toString(), "harina")
                .doesNotContainKey(SIN_VALOR_ID.toString());
    }

    @Test
    @DisplayName("Reconoce true, false y no; lo que no entiende queda como texto para que el registro lo marque")
    void tipadosShouldKeepWhatItCannotConvert() {
        assertThat(conPagado("true").get(PAGADO_ID.toString())).isEqualTo(Boolean.TRUE);
        assertThat(conPagado("FALSE").get(PAGADO_ID.toString())).isEqualTo(Boolean.FALSE);
        assertThat(conPagado("no").get(PAGADO_ID.toString())).isEqualTo(Boolean.FALSE);
        assertThat(conPagado("tal vez").get(PAGADO_ID.toString())).isEqualTo("tal vez");
        assertThat(new FilaPropuesta(Map.of(MONTO_ID.toString(), "mucho"), Set.of()).tipados(CAMPOS)
                .get(MONTO_ID.toString())).isEqualTo("mucho");
    }

    @Test
    @DisplayName("Actualizar una fila existente guarda su id y su nombre, y volverla nueva los olvida")
    void actualizandoShouldLinkTheExistingRow() {
        FilaPropuesta nueva = new FilaPropuesta(Map.of(MONTO_ID.toString(), "3000"), Set.of());

        FilaPropuesta actualiza = nueva.actualizando("r-1", "Harina 000", CONCEPTO_ID.toString());

        assertThat(nueva.actualizaUnaFila()).isFalse();
        assertThat(actualiza.actualizaUnaFila()).isTrue();
        assertThat(actualiza.registroId()).isEqualTo("r-1");
        assertThat(actualiza.nombreExistente()).isEqualTo("Harina 000");
    }

    @Test
    @DisplayName("Al actualizar se cambia todo menos el nombre con el que se encontró la fila")
    void cambiosShouldLeaveTheNameOut() {
        FilaPropuesta fila = new FilaPropuesta(Map.of(CONCEPTO_ID.toString(), "harina", MONTO_ID.toString(), "3000"),
                Set.of()).actualizando("r-1", "Harina 000", CONCEPTO_ID.toString());

        assertThat(fila.cambiosTipados(CAMPOS)).containsOnlyKeys(MONTO_ID.toString());
        assertThat(fila.cambiaAlgoAdemasDelNombre(CONCEPTO_ID.toString())).isTrue();
        assertThat(new FilaPropuesta(Map.of(CONCEPTO_ID.toString(), "harina"), Set.of())
                .cambiaAlgoAdemasDelNombre(CONCEPTO_ID.toString())).isFalse();
    }

    private static Map<String, Object> conPagado(String valor) {
        return new FilaPropuesta(Map.of(PAGADO_ID.toString(), valor), Set.of()).tipados(CAMPOS);
    }

    private static Campo campo(CampoId id, TipoCampo tipo) {
        return Campo.builder().id(id).nombre("Campo").tipo(tipo).posicion((short) 0).build();
    }
}
