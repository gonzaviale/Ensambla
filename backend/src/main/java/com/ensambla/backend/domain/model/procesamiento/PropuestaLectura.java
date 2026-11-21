package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.lectura.BloquePropuesto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lo que se le muestra a la persona para revisar.
 *
 * @param mapeoCampos  clave de cada columna leída → id del campo del bloque existente que le corresponde
 * @param camposNuevos columnas de la foto que el bloque destino todavía no tiene, con el tipo sugerido; también están
 *                     entre los campos de la lectura, pero sin entrada en {@code mapeoCampos}. Las lecturas guardadas
 *                     antes de que existiera esta lista vuelven con {@code null}: se leen como vacía
 */
public record PropuestaLectura(LecturaCuaderno lectura, Map<String, CampoId> mapeoCampos,
                               List<CampoPropuesto> camposNuevos) {

    static final String INCOMPATIBLE = "La estructura leída no es compatible con el bloque de destino";

    public PropuestaLectura {
        mapeoCampos = Map.copyOf(mapeoCampos);
        camposNuevos = camposNuevos == null ? List.of() : List.copyOf(camposNuevos);
    }

    public PropuestaLectura(LecturaCuaderno lectura, Map<String, CampoId> mapeoCampos) {
        this(lectura, mapeoCampos, List.of());
    }

    public static PropuestaLectura proponer(LecturaCuaderno lectura, List<BloqueLectura> bloques, BloqueId destino) {
        if (destino != null) {
            return proponerEnDestino(lectura, bloques, destino);
        }
        List<BloqueLectura> compatibles = bloques.stream()
                .filter(bloque -> compatible(lectura.bloque(), bloque))
                .toList();
        if (compatibles.size() != 1) {
            return conBloque(lectura, null, Map.of());
        }
        return enElBloqueCompatible(lectura, compatibles.getFirst());
    }

    private static PropuestaLectura enElBloqueCompatible(LecturaCuaderno lectura, BloqueLectura bloque) {
        Map<String, CampoId> mapeo = new LinkedHashMap<>();
        lectura.bloque().campos()
                .forEach(campo -> mapeo.put(campo.clave(), coincidencias(campo, bloque).getFirst().id()));
        return conBloque(lectura, bloque.id(), mapeo);
    }

    private static PropuestaLectura conBloque(LecturaCuaderno lectura, BloqueId bloqueId, Map<String, CampoId> mapeo) {
        BloquePropuesto propuesta = new BloquePropuesto(lectura.bloque().nombre(), bloqueId, lectura.bloque().campos());
        LecturaCuaderno resultado = new LecturaCuaderno(
                lectura.tipoRegistro(), propuesta, lectura.filas(), lectura.advertencias(), lectura.consumo());
        return new PropuestaLectura(resultado, mapeo);
    }

    /**
     * Con un bloque elegido por la persona no se adivina: cada columna leída se cruza con los campos del bloque. Las
     * que coinciden (mismo nombre o un sinónimo) se reutilizan con el nombre, tipo y opciones del campo; las que el
     * bloque no tiene se proponen como campos nuevos con el tipo que sugiere lo leído.
     */
    private static PropuestaLectura proponerEnDestino(LecturaCuaderno lectura, List<BloqueLectura> bloques,
                                                      BloqueId destino) {
        return CruceConElBloque.cruzar(lectura, bloqueElegido(bloques, destino)).propuesta();
    }

    private static BloqueLectura bloqueElegido(List<BloqueLectura> bloques, BloqueId destino) {
        return bloques.stream()
                .filter(candidato -> candidato.id().equals(destino))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(INCOMPATIBLE));
    }
    private static boolean compatible(BloquePropuesto propuesta, BloqueLectura bloque) {
        return normalizar(propuesta.nombre()).equals(normalizar(bloque.nombre()))
                && propuesta.campos().stream().allMatch(campo -> coincidencias(campo, bloque).size() == 1);
    }

    private static List<CampoConocido> coincidencias(CampoPropuesto campo, BloqueLectura bloque) {
        return bloque.campos().stream()
                .filter(conocido -> normalizar(conocido.nombre()).equals(normalizar(campo.nombre())))
                .filter(conocido -> conocido.tipo() == campo.tipo())
                .filter(conocido -> conocido.opciones().containsAll(campo.opciones()))
                .toList();
    }

    private static String normalizar(String valor) {
        return TextoNormalizado.de(valor);
    }
}
