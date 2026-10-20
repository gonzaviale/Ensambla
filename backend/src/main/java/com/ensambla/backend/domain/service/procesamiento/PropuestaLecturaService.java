package com.ensambla.backend.domain.service.procesamiento;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.lectura.BloquePropuesto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.SinonimosCampo;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.PropuestaLectura;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;

@Service
public class PropuestaLecturaService {

    static final String INCOMPATIBLE = "La estructura leída no es compatible con el bloque de destino";

    public PropuestaLectura proponer(LecturaCuaderno lectura, List<BloqueLectura> bloques, UUID destino) {
        if (destino != null) {
            return proponerEnDestino(lectura, bloques, destino);
        }
        List<BloqueLectura> compatibles = bloques.stream()
                .filter(bloque -> compatible(lectura.bloque(), bloque))
                .toList();
        UUID bloqueId = null;
        Map<String, UUID> mapeo = new LinkedHashMap<>();
        if (compatibles.size() == 1) {
            BloqueLectura bloque = compatibles.getFirst();
            bloqueId = bloque.id();
            for (CampoPropuesto campo : lectura.bloque().campos()) {
                mapeo.put(campo.clave(), coincidencias(campo, bloque).getFirst().id());
            }
        }
        BloquePropuesto propuesta = new BloquePropuesto(
                lectura.bloque().nombre(), bloqueId, lectura.bloque().campos());
        LecturaCuaderno resultado = new LecturaCuaderno(
                lectura.tipoRegistro(), propuesta, lectura.filas(), lectura.advertencias(), lectura.consumo());
        return new PropuestaLectura(resultado, mapeo);
    }

    /**
     * Con un bloque elegido por la persona no se adivina: cada columna leída se cruza con los campos del bloque. Las
     * que coinciden (mismo nombre o un sinónimo) se reutilizan con el nombre, tipo y opciones del campo; las que el
     * bloque no tiene se proponen como campos nuevos con el tipo que sugiere lo leído. Hace falta que al menos una
     * columna coincida: si ninguna lo hace, la foto no es de este bloque.
     */
    private PropuestaLectura proponerEnDestino(LecturaCuaderno lectura, List<BloqueLectura> bloques, UUID destino) {
        BloqueLectura bloque = bloques.stream()
                .filter(candidato -> candidato.id().equals(destino))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(INCOMPATIBLE));
        List<CampoPropuesto> leidos = lectura.bloque().campos();
        Map<String, CampoConocido> asignados = new LinkedHashMap<>();
        Set<String> ambiguas = new LinkedHashSet<>();
        Set<UUID> usados = new HashSet<>();
        cruzar(leidos, bloque, SinonimosCampo::mismoNombre, asignados, usados, ambiguas);
        cruzar(leidos, bloque, SinonimosCampo::equivalentes, asignados, usados, ambiguas);
        if (asignados.isEmpty()) {
            throw new BusinessRuleException(INCOMPATIBLE);
        }

        List<CampoPropuesto> campos = new ArrayList<>();
        List<CampoPropuesto> nuevos = new ArrayList<>();
        Map<String, UUID> mapeo = new LinkedHashMap<>();
        Set<String> sinDatos = new HashSet<>();
        List<String> advertencias = new ArrayList<>(lectura.advertencias());
        for (CampoPropuesto leido : leidos) {
            CampoConocido existente = asignados.get(leido.clave());
            if (existente != null) {
                campos.add(new CampoPropuesto(leido.clave(), existente.nombre(), existente.tipo(), existente.opciones()));
                mapeo.put(leido.clave(), existente.id());
            } else if (columnaVacia(lectura.filas(), leido.clave())) {
                sinDatos.add(leido.clave());
                advertencias.add("La columna «%s» no tiene datos en la foto, así que no se propone."
                        .formatted(leido.nombre()));
            } else {
                campos.add(leido);
                nuevos.add(leido);
                if (ambiguas.contains(leido.clave())) {
                    advertencias.add(("«%s» podría ser más de una columna de «%s»: la dejamos como columna nueva "
                            + "para que la revises.").formatted(leido.nombre(), bloque.nombre()));
                }
            }
        }
        List<FilaLeida> filas = lectura.filas().stream().map(fila -> sinColumnas(fila, sinDatos)).toList();
        BloquePropuesto propuesta = new BloquePropuesto(bloque.nombre(), bloque.id(), campos);
        LecturaCuaderno resultado = new LecturaCuaderno(
                lectura.tipoRegistro(), propuesta, filas, advertencias, lectura.consumo());
        return new PropuestaLectura(resultado, mapeo, nuevos);
    }

    /** Asigna a cada columna sin asignar el único campo libre del bloque que cumpla el criterio. */
    private void cruzar(List<CampoPropuesto> leidos, BloqueLectura bloque, BiPredicate<String, String> criterio,
                        Map<String, CampoConocido> asignados, Set<UUID> usados, Set<String> ambiguas) {
        for (CampoPropuesto leido : leidos) {
            if (asignados.containsKey(leido.clave())) {
                continue;
            }
            List<CampoConocido> candidatos = bloque.campos().stream()
                    .filter(campo -> !usados.contains(campo.id()))
                    .filter(campo -> criterio.test(campo.nombre(), leido.nombre()))
                    .toList();
            if (candidatos.size() == 1) {
                asignados.put(leido.clave(), candidatos.getFirst());
                usados.add(candidatos.getFirst().id());
                ambiguas.remove(leido.clave());
            } else if (candidatos.size() > 1) {
                ambiguas.add(leido.clave());
            }
        }
    }

    private boolean columnaVacia(List<FilaLeida> filas, String clave) {
        return filas.stream().noneMatch(fila -> {
            DatoLeido dato = fila.datos().get(clave);
            return dato != null && dato.valor() != null && !dato.valor().isBlank();
        });
    }

    private FilaLeida sinColumnas(FilaLeida fila, Set<String> claves) {
        Map<String, DatoLeido> datos = new LinkedHashMap<>(fila.datos());
        datos.keySet().removeAll(claves);
        return new FilaLeida(fila.pagina(), fila.renglon(), datos);
    }

    private boolean compatible(BloquePropuesto propuesta, BloqueLectura bloque) {
        return normalizar(propuesta.nombre()).equals(normalizar(bloque.nombre()))
                && propuesta.campos().stream().allMatch(campo -> coincidencias(campo, bloque).size() == 1);
    }

    private List<CampoConocido> coincidencias(CampoPropuesto campo, BloqueLectura bloque) {
        return bloque.campos().stream()
                .filter(conocido -> normalizar(conocido.nombre()).equals(normalizar(campo.nombre())))
                .filter(conocido -> conocido.tipo() == campo.tipo())
                .filter(conocido -> conocido.opciones().containsAll(campo.opciones()))
                .toList();
    }

    private String normalizar(String valor) {
        return TextoNormalizado.de(valor);
    }
}
