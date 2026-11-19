package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.PropuestaLectura;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import com.ensambla.backend.domain.model.whatsapp.FilaPropuesta;
import com.ensambla.backend.domain.model.whatsapp.InterpretacionRegistros;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Las filas que salen de lo que la IA entendió de un texto o leyó de una foto, con sólo lo que se puede cargar. */
@RequiredArgsConstructor
final class FilasDeLaIa {

    private final double umbralConfianza;

    List<FilaPropuesta> deTexto(InterpretacionRegistros interpretacion, TablaDisponible tabla) {
        Set<CampoId> cargables = camposCargables(tabla);
        return interpretacion.filas().stream()
                .map(fila -> filaDeTexto(fila, cargables))
                .filter(FilasDeLaIa::tieneValores)
                .toList();
    }

    List<FilaPropuesta> deFoto(PropuestaLectura propuesta, TablaDisponible tabla) {
        Set<CampoId> cargables = camposCargables(tabla);
        return propuesta.lectura().filas().stream()
                .map(fila -> filaDeFoto(fila, propuesta.mapeoCampos(), cargables))
                .filter(FilasDeLaIa::tieneValores)
                .toList();
    }

    private FilaPropuesta filaDeTexto(Map<CampoId, String> fila, Set<CampoId> cargables) {
        Map<String, String> valores = new LinkedHashMap<>();
        fila.forEach((campoId, valor) -> {
            if (sePuedeCargar(campoId, valor, cargables)) {
                valores.put(campoId.toString(), valor);
            }
        });
        return new FilaPropuesta(valores, Set.of());
    }

    private FilaPropuesta filaDeFoto(FilaLeida fila, Map<String, CampoId> mapeo, Set<CampoId> cargables) {
        Map<String, String> valores = new LinkedHashMap<>();
        Set<String> dudosos = new HashSet<>();
        fila.datos().forEach((clave, dato) -> {
            CampoId campoId = mapeo.get(clave);
            if (sePuedeCargar(campoId, dato.valor(), cargables)) {
                valores.put(campoId.toString(), dato.valor());
                marcarSiEsDudoso(dudosos, campoId, dato);
            }
        });
        return new FilaPropuesta(valores, dudosos);
    }

    private void marcarSiEsDudoso(Set<String> dudosos, CampoId campoId, DatoLeido dato) {
        if (dato.esDudoso(umbralConfianza)) {
            dudosos.add(campoId.toString());
        }
    }

    private static boolean sePuedeCargar(CampoId campoId, String valor, Set<CampoId> cargables) {
        return cargables.contains(campoId) && valor != null && !valor.isBlank();
    }

    /** Los campos donde se puede cargar un dato: no los calculados. */
    private static Set<CampoId> camposCargables(TablaDisponible tabla) {
        return tabla.bloque().campos().stream()
                .filter(campo -> !campo.esCalculado())
                .map(CampoConocido::id)
                .collect(Collectors.toSet());
    }

    private static boolean tieneValores(FilaPropuesta fila) {
        return !fila.valores().isEmpty();
    }
}
