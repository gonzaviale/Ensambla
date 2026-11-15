package com.ensambla.backend.domain.model.campo;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Campo en el que se elige una de las opciones que definió la persona. */
public class CampoOpcion extends Campo {

    private final List<String> opciones;

    public CampoOpcion(CampoBuilder datos) {
        super(datos);
        this.opciones = datos.opciones;
    }

    @Override
    public TipoCampo tipo() {
        return TipoCampo.OPCION;
    }

    @Override
    public List<String> getOpciones() {
        return opciones;
    }

    @Override
    public Optional<String> errorDe(Object valor) {
        return errorSi(!esOpcion(valor), "Elegí una de las opciones");
    }

    @Override
    protected Optional<String> errorDeValorInicial(String valor) {
        return errorSi(!opciones.contains(valor), "Tiene que ser una de las opciones");
    }

    @Override
    protected Optional<String> errorDeOpciones() {
        if (opciones.isEmpty()) {
            return Optional.of("Agregá al menos una opción");
        }
        if (opciones.stream().anyMatch(Objects::isNull)) {
            return Optional.of("Las opciones no pueden quedar vacías");
        }
        return errorSi(hayOpcionesRepetidas(), "Hay opciones repetidas");
    }

    private boolean esOpcion(Object valor) {
        return valor instanceof String texto && opciones.contains(texto);
    }

    private boolean hayOpcionesRepetidas() {
        long distintas = opciones.stream().map(opcion -> opcion.toLowerCase(Locale.ROOT)).distinct().count();
        return distintas < opciones.size();
    }
}
