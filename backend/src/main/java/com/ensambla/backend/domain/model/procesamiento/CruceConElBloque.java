package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.lectura.BloquePropuesto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.SinonimosCampo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Las columnas de una foto cruzadas con los campos del bloque que eligió la persona. Las que coinciden (mismo nombre o
 * un sinónimo) reutilizan el campo; las vacías no se proponen y las demás quedan como campos nuevos.
 */
final class CruceConElBloque {

    private final LecturaCuaderno lectura;
    private final BloqueLectura bloque;
    private final Map<String, CampoConocido> asignados = new LinkedHashMap<>();
    private final Set<CampoId> usados = new HashSet<>();
    private final Set<String> ambiguas = new LinkedHashSet<>();
    private final List<CampoPropuesto> campos = new ArrayList<>();
    private final List<CampoPropuesto> nuevos = new ArrayList<>();
    private final Map<String, CampoId> mapeo = new LinkedHashMap<>();
    private final Set<String> sinDatos = new HashSet<>();
    private final List<String> advertencias;

    private CruceConElBloque(LecturaCuaderno lectura, BloqueLectura bloque) {
        this.lectura = lectura;
        this.bloque = bloque;
        this.advertencias = new ArrayList<>(lectura.advertencias());
    }

    /** Hace falta que al menos una columna coincida: si ninguna lo hace, la foto no es de este bloque. */
    static CruceConElBloque cruzar(LecturaCuaderno lectura, BloqueLectura bloque) {
        CruceConElBloque cruce = new CruceConElBloque(lectura, bloque);
        cruce.asignarPor(SinonimosCampo::mismoNombre);
        cruce.asignarPor(SinonimosCampo::equivalentes);
        cruce.exigirAlgunaCoincidencia();
        return cruce;
    }

    PropuestaLectura propuesta() {
        leidos().forEach(this::ubicar);
        List<FilaLeida> filas = lectura.filas().stream().map(this::sinColumnasVacias).toList();
        BloquePropuesto propuesta = new BloquePropuesto(bloque.nombre(), bloque.id(), campos);
        LecturaCuaderno resultado = new LecturaCuaderno(
                lectura.tipoRegistro(), propuesta, filas, advertencias, lectura.consumo());
        return new PropuestaLectura(resultado, mapeo, nuevos);
    }

    private List<CampoPropuesto> leidos() {
        return lectura.bloque().campos();
    }

    /** Asigna a cada columna sin asignar el único campo libre del bloque que cumpla el criterio. */
    private void asignarPor(BiPredicate<String, String> criterio) {
        leidos().stream()
                .filter(leido -> !asignados.containsKey(leido.clave()))
                .forEach(leido -> asignarSiHayUnoSolo(leido, candidatos(leido, criterio)));
    }

    private List<CampoConocido> candidatos(CampoPropuesto leido, BiPredicate<String, String> criterio) {
        return bloque.campos().stream()
                .filter(campo -> !usados.contains(campo.id()))
                .filter(campo -> criterio.test(campo.nombre(), leido.nombre()))
                .toList();
    }

    private void asignarSiHayUnoSolo(CampoPropuesto leido, List<CampoConocido> candidatos) {
        if (candidatos.size() > 1) {
            ambiguas.add(leido.clave());
            return;
        }
        if (candidatos.size() == 1) {
            asignados.put(leido.clave(), candidatos.getFirst());
            usados.add(candidatos.getFirst().id());
            ambiguas.remove(leido.clave());
        }
    }

    private void exigirAlgunaCoincidencia() {
        if (asignados.isEmpty()) {
            throw new BusinessRuleException(PropuestaLectura.INCOMPATIBLE);
        }
    }

    private void ubicar(CampoPropuesto leido) {
        CampoConocido existente = asignados.get(leido.clave());
        if (existente != null) {
            reutilizar(leido, existente);
            return;
        }
        if (columnaVacia(leido.clave())) {
            descartarPorVacia(leido);
            return;
        }
        proponerComoNueva(leido);
    }

    private void reutilizar(CampoPropuesto leido, CampoConocido existente) {
        campos.add(new CampoPropuesto(leido.clave(), existente.nombre(), existente.tipo(), existente.opciones()));
        mapeo.put(leido.clave(), existente.id());
    }

    private void descartarPorVacia(CampoPropuesto leido) {
        sinDatos.add(leido.clave());
        advertencias.add("La columna «%s» no tiene datos en la foto, así que no se propone.".formatted(leido.nombre()));
    }

    private void proponerComoNueva(CampoPropuesto leido) {
        campos.add(leido);
        nuevos.add(leido);
        if (ambiguas.contains(leido.clave())) {
            advertencias.add(("«%s» podría ser más de una columna de «%s»: la dejamos como columna nueva "
                    + "para que la revises.").formatted(leido.nombre(), bloque.nombre()));
        }
    }

    private boolean columnaVacia(String clave) {
        return lectura.filas().stream().noneMatch(fila -> tieneDato(fila, clave));
    }

    private static boolean tieneDato(FilaLeida fila, String clave) {
        DatoLeido dato = fila.datos().get(clave);
        return dato != null && dato.valor() != null && !dato.valor().isBlank();
    }

    private FilaLeida sinColumnasVacias(FilaLeida fila) {
        Map<String, DatoLeido> datos = new LinkedHashMap<>(fila.datos());
        datos.keySet().removeAll(sinDatos);
        return new FilaLeida(fila.pagina(), fila.renglon(), datos);
    }
}
