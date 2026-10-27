package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class TextosWhatsappTest {

    private static final BloqueId GASTOS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final CampoId CONCEPTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final CampoId MONTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    private static final TablaDisponible GASTOS = new TablaDisponible(new BloqueLectura(GASTOS_ID, "Gastos", List.of(
            new CampoConocido(CONCEPTO_ID, "Concepto", TipoCampo.TEXTO, List.of()),
            new CampoConocido(MONTO_ID, "Monto", TipoCampo.MONEDA, List.of()))), "Mi panadería");

    @Test
    @DisplayName("El resumen muestra cada fila con sus columnas y pide confirmar")
    void resumenShouldListRowsAndAskToConfirm() {
        String resumen = TextosWhatsapp.resumen(givenPropuesta(1, Set.of()), GASTOS);

        assertThat(resumen).startsWith("Para *Gastos* (Mi panadería) entendí:")
                .contains("1) Concepto: harina")
                .contains("Monto: 5000")
                .contains("¿Lo guardo? Respondé *SÍ* para guardar o *NO* para descartar.")
                .doesNotContain("(?)");
    }

    @Test
    @DisplayName("El resumen marca con (?) lo dudoso y lo explica una sola vez")
    void resumenShouldMarkDoubtfulValues() {
        String resumen = TextosWhatsapp.resumen(givenPropuesta(2, Set.of(MONTO_ID.toString())), GASTOS);

        assertThat(resumen).contains("Monto: 5000 (?)").containsOnlyOnce("(?) = dato que no pude leer bien.");
    }

    @Test
    @DisplayName("El resumen muestra hasta 10 filas y cuenta las que quedaron afuera")
    void resumenShouldTruncateAtTenRows() {
        String resumen = TextosWhatsapp.resumen(givenPropuesta(13, Set.of()), GASTOS);

        assertThat(resumen).contains("10) Concepto").doesNotContain("11) Concepto").contains("y 3 filas más.");
    }

    @Test
    @DisplayName("La lista de tablas está numerada con proyecto y tabla")
    void elegirTablaShouldNumberTheTables() {
        assertThat(TextosWhatsapp.elegirTabla(List.of(GASTOS)))
                .contains("Respondé con el número:", "1) Mi panadería", "Gastos", "O respondé NO para descartar.");
    }

    @Test
    @DisplayName("El aviso de guardado concuerda en singular y plural")
    void guardadoShouldAgreeInNumber() {
        assertThat(TextosWhatsapp.guardado(1, "Gastos")).contains("guardé 1 fila en *Gastos*");
        assertThat(TextosWhatsapp.guardado(3, "Gastos")).contains("guardé 3 filas en *Gastos*");
    }

    @Test
    @DisplayName("El mensaje de datos inválidos nombra cada campo con su problema")
    void datosInvalidosShouldNameEachField() {
        String texto = TextosWhatsapp.datosInvalidos(Map.of("id-1", "Tiene que ser un número"),
                Map.of("id-1", "Monto"));

        assertThat(texto).contains("Monto (tiene que ser un número)").contains("Mandame los datos de nuevo");
        assertThat(TextosWhatsapp.datosInvalidos(Map.of("id-9", "Falta"), Map.of())).contains("dato (falta)");
    }

    @Test
    @DisplayName("El código de verificación va dentro del mensaje")
    void codigoVerificacionShouldContainTheCode() {
        assertThat(TextosWhatsapp.codigoVerificacion("042917")).contains("042917").contains("10 minutos");
    }

    private static PropuestaWhatsapp givenPropuesta(int filas, Set<String> dudosos) {
        List<FilaPropuesta> lista = IntStream.range(0, filas).mapToObj(i -> new FilaPropuesta(
                Map.of(CONCEPTO_ID.toString(), i == 0 ? "harina" : "item " + i, MONTO_ID.toString(), "5000"),
                i == 0 ? dudosos : Set.of())).toList();
        return PropuestaWhatsapp.pendiente(NegocioId.de("a0000000-0000-4000-8000-000000000001"), "5491122334455",
                OrigenPropuestaWhatsapp.TEXTO, "gasté", GASTOS_ID, lista, OffsetDateTime.parse("2026-10-27T12:00:00Z"));
    }
}
