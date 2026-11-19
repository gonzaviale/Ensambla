package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.model.common.TextoNormalizado;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Lo que la persona escribió para pedir una métrica («¿cuánto vendí esta semana?»), listo para buscarle frases del
 * diccionario sin que importen las tildes, las mayúsculas ni los plurales.
 *
 * @param escritas cada palabra como la escribió la persona (en minúscula), a la par de su forma comparable
 * @param palabras cada palabra en su forma comparable, la que se busca en el diccionario
 */
public record PedidoEnPalabras(String texto, List<String> escritas, List<String> palabras) {

    private static final String SIGNOS_DE_LOS_BORDES = "^[\\s¿¡?!.,;:]+|[\\s¿¡?!.,;:]+$";

    private static final Pattern ENTRE_PALABRAS = Pattern.compile("[^\\p{L}\\p{M}\\p{N}]+");

    public static PedidoEnPalabras de(String texto) {
        List<String> trozos = ENTRE_PALABRAS.splitAsStream(texto).toList();
        return new PedidoEnPalabras(texto, comoSeEscribieron(trozos), enFormaComparable(trozos));
    }

    /** {@code true} si aparece alguna de las frases, completa y en orden. */
    public boolean menciona(List<String> frases) {
        return frases.stream().anyMatch(this::mencionaLaFrase);
    }

    /** La primera de las opciones, en su orden, que el pedido menciona con alguna de sus frases. */
    public <T extends Enum<T>> Optional<T> loPrimeroQueMenciona(T[] opciones, Map<T, List<String>> frasesDe) {
        return Arrays.stream(opciones)
                .filter(opcion -> menciona(frasesDe.getOrDefault(opcion, List.of())))
                .findFirst();
    }

    /** El pedido sin las frases dadas, para que lo que ellas dicen no se confunda con otra cosa. */
    public PedidoEnPalabras sin(List<String> frases) {
        Set<Integer> quitadas = posicionesDe(frases);
        IntPredicate queda = posicion -> !quitadas.contains(posicion);
        return new PedidoEnPalabras(texto, lasQue(queda, escritas), lasQue(queda, palabras));
    }

    /** Cómo escribió la persona cada una de las frases que menciona: «el finde» → «finde». */
    public List<String> loQueEscribioDe(List<String> frases) {
        return enFormaComparableSinVacias(frases)
                .flatMap(frase -> comienzosDe(frase).mapToObj(comienzo -> escritoDesde(comienzo, frase.size())))
                .distinct()
                .toList();
    }

    /** Las palabras, como las escribió la persona, que no forman parte de ninguna de las frases conocidas. */
    public List<String> palabrasFueraDe(List<String> conocidas) {
        Set<Integer> reconocidas = posicionesDe(conocidas);
        return lasQue(posicion -> !reconocidas.contains(posicion), escritas).stream().distinct().toList();
    }

    /** El pedido como nombre de la métrica: sin los signos de los bordes, con mayúscula y del largo permitido. */
    public String nombreSugerido() {
        String limpio = texto.replaceAll(SIGNOS_DE_LOS_BORDES, "");
        String recortado = limpio.substring(0, Math.min(limpio.length(), DefinicionMetrica.LARGO_MAXIMO_DEL_NOMBRE));
        return conMayuscula(recortado.strip());
    }

    private boolean mencionaLaFrase(String frase) {
        return TextoNormalizado.contiene(palabras, TextoNormalizado.palabras(frase));
    }

    private Set<Integer> posicionesDe(List<String> frases) {
        return enFormaComparableSinVacias(frases).flatMap(this::posicionesDeLaFrase).collect(Collectors.toSet());
    }

    private Stream<Integer> posicionesDeLaFrase(List<String> frase) {
        return comienzosDe(frase).boxed()
                .flatMap(comienzo -> IntStream.range(comienzo, comienzo + frase.size()).boxed());
    }

    private IntStream comienzosDe(List<String> frase) {
        return IntStream.rangeClosed(0, palabras.size() - frase.size())
                .filter(comienzo -> palabras.subList(comienzo, comienzo + frase.size()).equals(frase));
    }

    private String escritoDesde(int comienzo, int largo) {
        return String.join(" ", escritas.subList(comienzo, comienzo + largo));
    }

    private static Stream<List<String>> enFormaComparableSinVacias(List<String> frases) {
        return frases.stream().map(TextoNormalizado::palabras).filter(frase -> !frase.isEmpty());
    }

    private static List<String> lasQue(IntPredicate quedan, List<String> lista) {
        return IntStream.range(0, lista.size()).filter(quedan).mapToObj(lista::get).toList();
    }

    /** Un trozo da tantas palabras como formas comparables tiene, así las dos listas van a la par. */
    private static List<String> comoSeEscribieron(List<String> trozos) {
        return trozos.stream()
                .flatMap(trozo -> TextoNormalizado.palabras(trozo).stream().map(forma -> enMinuscula(trozo)))
                .toList();
    }

    private static List<String> enFormaComparable(List<String> trozos) {
        return trozos.stream().flatMap(trozo -> TextoNormalizado.palabras(trozo).stream()).toList();
    }

    private static String enMinuscula(String trozo) {
        return trozo.toLowerCase(Locale.ROOT);
    }

    private static String conMayuscula(String texto) {
        return texto.isEmpty() ? texto : texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }
}
