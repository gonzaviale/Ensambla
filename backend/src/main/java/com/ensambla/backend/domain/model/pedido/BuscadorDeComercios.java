package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.model.whatsapp.CoincidenciaDeFilas;

import java.util.List;

/**
 * Encuentra el comercio que el cliente nombró, aunque lo haya escrito como pueda (sin tildes, con una letra mal, sin el
 * «Almacén» o «Kiosco» del principio). Si es el nombre exacto de uno solo, ese; si varios se le parecen, devuelve todos
 * para que el cliente elija con un número.
 */
public final class BuscadorDeComercios {

    /** Cuántos comercios se le muestran como máximo: se elige respondiendo un número de una cifra. */
    public static final int MAX_OPCIONES = 9;

    private BuscadorDeComercios() {
    }

    /** @return los comercios que se parecen a lo escrito, los más parecidos primero; vacío si ninguno */
    public static List<Comercio> buscar(String texto, List<Comercio> comercios) {
        List<String> nombres = comercios.stream().map(Comercio::nombre).toList();
        return CoincidenciaDeFilas.coincidencias(texto, nombres).stream()
                .limit(MAX_OPCIONES)
                .map(comercios::get)
                .toList();
    }
}
