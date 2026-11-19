package com.ensambla.backend.domain.model.whatsapp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CoincidenciaDeFilasTest {

    private static final List<String> PRODUCTOS = List.of("Harina 000", "Azúcar impalpable", "Torta de chocolate",
            "Dulce de leche");
    private static final List<String> HARINAS = List.of("Harina 000", "Harina 0000", "Azúcar");
    private static final List<String> LECHE_Y_YERBA = List.of("Leche La Serenísima 1 L", "Yerba Playadito 1 kg");
    private static final List<String> SOLO_LECHE = List.of("Leche La Serenísima 1 L");

    private List<String> filas = PRODUCTOS;
    private Optional<Integer> encontrada;

    @Test
    @DisplayName("Encuentra el nombre exacto sin importar tildes ni mayúsculas")
    void encuentraElNombreExactoSinImportarTildesNiMayusculas() {
        cuandoSeBusca("AZUCAR IMPALPABLE");

        entoncesEncuentraLaFila(1);
    }

    @ParameterizedTest
    @CsvSource({"harina,0", "torta chocolate,2"})
    @DisplayName("Encuentra el producto si la persona escribió sólo parte del nombre")
    void encuentraElProductoConSoloParteDelNombre(String buscado, int fila) {
        cuandoSeBusca(buscado);

        entoncesEncuentraLaFila(fila);
    }

    @ParameterizedTest
    @CsvSource({"tortas,2", "hariana,0", "dulse de leche,3"})
    @DisplayName("Tolera plural, singular y una letra mal escrita")
    void toleraPluralSingularYUnaLetraMalEscrita(String buscado, int fila) {
        cuandoSeBusca(buscado);

        entoncesEncuentraLaFila(fila);
    }

    @Test
    @DisplayName("Una palabra que empieza igual que la del producto cuenta como la misma")
    void unaPalabraQueEmpiezaIgualCuentaComoLaMisma() {
        cuandoSeBusca("choco");

        entoncesEncuentraLaFila(2);
    }

    @Test
    @DisplayName("También coincide cuando la persona escribió la palabra más larga que la de la base")
    void coincideCuandoLaPalabraEscritaEsMasLarga() {
        dadoLasFilas("Choco", "Pan");

        cuandoSeBusca("chocolate");

        entoncesEncuentraLaFila(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"manteca", "de la"})
    @DisplayName("Si ningún producto se parece no elige ninguno")
    void siNingunProductoSeParecenNoEligeNinguno(String buscado) {
        cuandoSeBusca(buscado);

        entoncesNoEligeNinguna();
    }

    @Test
    @DisplayName("Si dos productos parecen igual de buenos no elige ninguno, para no pisar el equivocado")
    void siDosProductosEmpatanNoEligeNinguno() {
        dadoLasFilas(HARINAS);

        cuandoSeBusca("harina");

        entoncesNoEligeNinguna();
    }

    @Test
    @DisplayName("Entre dos productos parecidos elige el que tiene el nombre completo")
    void entreDosParecidosEligeElDelNombreCompleto() {
        dadoLasFilas(HARINAS);

        cuandoSeBusca("harina 000");

        entoncesEncuentraLaFila(0);
    }

    @Test
    @DisplayName("Un texto vacío no coincide con nada")
    void unTextoVacioNoCoincideConNada() {
        cuandoSeBusca("");

        entoncesNoEligeNinguna();
    }

    @Test
    @DisplayName("Sin candidatos no coincide con nada")
    void sinCandidatosNoCoincideConNada() {
        dadoLasFilas();

        cuandoSeBusca("harina");

        entoncesNoEligeNinguna();
    }

    @Test
    @DisplayName("Una fila sin nombre no coincide con nada")
    void unaFilaSinNombreNoCoincideConNada() {
        dadoLasFilas("");

        cuandoSeBusca("harina");

        entoncesNoEligeNinguna();
    }

    @Test
    @DisplayName("Una palabra corta con una letra de diferencia no se confunde con otra")
    void unaPalabraCortaConUnaLetraDistintaNoSeConfunde() {
        dadoLasFilas("Sol", "Mal");

        cuandoSeBusca("sal");

        entoncesNoEligeNinguna();
    }

    @ParameterizedTest
    @ValueSource(strings = {"leche la serenisima de 1 L", "leche serenisima 1l", "lechee serenissimaa"})
    @DisplayName("Encuentra el producto con la medida pegada al número, sin los «de» ni «la» y con letras de más")
    void encuentraElProductoComoSeEscribeEnWhatsapp(String buscado) {
        dadoLasFilas(LECHE_Y_YERBA);

        cuandoSeBusca(buscado);

        entoncesEncuentraLaFila(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"leche sancor 1 l", "leche serenisima 2 l"})
    @DisplayName("No confunde otra marca ni otra medida aunque compartan casi todo el nombre")
    void noConfundeOtraMarcaNiOtraMedida(String buscado) {
        dadoLasFilas(SOLO_LECHE);

        cuandoSeBusca(buscado);

        entoncesNoEligeNinguna();
    }

    @Test
    @DisplayName("No confunde un número con otro que lo contiene")
    void noConfundeUnNumeroConOtroQueLoContiene() {
        dadoLasFilas("Harina 5000");

        cuandoSeBusca("harina 500");

        entoncesNoEligeNinguna();
    }

    @Test
    @DisplayName("Prefiere el nombre completo a uno que comparte sólo una parte")
    void prefiereElNombreCompleto() {
        dadoLasFilas("Torta de chocolate", "Torta");

        cuandoSeBusca("torta chocolate");

        entoncesEncuentraLaFila(0);
    }

    @ParameterizedTest
    @CsvSource({"torta grande rica,Torta", "pan l,Pan kg cm"})
    @DisplayName("Si sobra más de la mitad de lo que escribió la persona no coincide, aunque sean palabras cortas")
    void siSobraMasDeLaMitadNoCoincide(String buscado, String fila) {
        dadoLasFilas(fila);

        cuandoSeBusca(buscado);

        entoncesNoEligeNinguna();
    }

    @Test
    @DisplayName("Con mucho en común pero no todo, coincide si comparten al menos la mitad de las palabras")
    void coincideSiCompartenAlMenosLaMitadDeLasPalabras() {
        dadoLasFilas("Torta chocolate", "Pan");

        cuandoSeBusca("torta chocolate grande");

        entoncesEncuentraLaFila(0);
    }

    private void dadoLasFilas(String... nombres) {
        filas = List.of(nombres);
    }

    private void dadoLasFilas(List<String> nombres) {
        filas = nombres;
    }

    private void cuandoSeBusca(String buscado) {
        encontrada = CoincidenciaDeFilas.buscar(buscado, filas);
    }

    private void entoncesEncuentraLaFila(int posicion) {
        assertThat(encontrada).contains(posicion);
    }

    private void entoncesNoEligeNinguna() {
        assertThat(encontrada).isEmpty();
    }
}