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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TextosWhatsappTest {

    private static final BloqueId GASTOS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final CampoId CONCEPTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final CampoId MONTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    private static final String CONCEPTO = CONCEPTO_ID.toString();
    private static final String MONTO = MONTO_ID.toString();
    private static final TablaDisponible GASTOS = new TablaDisponible(new BloqueLectura(GASTOS_ID, "Gastos", List.of(
            new CampoConocido(CONCEPTO_ID, "Concepto", TipoCampo.TEXTO, List.of()),
            new CampoConocido(MONTO_ID, "Monto", TipoCampo.MONEDA, List.of()))), "Mi panadería");

    private List<FilaPropuesta> filas = List.of();
    private String texto;

    @Test
    @DisplayName("Sin tablas manda a armar el negocio y la foto sigue siendo la del cuaderno de papel")
    void losTextosUsanElVocabularioDeNegocios() {
        entoncesSinTablasMandaAArmarElNegocio();
        entoncesLaBienvenidaPideUnaFotoDelCuaderno();
        entoncesAlVincularHablaDelNegocio();
    }

    @Test
    @DisplayName("El resumen muestra cada fila con sus columnas y pide confirmar")
    void elResumenMuestraCadaFilaYPideConfirmar() {
        dadoGastosDeHarina(1, Set.of());

        cuandoSeResume();

        entoncesElTextoEmpiezaCon("Para *Gastos* (Mi panadería) entendí:");
        entoncesElTextoDice("1) Agrego: Concepto: harina", "Monto: 5000",
                "¿Lo guardo? Respondé *SÍ* para guardar o *NO* para descartar.");
        entoncesElTextoNoDice("(?)");
    }

    @Test
    @DisplayName("El resumen marca con (?) lo dudoso y lo explica una sola vez")
    void elResumenMarcaLoDudosoYLoExplicaUnaVez() {
        dadoGastosDeHarina(2, Set.of(MONTO));

        cuandoSeResume();

        entoncesElTextoDice("Monto: 5000 (?)");
        entoncesElTextoDiceUnaSolaVez("(?) = dato que no pude leer bien.");
    }

    @Test
    @DisplayName("El resumen muestra hasta 10 filas y cuenta las que quedaron afuera")
    void elResumenMuestraHastaDiezFilas() {
        dadoGastosDeHarina(13, Set.of());

        cuandoSeResume();

        entoncesElTextoDice("10) Agrego: Concepto", "y 3 filas más.");
        entoncesElTextoNoDice("11) Agrego");
    }

    @Test
    @DisplayName("La lista de tablas está numerada con proyecto y tabla")
    void laListaDeTablasEstaNumerada() {
        cuandoSePideElegirEntre(GASTOS);

        entoncesElTextoDice("Respondé con el número:", "1) Mi panadería", "Gastos", "O respondé NO para descartar.");
    }

    @ParameterizedTest
    @CsvSource({"1,0,Gastos,guardé 1 fila en *Gastos*", "3,0,Gastos,guardé 3 filas en *Gastos*",
            "0,1,Productos,actualicé 1 fila en *Productos*", "0,2,Productos,actualicé 2 filas en *Productos*",
            "2,3,Productos,actualicé 3 y agregué 2 filas en *Productos*"})
    @DisplayName("El aviso de guardado concuerda en singular y plural y distingue lo agregado de lo actualizado")
    void elAvisoDeGuardadoConcuerdaYDistingue(int nuevas, int actualizadas, String tabla, String esperado) {
        cuandoSeAvisaQueSeGuardaron(nuevas, actualizadas, tabla);

        entoncesElTextoDice(esperado);
    }

    @Test
    @DisplayName("El resumen dice qué fila existente se actualiza y cuáles se agregan")
    void elResumenDiceQueSeActualizaYQueSeAgrega() {
        dadoLasFilas(
                new FilaPropuesta(Map.of(MONTO, "3000"), Set.of())
                        .actualizando("r-1", "Harina 000", CONCEPTO, Map.of()),
                new FilaPropuesta(Map.of(CONCEPTO, "azúcar"), Set.of()));

        cuandoSeResume();

        entoncesElTextoDice("1) Actualizo *Harina 000*: Monto: 3000", "2) Agrego: Concepto: azúcar");
    }

    @Test
    @DisplayName("Lo que se suma a una fila existente muestra cuánto había y cómo queda")
    void loQueSeSumaMuestraCuantoHabiaYComoQueda() {
        dadoLasFilas(new FilaPropuesta(Map.of(MONTO, "+10"), Set.of())
                .actualizando("r-1", "Leche", CONCEPTO, Map.of(MONTO, 5)));

        cuandoSeResume();

        entoncesElTextoDice("1) Actualizo *Leche*: Monto: 5 → 15 (+10)");
    }

    @Test
    @DisplayName("Un vínculo muestra la fila que encontró, o avisa que la suma si es nueva")
    void unVinculoMuestraLaFilaQueEncontroOAvisaQueLaSuma() {
        dadoLasFilas(
                new FilaPropuesta(Map.of(CONCEPTO, "leche"), Set.of()).vinculando(CONCEPTO,
                        new VinculoPropuesto("Leche La Serenísima", "r-1", "b-1", "Productos")),
                new FilaPropuesta(Map.of(CONCEPTO, "Marta"), Set.of()).vinculando(CONCEPTO,
                        new VinculoPropuesto("Marta", null, "b-2", "Clientes")));

        cuandoSeResume();

        entoncesElTextoDice("1) Agrego: Concepto: Leche La Serenísima",
                "2) Agrego: Concepto: Marta (nuevo, lo sumo a Clientes)");
    }

    @Test
    @DisplayName("El aviso de guardado cuenta lo que se sumó para vincular y cómo quedó el stock")
    void elAvisoDeGuardadoCuentaLoVinculadoYElStock() {
        cuandoSeAvisaQueSeGuardaronVentasConStock();

        entoncesElTextoDice("guardé 2 filas en *Ventas*", "También sumé Marta en Clientes.",
                "Stock de Leche: 15 → 13.", "Stock de Yerba: 1 → 0. No alcanzaba para 3: lo dejé en 0.");
    }

    @Test
    @DisplayName("El mensaje de datos inválidos nombra cada campo con su problema")
    void elMensajeDeDatosInvalidosNombraCadaCampo() {
        cuandoSeAvisanLosDatosInvalidos(Map.of("id-1", "Tiene que ser un número"), Map.of("id-1", "Monto"));

        entoncesElTextoDice("Monto (tiene que ser un número)", "Mandame los datos de nuevo");
    }

    @Test
    @DisplayName("Un campo sin nombre conocido se nombra como dato")
    void unCampoSinNombreConocidoSeNombraComoDato() {
        cuandoSeAvisanLosDatosInvalidos(Map.of("id-9", "Falta"), Map.of());

        entoncesElTextoDice("dato (falta)");
    }

    @Test
    @DisplayName("El código de verificación va dentro del mensaje")
    void elCodigoDeVerificacionVaDentroDelMensaje() {
        cuandoSeMandaElCodigo("042917");

        entoncesElTextoDice("042917", "10 minutos");
    }

    private void dadoLasFilas(FilaPropuesta... propuestas) {
        filas = List.of(propuestas);
    }

    /** {@code cantidad} gastos de 5000: el primero es la harina y lleva los datos dudosos. */
    private void dadoGastosDeHarina(int cantidad, Set<String> dudosos) {
        Stream<FilaPropuesta> harina = Stream.of(new FilaPropuesta(Map.of(CONCEPTO, "harina", MONTO, "5000"), dudosos));
        Stream<FilaPropuesta> resto = IntStream.range(1, cantidad)
                .mapToObj(i -> new FilaPropuesta(Map.of(CONCEPTO, "item " + i, MONTO, "5000"), Set.of()));
        filas = Stream.concat(harina, resto).toList();
    }

    private void cuandoSeResume() {
        PropuestaWhatsapp propuesta = PropuestaWhatsapp.pendiente(NegocioId.de("a0000000-0000-4000-8000-000000000001"),
                "5491122334455", OrigenPropuestaWhatsapp.TEXTO, "gasté", GASTOS_ID, filas,
                OffsetDateTime.parse("2026-10-27T12:00:00Z"));
        texto = TextosWhatsapp.resumen(propuesta, GASTOS);
    }

    private void cuandoSePideElegirEntre(TablaDisponible... tablas) {
        texto = TextosWhatsapp.elegirTabla(List.of(tablas));
    }

    private void cuandoSeAvisaQueSeGuardaron(int nuevas, int actualizadas, String tabla) {
        texto = TextosWhatsapp.guardado(nuevas, actualizadas, tabla);
    }

    private void cuandoSeAvisaQueSeGuardaronVentasConStock() {
        texto = TextosWhatsapp.guardado(2, 0, "Ventas", List.of("Marta en Clientes"), List.of(
                MovimientoDeStock.aplicar("Leche", new BigDecimal("15"), new BigDecimal("2"), -1),
                MovimientoDeStock.aplicar("Yerba", new BigDecimal("1"), new BigDecimal("3"), -1)));
    }

    private void cuandoSeAvisanLosDatosInvalidos(Map<String, String> errores, Map<String, String> nombres) {
        texto = TextosWhatsapp.datosInvalidos(errores, nombres);
    }

    private void cuandoSeMandaElCodigo(String codigo) {
        texto = TextosWhatsapp.codigoVerificacion(codigo);
    }

    private void entoncesElTextoEmpiezaCon(String inicio) {
        assertThat(texto).startsWith(inicio);
    }

    private void entoncesElTextoDice(String... partes) {
        assertThat(texto).contains(partes);
    }

    private void entoncesElTextoNoDice(String parte) {
        assertThat(texto).doesNotContain(parte);
    }

    private void entoncesElTextoDiceUnaSolaVez(String parte) {
        assertThat(texto).containsOnlyOnce(parte);
    }

    private static void entoncesSinTablasMandaAArmarElNegocio() {
        assertThat(TextosWhatsapp.SIN_TABLAS).contains("Armá tu negocio en la app").doesNotContain("sistema");
    }

    private static void entoncesLaBienvenidaPideUnaFotoDelCuaderno() {
        assertThat(TextosWhatsapp.BIENVENIDA).contains("una foto de tu cuaderno");
    }

    private static void entoncesAlVincularHablaDelNegocio() {
        assertThat(TextosWhatsapp.VINCULADO).contains("tu negocio");
    }
}