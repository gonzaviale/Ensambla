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

        FilaPropuesta actualiza = nueva.actualizando("r-1", "Harina 000", CONCEPTO_ID.toString(), Map.of());

        assertThat(nueva.actualizaUnaFila()).isFalse();
        assertThat(actualiza.actualizaUnaFila()).isTrue();
        assertThat(actualiza.registroId()).isEqualTo("r-1");
        assertThat(actualiza.nombreExistente()).isEqualTo("Harina 000");
    }

    @Test
    @DisplayName("Al actualizar se cambia todo menos el nombre con el que se encontró la fila")
    void cambiosShouldLeaveTheNameOut() {
        FilaPropuesta fila = new FilaPropuesta(Map.of(CONCEPTO_ID.toString(), "harina", MONTO_ID.toString(), "3000"),
                Set.of()).actualizando("r-1", "Harina 000", CONCEPTO_ID.toString(), Map.of());

        assertThat(fila.cambiosTipados(CAMPOS, Map.of())).containsOnlyKeys(MONTO_ID.toString());
        assertThat(fila.cambiaAlgoAdemasDelNombre(CONCEPTO_ID.toString())).isTrue();
        assertThat(new FilaPropuesta(Map.of(CONCEPTO_ID.toString(), "harina"), Set.of())
                .cambiaAlgoAdemasDelNombre(CONCEPTO_ID.toString())).isFalse();
    }

    @Test
    @DisplayName("Un número con signo en una fila existente se suma o se resta a lo que tenía, sin bajar de 0")
    void cambiosShouldAddRelativeNumbersToTheCurrentValue() {
        Map<String, Object> actuales = Map.of(MONTO_ID.toString(), new BigDecimal("5"), CONCEPTO_ID.toString(), "Leche");

        assertThat(cambios("+10", actuales)).containsEntry(MONTO_ID.toString(), new BigDecimal("15"));
        assertThat(cambios("- 3", actuales)).containsEntry(MONTO_ID.toString(), new BigDecimal("2"));
        assertThat(cambios("-8", actuales)).containsEntry(MONTO_ID.toString(), BigDecimal.ZERO);
        assertThat(cambios("10", actuales)).containsEntry(MONTO_ID.toString(), new BigDecimal("10"));
        assertThat(cambios("+4", Map.of())).containsEntry(MONTO_ID.toString(), new BigDecimal("4"));
    }

    @Test
    @DisplayName("Un signo en un campo que no es número no suma nada: queda como lo escribió")
    void cambiosShouldNotAddOnNonNumericFields() {
        FilaPropuesta fila = new FilaPropuesta(Map.of(CONCEPTO_ID.toString(), "+2"), Set.of())
                .actualizando("r-1", "Leche", MONTO_ID.toString(), Map.of(CONCEPTO_ID.toString(), "1"));

        assertThat(fila.cambiosTipados(CAMPOS, Map.of(CONCEPTO_ID.toString(), "1")))
                .containsEntry(CONCEPTO_ID.toString(), "+2");
    }

    @Test
    @DisplayName("Al actualizar recuerda lo que había sólo en los campos que se suman o se restan")
    void actualizandoShouldRememberThePreviousValueOfRelativeFields() {
        FilaPropuesta fila = new FilaPropuesta(Map.of(MONTO_ID.toString(), "+10", PAGADO_ID.toString(), "sí",
                SIN_VALOR_ID.toString(), "+1"), Set.of())
                .actualizando("r-1", "Leche", CONCEPTO_ID.toString(), Map.of(MONTO_ID.toString(), 5,
                        PAGADO_ID.toString(), true));

        assertThat(fila.antes()).containsExactly(Map.entry(MONTO_ID.toString(), "5"));
    }

    @Test
    @DisplayName("Lo que había se toma como 0 si no es un número")
    void sumadoShouldTreatNonNumbersAsZero() {
        assertThat(FilaPropuesta.sumado(null, new BigDecimal("3"))).isEqualTo(new BigDecimal("3"));
        assertThat(FilaPropuesta.sumado("mucho", new BigDecimal("3"))).isEqualTo(new BigDecimal("3"));
        assertThat(FilaPropuesta.sumado("2,5", new BigDecimal("1"))).isEqualTo(new BigDecimal("3.5"));
    }

    @Test
    @DisplayName("Sólo un número con signo adelante es un cambio relativo")
    void esRelativoShouldRecognizeSignedNumbers() {
        assertThat(FilaPropuesta.esRelativo(" +10 ")).isTrue();
        assertThat(FilaPropuesta.esRelativo("-2,5")).isTrue();
        assertThat(FilaPropuesta.esRelativo("10")).isFalse();
        assertThat(FilaPropuesta.esRelativo("+mucho")).isFalse();
        assertThat(FilaPropuesta.esRelativo(null)).isFalse();
    }

    @Test
    @DisplayName("Un vínculo se suma a la fila y un valor se puede quitar sin perder lo demás")
    void vinculandoAndSinValorShouldKeepTheRestOfTheRow() {
        VinculoPropuesto vinculo = new VinculoPropuesto("Marta", null, "b-1", "Clientes");
        FilaPropuesta fila = new FilaPropuesta(Map.of(CONCEPTO_ID.toString(), "Marta", MONTO_ID.toString(), "3"),
                Set.of()).vinculando(CONCEPTO_ID.toString(), vinculo);

        FilaPropuesta sinMonto = fila.sinValor(MONTO_ID.toString());

        assertThat(sinMonto.valores()).containsOnlyKeys(CONCEPTO_ID.toString());
        assertThat(sinMonto.vinculos()).containsEntry(CONCEPTO_ID.toString(), vinculo);
        assertThat(vinculo.esNuevo()).isTrue();
    }

    @Test
    @DisplayName("Una fila guardada antes de los vínculos, sin esos datos, se lee igual")
    void constructorShouldAcceptMissingOptionalMaps() {
        FilaPropuesta fila = new FilaPropuesta(Map.of(), Set.of(), null, null, null, null, null);

        assertThat(fila.antes()).isEmpty();
        assertThat(fila.vinculos()).isEmpty();
    }

    private static Map<String, Object> cambios(String monto, Map<String, Object> actuales) {
        return new FilaPropuesta(Map.of(MONTO_ID.toString(), monto), Set.of())
                .actualizando("r-1", "Leche", CONCEPTO_ID.toString(), actuales).cambiosTipados(CAMPOS, actuales);
    }

    private static Map<String, Object> conPagado(String valor) {
        return new FilaPropuesta(Map.of(PAGADO_ID.toString(), valor), Set.of()).tipados(CAMPOS);
    }

    private static Campo campo(CampoId id, TipoCampo tipo) {
        return Campo.builder().id(id).nombre("Campo").tipo(tipo).posicion((short) 0).build();
    }
}
