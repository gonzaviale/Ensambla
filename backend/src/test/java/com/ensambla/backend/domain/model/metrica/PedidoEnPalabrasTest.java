package com.ensambla.backend.domain.model.metrica;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PedidoEnPalabrasTest {

    private static final String UN_PEDIDO_LARGUISIMO = "cuánto vendí " + "y cuánto más ".repeat(10);

    private PedidoEnPalabras pedido;

    @Test
    @DisplayName("Encuentra una frase aunque cambien las tildes, las mayúsculas y el plural")
    void encuentraUnaFraseSinImportarTildesNiPlurales() {
        dadoElPedido("¿Cuánto VENDÍ por productos?");

        entoncesMenciona(List.of("por producto"));
    }

    @Test
    @DisplayName("No encuentra una frase con las palabras desordenadas")
    void noEncuentraUnaFraseDesordenada() {
        dadoElPedido("productos por vendidos");

        entoncesNoMenciona(List.of("por producto"));
    }

    @Test
    @DisplayName("Entre varias opciones elige la primera, en su orden, que el pedido menciona")
    void eligeLaPrimeraOpcionQueSeMenciona() {
        dadoElPedido("cuánto gasté y cuánto vendí");

        entoncesLoPrimeroQueMencionaEs(Optional.of(FuenteMetrica.VENTAS));
    }

    @Test
    @DisplayName("Si no menciona ninguna opción no elige nada")
    void sinMencionarNingunaOpcionNoEligeNada() {
        dadoElPedido("hola");

        entoncesLoPrimeroQueMencionaEs(Optional.empty());
    }

    @Test
    @DisplayName("Sin una frase, el pedido deja de mencionarla")
    void sinUnaFraseDejaDeMencionarla() {
        dadoElPedido("ventas del mes pasado");

        cuandoSeLeQuita(List.of("mes pasado"));

        entoncesNoMenciona(List.of("mes"));
    }

    @Test
    @DisplayName("Dice cómo escribió la persona cada frase que menciona, sin contar las frases vacías")
    void diceComoSeEscribioCadaFrase() {
        dadoElPedido("Ventas del FINDE");

        entoncesEscribio(List.of("finde", "¿?", "fin de semana", "una frase más larga que todo el pedido"),
                List.of("finde"));
    }

    @Test
    @DisplayName("Avisa las palabras que no forman parte de ninguna frase conocida, una sola vez")
    void avisaLasPalabrasFueraDeLasConocidas() {
        dadoElPedido("ventas de feria y feria");

        entoncesQuedanFuera(List.of("ventas", "de", "y"), List.of("feria"));
    }

    @Test
    @DisplayName("El nombre sugerido es el pedido sin signos en los bordes y con mayúscula")
    void elNombreSugeridoEsElPedidoLimpio() {
        dadoElPedido("  ¿cuánto vendí hoy?  ");

        entoncesSeLlama("Cuánto vendí hoy");
    }

    @Test
    @DisplayName("El nombre sugerido no pasa el largo permitido para una métrica")
    void elNombreSugeridoNoPasaElLargoPermitido() {
        dadoElPedido(UN_PEDIDO_LARGUISIMO);

        entoncesElNombreTieneComoMucho(DefinicionMetrica.LARGO_MAXIMO_DEL_NOMBRE);
    }

    @Test
    @DisplayName("Un pedido hecho sólo de signos no sugiere nombre")
    void unPedidoDeSoloSignosNoSugiereNombre() {
        dadoElPedido("¿?");

        entoncesSeLlama("");
    }

    private void dadoElPedido(String texto) {
        pedido = PedidoEnPalabras.de(texto);
    }

    private void cuandoSeLeQuita(List<String> frases) {
        pedido = pedido.sin(frases);
    }

    private void entoncesEscribio(List<String> frases, List<String> escritas) {
        assertThat(pedido.loQueEscribioDe(frases)).isEqualTo(escritas);
    }

    private void entoncesQuedanFuera(List<String> conocidas, List<String> fuera) {
        assertThat(pedido.palabrasFueraDe(conocidas)).isEqualTo(fuera);
    }

    private void entoncesMenciona(List<String> frases) {
        assertThat(pedido.menciona(frases)).isTrue();
    }

    private void entoncesNoMenciona(List<String> frases) {
        assertThat(pedido.menciona(frases)).isFalse();
    }

    private void entoncesLoPrimeroQueMencionaEs(Optional<FuenteMetrica> esperada) {
        Map<FuenteMetrica, List<String>> frases = Map.of(
                FuenteMetrica.VENTAS, List.of("vendí"), FuenteMetrica.GASTOS, List.of("gasté"));
        assertThat(pedido.loPrimeroQueMenciona(FuenteMetrica.values(), frases)).isEqualTo(esperada);
    }

    private void entoncesSeLlama(String nombre) {
        assertThat(pedido.nombreSugerido()).isEqualTo(nombre);
    }

    private void entoncesElNombreTieneComoMucho(int largo) {
        assertThat(pedido.nombreSugerido()).hasSizeLessThanOrEqualTo(largo).startsWith("Cuánto vendí");
    }
}
