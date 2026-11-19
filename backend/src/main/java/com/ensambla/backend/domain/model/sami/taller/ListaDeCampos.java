package com.ensambla.backend.domain.model.sami.taller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Los datos de una pieza, con las preguntas que el taller les hace y las listas nuevas que salen de cambiarlos. */
public record ListaDeCampos(List<CampoTaller> campos) {

    /** El dato de la pieza con esa etiqueta, sin importar mayúsculas. */
    public Optional<CampoTaller> campoDe(String etiqueta) {
        return campos.stream().filter(campo -> campo.etiqueta().equalsIgnoreCase(etiqueta.strip())).findFirst();
    }

    /** La etiqueta tal como está en la pieza, si alguna coincide sin importar mayúsculas. */
    public Optional<String> etiquetaDe(String buscada) {
        return campoDe(buscada).map(CampoTaller::etiqueta);
    }

    public Optional<String> etiquetaDelPrimero() {
        return campos.stream().findFirst().map(CampoTaller::etiqueta);
    }

    public boolean esElPrimero(CampoTaller campo) {
        return campo.equals(campos.getFirst());
    }

    public int cantidad() {
        return campos.size();
    }

    /** El lugar del dato, que empieza en 1. */
    public int posicionDe(CampoTaller campo) {
        return campos.indexOf(campo) + 1;
    }

    public List<CampoTaller> con(CampoTaller campo) {
        List<CampoTaller> nuevos = new ArrayList<>(campos);
        nuevos.add(campo);
        return nuevos;
    }

    public List<CampoTaller> sin(CampoTaller campo) {
        List<CampoTaller> nuevos = new ArrayList<>(campos);
        nuevos.remove(campo);
        return nuevos;
    }

    /** Los datos con {@code campo} cambiado y en su lugar nuevo, para las llamadas siguientes del turno. */
    public List<CampoTaller> editando(CampoTaller campo, Map<String, Object> cambios) {
        List<CampoTaller> editados = new ArrayList<>(campos);
        int lugar = editados.indexOf(campo);
        editados.remove(lugar);
        CampoTaller editado = new CampoTaller(
                (String) cambios.getOrDefault("etiqueta", campo.etiqueta()),
                (String) cambios.getOrDefault("tipo", campo.tipo()));
        editados.add((int) cambios.getOrDefault("posicion", lugar + 1) - 1, editado);
        return editados;
    }

    /** Si ya había un dato común con esa etiqueta pasa a ser el vínculo; si no, el vínculo se suma al final. */
    public List<CampoTaller> vinculando(Optional<CampoTaller> existente, CampoTaller vinculo) {
        List<CampoTaller> nuevos = new ArrayList<>(campos);
        existente.ifPresentOrElse(campo -> nuevos.set(nuevos.indexOf(campo), vinculo), () -> nuevos.add(vinculo));
        return nuevos;
    }
}
