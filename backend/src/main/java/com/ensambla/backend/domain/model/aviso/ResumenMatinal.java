package com.ensambla.backend.domain.model.aviso;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** El mensaje de WhatsApp con el que Sami cuenta, cada mañana, lo que encontró en el negocio. */
@RequiredArgsConstructor
public class ResumenMatinal {

    private static final String SALUDO = "Buen día";
    private static final String CIERRE = "Abrí Ensambla para ver los detalles.";

    private final List<SeccionResumen> secciones;

    /** El mensaje para la persona (sin nombre se la saluda sin nombre); vacío si no hay nada que contar. */
    public Optional<String> armar(String nombre, String negocio, List<Aviso> pendientes) {
        List<String> novedades = novedadesDe(pendientes);
        return novedades.isEmpty() ? Optional.empty() : Optional.of(mensaje(nombre, negocio, novedades));
    }

    private List<String> novedadesDe(List<Aviso> pendientes) {
        return secciones.stream()
                .map(seccion -> seccion.texto(pendientes))
                .flatMap(Optional::stream)
                .toList();
    }

    private String mensaje(String nombre, String negocio, List<String> novedades) {
        return saludo(nombre) + " Sami revisó " + negocio + " y encontró:\n" + listaDe(novedades) + "\n" + CIERRE;
    }

    private String saludo(String nombre) {
        return nombre == null ? SALUDO + "." : SALUDO + ", " + nombre + ".";
    }

    private String listaDe(List<String> novedades) {
        return novedades.stream().map(novedad -> "- " + novedad).collect(Collectors.joining("\n"));
    }
}
