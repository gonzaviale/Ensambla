package com.ensambla.backend.domain.service.procesamiento;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.lectura.BloquePropuesto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.PropuestaLectura;
import com.ensambla.backend.domain.model.registro.Referencia;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Convierte el texto que la IA leyó en una columna de conexión (RELACION) en la fila a la que apunta: busca en la
 * tabla conectada una fila cuya etiqueta sea igual (sin importar tildes ni mayúsculas). Si no hay ninguna, o hay más
 * de una, deja la celda vacía con confianza 0 y avisa, para que la persona la elija en la revisión.
 */
@RequiredArgsConstructor
public class ConexionesLecturaService {

    static final double SIN_CONFIANZA = 0.0;

    private final CampoService campoService;

    private final RegistroDatosService registroDatosService;

    public PropuestaLectura resolver(UUID negocioId, PropuestaLectura propuesta, List<BloqueLectura> bloques) {
        Map<String, BloqueLectura> conectadas = tablasConectadas(propuesta, bloques);
        if (conectadas.isEmpty()) {
            return propuesta;
        }
        Resolucion resolucion = new Resolucion(negocioId);
        List<FilaLeida> filas = propuesta.lectura().filas().stream()
                .map(fila -> resolverFila(fila, conectadas, resolucion))
                .toList();
        List<String> advertencias = new ArrayList<>(propuesta.lectura().advertencias());
        advertencias.addAll(resolucion.advertencias);
        LecturaCuaderno lectura = propuesta.lectura();
        return new PropuestaLectura(new LecturaCuaderno(lectura.tipoRegistro(), lectura.bloque(), filas, advertencias,
                lectura.consumo()), propuesta.mapeoCampos(), propuesta.camposNuevos());
    }

    /** Clave de cada columna de conexión del bloque destino → tabla con la que se conecta. */
    private Map<String, BloqueLectura> tablasConectadas(PropuestaLectura propuesta, List<BloqueLectura> bloques) {
        BloquePropuesto propuesto = propuesta.lectura().bloque();
        Optional<BloqueLectura> destino = propuesto.bloqueExistente()
                .flatMap(id -> bloques.stream().filter(bloque -> bloque.id().equals(id)).findFirst());
        Map<String, BloqueLectura> conectadas = new LinkedHashMap<>();
        for (CampoPropuesto campo : propuesto.campos()) {
            if (campo.tipo() != TipoCampo.RELACION) {
                continue;
            }
            destino.flatMap(bloque -> conocido(bloque, propuesta.mapeoCampos().get(campo.clave())))
                    .map(CampoConocido::bloqueRelacionadoId)
                    .flatMap(id -> bloques.stream().filter(bloque -> bloque.id().equals(id)).findFirst())
                    .ifPresent(tabla -> conectadas.put(campo.clave(), tabla));
        }
        return conectadas;
    }

    private Optional<CampoConocido> conocido(BloqueLectura bloque, UUID campoId) {
        return bloque.campos().stream().filter(campo -> campo.id().equals(campoId)).findFirst();
    }

    private FilaLeida resolverFila(FilaLeida fila, Map<String, BloqueLectura> conectadas, Resolucion resolucion) {
        Map<String, DatoLeido> datos = new LinkedHashMap<>(fila.datos());
        conectadas.forEach((clave, tabla) -> {
            DatoLeido dato = datos.get(clave);
            if (dato != null && dato.valor() != null && !dato.valor().isBlank()) {
                datos.put(clave, resolucion.resolver(tabla, dato));
            }
        });
        return new FilaLeida(fila.pagina(), fila.renglon(), datos);
    }

    /** Una pasada por lectura: cada tabla conectada se consulta una vez por texto distinto. */
    private final class Resolucion {

        private final UUID negocioId;
        private final Map<UUID, List<Campo>> camposPorTabla = new HashMap<>();
        private final Map<String, Optional<UUID>> encontradas = new HashMap<>();
        private final Set<String> avisadas = new HashSet<>();
        private final Set<String> advertencias = new LinkedHashSet<>();

        private Resolucion(UUID negocioId) {
            this.negocioId = negocioId;
        }

        private DatoLeido resolver(BloqueLectura tabla, DatoLeido dato) {
            String clave = tabla.id() + ":" + TextoNormalizado.de(dato.valor());
            Optional<UUID> fila = encontradas.computeIfAbsent(clave, ignorada -> buscar(tabla.id(), dato.valor()));
            if (fila.isPresent()) {
                return new DatoLeido(fila.get().toString(), dato.confianza());
            }
            if (avisadas.add(clave)) {
                advertencias.add("No encontramos «%s» en «%s», o hay más de una igual: elegila vos."
                        .formatted(dato.valor().strip(), tabla.nombre()));
            }
            return new DatoLeido(null, SIN_CONFIANZA);
        }

        private Optional<UUID> buscar(UUID tablaId, String texto) {
            List<Campo> campos = camposPorTabla.computeIfAbsent(tablaId,
                    id -> campoService.listarDeBloque(id, negocioId));
            var etiquetar = Referencia.etiquetador(campos);
            List<Referencia> iguales = registroDatosService
                    .buscarReferenciables(negocioId, tablaId, campos, texto, List.of()).stream()
                    .map(etiquetar)
                    .filter(referencia ->
                            TextoNormalizado.de(referencia.etiqueta()).equals(TextoNormalizado.de(texto)))
                    .toList();
            return iguales.size() == 1 ? Optional.of(iguales.getFirst().id()) : Optional.empty();
        }
    }
}
