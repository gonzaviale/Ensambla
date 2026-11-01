package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import com.ensambla.backend.domain.model.registro.MovimientoDeStock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
                .contains("1) Agrego: Concepto: harina")
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

        assertThat(resumen).contains("10) Agrego: Concepto").doesNotContain("11) Agrego").contains("y 3 filas más.");
    }

    @Test
    @DisplayName("La lista de tablas está numerada con proyecto y tabla")
    void elegirTablaShouldNumberTheTables() {
        assertThat(TextosWhatsapp.elegirTabla(List.of(GASTOS)))
                .contains("Respondé con el número:", "1) Mi panadería", "Gastos", "O respondé NO para descartar.");
    }

    @Test
    @DisplayName("El aviso de guardado concuerda en singular y plural y distingue lo agregado de lo actualizado")
    void guardadoShouldAgreeInNumberAndDistinguishUpdates() {
        assertThat(TextosWhatsapp.guardado(1, 0, "Gastos")).contains("guardé 1 fila en *Gastos*");
        assertThat(TextosWhatsapp.guardado(3, 0, "Gastos")).contains("guardé 3 filas en *Gastos*");
        assertThat(TextosWhatsapp.guardado(0, 1, "Productos")).contains("actualicé 1 fila en *Productos*");
        assertThat(TextosWhatsapp.guardado(0, 2, "Productos")).contains("actualicé 2 filas en *Productos*");
        assertThat(TextosWhatsapp.guardado(2, 3, "Productos")).contains("actualicé 3 y agregué 2 filas en *Productos*");
    }

    @Test
    @DisplayName("El resumen dice qué fila existente se actualiza y cuáles se agregan")
    void resumenShouldTellUpdatesFromAdditions() {
        FilaPropuesta actualiza = new FilaPropuesta(Map.of(MONTO_ID.toString(), "3000"), Set.of())
                .actualizando("r-1", "Harina 000", CONCEPTO_ID.toString(), Map.of());
        FilaPropuesta agrega = new FilaPropuesta(Map.of(CONCEPTO_ID.toString(), "azúcar"), Set.of());
        PropuestaWhatsapp propuesta = PropuestaWhatsapp.pendiente(NegocioId.de("a0000000-0000-4000-8000-000000000001"),
                "5491122334455", OrigenPropuestaWhatsapp.TEXTO, "x", GASTOS_ID, List.of(actualiza, agrega),
                OffsetDateTime.parse("2026-10-27T12:00:00Z"));

        assertThat(TextosWhatsapp.resumen(propuesta, GASTOS))
                .contains("1) Actualizo *Harina 000*: Monto: 3000").contains("2) Agrego: Concepto: azúcar");
    }

    @Test
    @DisplayName("Lo que se suma a una fila existente muestra cuánto había y cómo queda")
    void resumenShouldShowHowARelativeNumberEnds() {
        FilaPropuesta suma = new FilaPropuesta(Map.of(MONTO_ID.toString(), "+10"), Set.of())
                .actualizando("r-1", "Leche", CONCEPTO_ID.toString(), Map.of(MONTO_ID.toString(), 5));

        assertThat(TextosWhatsapp.resumen(givenPropuesta(List.of(suma)), GASTOS))
                .contains("1) Actualizo *Leche*: Monto: 5 → 15 (+10)");
    }

    @Test
    @DisplayName("Un vínculo muestra la fila que encontró, o avisa que la suma si es nueva")
    void resumenShouldShowLinkedRows() {
        FilaPropuesta encontrada = new FilaPropuesta(Map.of(CONCEPTO_ID.toString(), "leche"), Set.of())
                .vinculando(CONCEPTO_ID.toString(), new VinculoPropuesto("Leche La Serenísima", "r-1", "b-1",
                        "Productos"));
        FilaPropuesta nueva = new FilaPropuesta(Map.of(CONCEPTO_ID.toString(), "Marta"), Set.of())
                .vinculando(CONCEPTO_ID.toString(), new VinculoPropuesto("Marta", null, "b-2", "Clientes"));

        assertThat(TextosWhatsapp.resumen(givenPropuesta(List.of(encontrada, nueva)), GASTOS))
                .contains("1) Agrego: Concepto: Leche La Serenísima")
                .contains("2) Agrego: Concepto: Marta (nuevo, lo sumo a Clientes)");
    }

    @Test
    @DisplayName("El aviso de guardado cuenta lo que se sumó para vincular y cómo quedó el stock")
    void guardadoShouldTellLinkedRowsAndStock() {
        String texto = TextosWhatsapp.guardado(2, 0, "Ventas", List.of("Marta en Clientes"), List.of(
                MovimientoDeStock.aplicar("Leche", new BigDecimal("15"), new BigDecimal("2"), -1),
                MovimientoDeStock.aplicar("Yerba", new BigDecimal("1"), new BigDecimal("3"), -1)));

        assertThat(texto).contains("guardé 2 filas en *Ventas*", "También sumé Marta en Clientes.",
                "Stock de Leche: 15 → 13.", "Stock de Yerba: 1 → 0. No alcanzaba para 3: lo dejé en 0.");
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

    private static PropuestaWhatsapp givenPropuesta(List<FilaPropuesta> filas) {
        return PropuestaWhatsapp.pendiente(NegocioId.de("a0000000-0000-4000-8000-000000000001"), "5491122334455",
                OrigenPropuestaWhatsapp.TEXTO, "x", GASTOS_ID, filas, OffsetDateTime.parse("2026-10-27T12:00:00Z"));
    }

    private static PropuestaWhatsapp givenPropuesta(int filas, Set<String> dudosos) {
        List<FilaPropuesta> lista = IntStream.range(0, filas).mapToObj(i -> new FilaPropuesta(
                Map.of(CONCEPTO_ID.toString(), i == 0 ? "harina" : "item " + i, MONTO_ID.toString(), "5000"),
                i == 0 ? dudosos : Set.of())).toList();
        return PropuestaWhatsapp.pendiente(NegocioId.de("a0000000-0000-4000-8000-000000000001"), "5491122334455",
                OrigenPropuestaWhatsapp.TEXTO, "gasté", GASTOS_ID, lista, OffsetDateTime.parse("2026-10-27T12:00:00Z"));
    }
}
