package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

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
    private static final String MONTO = MONTO_ID.toString();
    private static final String PAGADO = PAGADO_ID.toString();
    private static final String CONCEPTO = CONCEPTO_ID.toString();
    private static final List<Campo> CAMPOS = List.of(unCampo(MONTO_ID, TipoCampo.MONEDA),
            unCampo(PAGADO_ID, TipoCampo.SI_NO), unCampo(CONCEPTO_ID, TipoCampo.TEXTO),
            unCampo(SIN_VALOR_ID, TipoCampo.TEXTO));
    private static final Map<String, Object> CINCO_DE_LECHE = Map.of(MONTO, new BigDecimal("5"), CONCEPTO, "Leche");
    private static final VinculoPropuesto MARTA_EN_CLIENTES = new VinculoPropuesto("Marta", null, "b-1", "Clientes");

    private FilaPropuesta fila;
    private FilaPropuesta otra;
    private Map<String, Object> resultado;

    @Test
    @DisplayName("Los números se convierten a número, con coma o con punto, y los sí/no a booleano")
    void losNumerosYLosSiNoSeConviertenASuTipo() {
        dadoUnaFilaNueva(Map.of(MONTO, " 5000,50 ", PAGADO, "Sí", CONCEPTO, " harina "));

        cuandoSeTipan();

        entoncesQueda(MONTO, new BigDecimal("5000.50"));
        entoncesQueda(PAGADO, Boolean.TRUE);
        entoncesQueda(CONCEPTO, "harina");
        entoncesNoTiene(SIN_VALOR_ID.toString());
    }

    @ParameterizedTest
    @CsvSource({"true,true", "FALSE,false", "no,false"})
    @DisplayName("Reconoce true, false y no como sí o no")
    void reconoceTrueFalseYNo(String escrito, boolean esperado) {
        dadoUnaFilaNueva(Map.of(PAGADO, escrito));

        cuandoSeTipan();

        entoncesQueda(PAGADO, esperado);
    }

    @ParameterizedTest
    @CsvSource({"PAGADO,tal vez", "MONTO,mucho"})
    @DisplayName("Lo que no entiende queda como texto para que el registro lo marque")
    void loQueNoEntiendeQuedaComoTexto(String campo, String escrito) {
        dadoUnaFilaNueva(Map.of(idDe(campo), escrito));

        cuandoSeTipan();

        entoncesQueda(idDe(campo), escrito);
    }

    @Test
    @DisplayName("Una fila nueva no actualiza ninguna existente")
    void unaFilaNuevaNoActualizaNingunaExistente() {
        dadoUnaFilaNueva(Map.of(MONTO, "3000"));

        entoncesEsUnaFilaNueva();
    }

    @Test
    @DisplayName("Actualizar una fila existente guarda su id y su nombre")
    void actualizarUnaFilaExistenteGuardaSuIdYSuNombre() {
        dadoUnaFilaNueva(Map.of(MONTO, "3000"));

        cuandoActualizaLaFila("r-1", "Harina 000", Map.of());

        entoncesActualizaLaFila("r-1", "Harina 000");
    }

    @Test
    @DisplayName("Al actualizar se cambia todo menos el nombre con el que se encontró la fila")
    void alActualizarSeCambiaTodoMenosElNombre() {
        dadoUnaFilaQueActualiza(Map.of(CONCEPTO, "harina", MONTO, "3000"), Map.of());

        cuandoSeCalculanLosCambios(Map.of());

        entoncesSoloCambian(MONTO);
        entoncesCambiaAlgoAdemasDelNombre(true);
    }

    @Test
    @DisplayName("Una fila que sólo trae el nombre no cambia nada")
    void unaFilaQueSoloTraeElNombreNoCambiaNada() {
        dadoUnaFilaNueva(Map.of(CONCEPTO, "harina"));

        entoncesCambiaAlgoAdemasDelNombre(false);
    }

    @ParameterizedTest
    @CsvSource({"+10,15", "- 3,2", "-8,0", "10,10"})
    @DisplayName("Un número con signo en una fila existente se suma o se resta a lo que tenía, sin bajar de 0")
    void unNumeroConSignoSeSumaOSeRestaALoQueTenia(String escrito, String queda) {
        dadoUnaFilaQueActualiza(Map.of(MONTO, escrito), CINCO_DE_LECHE);

        cuandoSeCalculanLosCambios(CINCO_DE_LECHE);

        entoncesQueda(MONTO, new BigDecimal(queda));
    }

    @Test
    @DisplayName("Un número con signo en una fila sin ese dato se suma a 0")
    void unNumeroConSignoEnUnaFilaSinEseDatoSeSumaACero() {
        dadoUnaFilaQueActualiza(Map.of(MONTO, "+4"), Map.of());

        cuandoSeCalculanLosCambios(Map.of());

        entoncesQueda(MONTO, new BigDecimal("4"));
    }

    @Test
    @DisplayName("Un signo en un campo que no es número no suma nada: queda como lo escribió")
    void unSignoEnUnCampoQueNoEsNumeroQuedaComoLoEscribio() {
        Map<String, Object> actuales = Map.of(CONCEPTO, "1");
        dadoUnaFilaNueva(Map.of(CONCEPTO, "+2"));
        dadoQueActualizaPorElNombre(MONTO, actuales);

        cuandoSeCalculanLosCambios(actuales);

        entoncesQueda(CONCEPTO, "+2");
    }

    @Test
    @DisplayName("Al actualizar recuerda lo que había sólo en los campos que se suman o se restan")
    void alActualizarRecuerdaSoloLoQueSeSumaOSeResta() {
        dadoUnaFilaNueva(Map.of(MONTO, "+10", PAGADO, "sí", SIN_VALOR_ID.toString(), "+1"));

        cuandoActualizaLaFila("r-1", "Leche", Map.of(MONTO, 5, PAGADO, true));

        entoncesRecuerdaQueHabia(MONTO, "5");
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL,3,3", "mucho,3,3", "'2,5',1,3.5"}, nullValues = "NULL")
    @DisplayName("Lo que había se toma como 0 si no es un número")
    void loQueHabiaSeTomaComoCeroSiNoEsUnNumero(String habia, String cambio, String queda) {
        entoncesSumadoQueda(habia, new BigDecimal(cambio), new BigDecimal(queda));
    }

    @ParameterizedTest
    @ValueSource(strings = {" +10 ", "-2,5"})
    @DisplayName("Un número con signo adelante es un cambio relativo")
    void unNumeroConSignoEsUnCambioRelativo(String valor) {
        entoncesEsRelativo(valor, true);
    }

    @ParameterizedTest
    @CsvSource(value = {"10", "+mucho", "NULL"}, nullValues = "NULL")
    @DisplayName("Un número sin signo, un texto o nada no son cambios relativos")
    void unNumeroSinSignoOUnTextoNoSonRelativos(String valor) {
        entoncesEsRelativo(valor, false);
    }

    @Test
    @DisplayName("Un vínculo se suma a la fila y un valor se puede quitar sin perder lo demás")
    void unVinculoSeSumaYUnValorSePuedeQuitar() {
        dadoUnaFilaNueva(Map.of(CONCEPTO, "Marta", MONTO, "3"));
        dadoQueSeVinculaCon(CONCEPTO, MARTA_EN_CLIENTES);

        cuandoSeLeQuita(MONTO);

        entoncesSoloTieneLosValores(CONCEPTO);
        entoncesSigueVinculadaCon(CONCEPTO, MARTA_EN_CLIENTES);
    }

    @Test
    @DisplayName("Un vínculo sin fila existente es nuevo")
    void unVinculoSinFilaExistenteEsNuevo() {
        entoncesElVinculoEsNuevo(MARTA_EN_CLIENTES);
    }

    @Test
    @DisplayName("Una fila guardada antes de los vínculos, sin esos datos, se lee igual")
    void unaFilaGuardadaAntesDeLosVinculosSeLeeIgual() {
        cuandoSeLeeUnaFilaSinDatosOpcionales();

        entoncesNoTieneNadaAntesNiVinculos();
    }

    private static Campo unCampo(CampoId id, TipoCampo tipo) {
        return Campo.builder().id(id).nombre("Campo").tipo(tipo).posicion((short) 0).build();
    }

    private static String idDe(String campo) {
        return Map.of("PAGADO", PAGADO, "MONTO", MONTO).get(campo);
    }

    private void dadoUnaFilaNueva(Map<String, String> valores) {
        fila = new FilaPropuesta(valores, Set.of());
    }

    private void dadoUnaFilaQueActualiza(Map<String, String> valores, Map<String, Object> actuales) {
        dadoUnaFilaNueva(valores);
        dadoQueActualizaPorElNombre(CONCEPTO, actuales);
    }

    private void dadoQueActualizaPorElNombre(String campoDelNombre, Map<String, Object> actuales) {
        fila = fila.actualizando("r-1", "Leche", campoDelNombre, actuales);
    }

    private void dadoQueSeVinculaCon(String campo, VinculoPropuesto vinculo) {
        fila = fila.vinculando(campo, vinculo);
    }

    private void cuandoSeTipan() {
        resultado = fila.tipados(CAMPOS);
    }

    private void cuandoActualizaLaFila(String id, String nombre, Map<String, Object> actuales) {
        otra = fila.actualizando(id, nombre, CONCEPTO, actuales);
    }

    private void cuandoSeCalculanLosCambios(Map<String, Object> actuales) {
        resultado = fila.cambiosTipados(CAMPOS, actuales);
    }

    private void cuandoSeLeQuita(String campo) {
        otra = fila.sinValor(campo);
    }

    private void cuandoSeLeeUnaFilaSinDatosOpcionales() {
        otra = new FilaPropuesta(Map.of(), Set.of(), null, null, null, null, null);
    }

    private void entoncesQueda(String campo, Object valor) {
        assertThat(resultado).containsEntry(campo, valor);
    }

    private void entoncesNoTiene(String campo) {
        assertThat(resultado).doesNotContainKey(campo);
    }

    private void entoncesSoloCambian(String... campos) {
        assertThat(resultado).containsOnlyKeys(campos);
    }

    private void entoncesEsUnaFilaNueva() {
        assertThat(fila.actualizaUnaFila()).isFalse();
    }

    private void entoncesActualizaLaFila(String id, String nombre) {
        assertThat(otra.actualizaUnaFila()).isTrue();
        assertThat(otra.registroId()).isEqualTo(id);
        assertThat(otra.nombreExistente()).isEqualTo(nombre);
    }

    private void entoncesCambiaAlgoAdemasDelNombre(boolean cambia) {
        assertThat(fila.cambiaAlgoAdemasDelNombre(CONCEPTO)).isEqualTo(cambia);
    }

    private void entoncesRecuerdaQueHabia(String campo, String valor) {
        assertThat(otra.antes()).containsExactly(Map.entry(campo, valor));
    }

    private static void entoncesSumadoQueda(String habia, BigDecimal cambio, BigDecimal queda) {
        assertThat(FilaPropuesta.sumado(habia, cambio)).isEqualTo(queda);
    }

    private static void entoncesEsRelativo(String valor, boolean esRelativo) {
        assertThat(FilaPropuesta.esRelativo(valor)).isEqualTo(esRelativo);
    }

    private void entoncesSoloTieneLosValores(String... campos) {
        assertThat(otra.valores()).containsOnlyKeys(campos);
    }

    private void entoncesSigueVinculadaCon(String campo, VinculoPropuesto vinculo) {
        assertThat(otra.vinculos()).containsEntry(campo, vinculo);
    }

    private static void entoncesElVinculoEsNuevo(VinculoPropuesto vinculo) {
        assertThat(vinculo.esNuevo()).isTrue();
    }

    private void entoncesNoTieneNadaAntesNiVinculos() {
        assertThat(otra.antes()).isEmpty();
        assertThat(otra.vinculos()).isEmpty();
    }
}