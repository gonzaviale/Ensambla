package com.ensambla.backend.domain.model.sami.taller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Los argumentos con que el modelo invocó una herramienta (o con que se armó una acción): claves de texto, valores sin garantías. */
public final class ArgumentosDeHerramienta {

    private static final String EJEMPLOS = "ejemplos";

    private final Map<String, Object> valores;

    public ArgumentosDeHerramienta(Map<String, Object> valores) {
        this.valores = valores;
    }

    public ValorDeArgumento de(String clave) {
        return new ValorDeArgumento(valores.get(clave));
    }

    public boolean contiene(String clave) {
        return valores.containsKey(clave);
    }

    public boolean estaVacio() {
        return valores.isEmpty();
    }

    public int cantidad() {
        return valores.size();
    }

    public Map<String, Object> comoMapa() {
        return valores;
    }

    /**
     * Las filas que inventa el modelo («ejemplos» de una pieza) no pasan nunca: se mostrarían como si fueran datos de
     * la persona. Los únicos ejemplos válidos son los del kit del catálogo, que se suman sin pasar por acá.
     */
    public ArgumentosDeHerramienta sinEjemplos() {
        Map<String, Object> limpios = new LinkedHashMap<>(valores);
        limpios.remove(EJEMPLOS);
        return new ArgumentosDeHerramienta(limpios);
    }

    /** Un dato nuevo con etiqueta que la pieza no tenga, tipo válido y, si es una lista, sus opciones. */
    public Optional<CampoNuevo> comoCampoNuevo(ListaDeCampos campos) {
        Optional<String> etiqueta = de("etiqueta").texto(CatalogoTaller.MAX_LARGO_NOMBRE)
                .filter(nueva -> campos.etiquetaDe(nueva).isEmpty());
        Optional<String> tipo = de("tipo").clave().filter(CatalogoTaller.TIPOS_DE_CAMPO::contains);
        List<String> opciones = opcionesDe(tipo);
        boolean sinDatos = etiqueta.isEmpty() || tipo.isEmpty();
        if (sinDatos || faltanOpciones(tipo, opciones)) {
            return Optional.empty();
        }
        return Optional.of(new CampoNuevo(etiqueta.get(), tipo.get(), opciones));
    }

    /** Los valores simples cuyas etiquetas son de {@code campos}, con la etiqueta tal como la tiene la pieza. */
    public Map<String, Object> comoFila(ListaDeCampos campos) {
        Map<String, Object> fila = new LinkedHashMap<>();
        valores.forEach((etiqueta, valor) -> campos.etiquetaDe(etiqueta)
                .ifPresent(existente -> new ValorDeArgumento(valor).valorSimple()
                        .ifPresent(dato -> fila.put(existente, dato))));
        return fila;
    }

    private List<String> opcionesDe(Optional<String> tipo) {
        return esLista(tipo) ? de("opciones").textos().stream().limit(CatalogoTaller.MAX_OPCIONES).toList() : List.of();
    }

    private boolean faltanOpciones(Optional<String> tipo, List<String> opciones) {
        return esLista(tipo) && opciones.size() < CatalogoTaller.MIN_OPCIONES;
    }

    private boolean esLista(Optional<String> tipo) {
        return tipo.filter(CatalogoTaller.TIPO_LISTA::equals).isPresent();
    }
}
