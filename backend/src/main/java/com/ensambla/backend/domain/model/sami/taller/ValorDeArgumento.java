package com.ensambla.backend.domain.model.sami.taller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Lo que el modelo mandó en un argumento: puede ser cualquier cosa y sólo se lee lo que vale. */
public record ValorDeArgumento(Object valor) {

    public Optional<String> texto(int maximo) {
        return Optional.ofNullable(valor)
                .filter(String.class::isInstance)
                .map(texto -> ((String) texto).strip())
                .filter(texto -> !texto.isEmpty() && texto.length() <= maximo);
    }

    public Optional<String> clave() {
        return texto(CatalogoTaller.MAX_LARGO_NOMBRE).map(texto -> texto.toLowerCase(Locale.ROOT));
    }

    public String comoTexto() {
        return String.valueOf(valor);
    }

    public List<String> textos() {
        return elementos().stream()
                .flatMap(elemento -> elemento.texto(CatalogoTaller.MAX_LARGO_NOMBRE).stream())
                .distinct()
                .toList();
    }

    public List<ValorDeArgumento> elementos() {
        if (!(valor instanceof List<?> lista)) {
            return List.of();
        }
        return lista.stream().map(ValorDeArgumento::new).toList();
    }

    /** Los elementos de una lista que son objetos JSON; cualquier otra cosa se ignora. */
    public List<ArgumentosDeHerramienta> objetos() {
        return elementos().stream().flatMap(elemento -> elemento.comoObjeto().stream()).toList();
    }

    /** Un objeto solo o una lista de objetos. */
    public List<ArgumentosDeHerramienta> objetosOUno() {
        return comoObjeto().map(List::of).orElseGet(this::objetos);
    }

    /** Sólo vale un objeto solo; una lista no. */
    public List<ArgumentosDeHerramienta> unObjeto() {
        return comoObjeto().map(List::of).orElse(List.of());
    }

    public Optional<ArgumentosDeHerramienta> comoObjeto() {
        if (!(valor instanceof Map<?, ?> mapa)) {
            return Optional.empty();
        }
        Map<String, Object> copia = new LinkedHashMap<>();
        mapa.forEach((clave, contenido) -> copia.put(String.valueOf(clave), contenido));
        return Optional.of(new ArgumentosDeHerramienta(copia));
    }

    /** Si el modelo mandó algo en un argumento optativo: un nulo, un texto en blanco o una lista vacía no cuentan. */
    public boolean llego() {
        return valor != null
                && !(valor instanceof String texto && texto.isBlank())
                && !(valor instanceof List<?> lista && lista.isEmpty());
    }

    public boolean esBooleano() {
        return valor instanceof Boolean;
    }

    public Optional<Long> entero() {
        return Optional.ofNullable(valor)
                .filter(Number.class::isInstance)
                .map(numero -> ((Number) numero).doubleValue())
                .filter(numero -> numero == Math.rint(numero))
                .map(Math::round);
    }

    public Optional<Object> valorSimple() {
        if (valor instanceof Boolean || valor instanceof Number) {
            return Optional.of(valor);
        }
        return texto(CatalogoTaller.MAX_LARGO_VALOR).map(Object.class::cast);
    }

    /** {@code 3} y {@code 3.0} son el mismo número: el modelo y el taller no siempre los escriben igual. */
    public boolean equivaleA(Object otro) {
        if (valor instanceof Number actual && otro instanceof Number nuevo) {
            return actual.doubleValue() == nuevo.doubleValue();
        }
        return Objects.equals(valor, otro);
    }

    /**
     * Filas (de ejemplo o registros) con etiquetas de {@code campos} y valores simples; las celdas inválidas se caen.
     * Una fila sin el dato principal de la pieza (el primero) también se cae: en la tabla se vería vacía. Quedan hasta
     * {@code maximo}.
     */
    public List<Map<String, Object>> filas(ListaDeCampos campos, int maximo) {
        Optional<String> principal = campos.etiquetaDelPrimero();
        return objetos().stream()
                .map(pedida -> pedida.comoFila(campos))
                .filter(fila -> principal.map(fila::containsKey).orElse(false))
                .limit(maximo)
                .toList();
    }
}
