package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.enums.TipoRegistroCuaderno;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.BloquePropuesto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.ConsumoIa;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.infrastructure.ia.LecturaIaJson.BloqueJson;
import com.ensambla.backend.infrastructure.ia.LecturaIaJson.CampoJson;
import com.ensambla.backend.infrastructure.ia.LecturaIaJson.DatoJson;
import com.ensambla.backend.infrastructure.ia.LecturaIaJson.FilaJson;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Valida la salida del modelo y la convierte al dominio. No confía en nada de lo que devuelve la IA: tipos,
 * claves, rangos y el id de bloque sugerido se controlan acá.
 */
class LecturaIaJsonMapper {

    static final String PREFIJO_ERROR = "La lectura de la IA no es válida: ";

    LecturaCuaderno toDomain(LecturaIaJson json, List<BloqueConocido> bloquesExistentes, int paginas,
                             ConsumoIa consumo) {
        TipoRegistroCuaderno tipoRegistro = enumeracion(TipoRegistroCuaderno.class, json.tipoRegistro(), "tipoRegistro");
        BloquePropuesto bloque = bloque(json.bloque(), bloquesExistentes);
        Set<String> claves = claves(bloque.campos());
        List<FilaLeida> filas = listaOVacia(json.filas()).stream()
                .map(fila -> fila(fila, claves, paginas))
                .toList();
        return new LecturaCuaderno(tipoRegistro, bloque, filas, listaOVacia(json.advertencias()), consumo);
    }

    private BloquePropuesto bloque(BloqueJson json, List<BloqueConocido> bloquesExistentes) {
        if (json == null) {
            throw invalida("falta el bloque propuesto");
        }
        List<CampoPropuesto> campos = listaOVacia(json.campos()).stream().map(this::campo).toList();
        if (campos.isEmpty()) {
            throw invalida("el bloque no tiene campos");
        }
        return new BloquePropuesto(textoObligatorio(json.nombre(), "el nombre del bloque"),
                bloqueExistente(json.bloqueExistenteId(), bloquesExistentes), campos);
    }

    /** Sólo se acepta un id que esté entre los bloques del negocio; cualquier otro se descarta (propone uno nuevo). */
    private UUID bloqueExistente(String id, List<BloqueConocido> bloquesExistentes) {
        return bloquesExistentes.stream()
                .map(BloqueConocido::id)
                .filter(conocido -> conocido.toString().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    private CampoPropuesto campo(CampoJson json) {
        String clave = textoObligatorio(json.clave(), "la clave de un campo");
        TipoCampo tipo = enumeracion(TipoCampo.class, json.tipo(), "el tipo del campo " + clave);
        List<String> opciones = listaOVacia(json.opciones());
        if (tipo == TipoCampo.OPCION && opciones.isEmpty()) {
            throw invalida("el campo " + clave + " es OPCION y no tiene opciones");
        }
        return new CampoPropuesto(clave, textoObligatorio(json.nombre(), "el nombre del campo " + clave), tipo, opciones);
    }

    private Set<String> claves(List<CampoPropuesto> campos) {
        Set<String> claves = new HashSet<>();
        for (CampoPropuesto campo : campos) {
            if (!claves.add(campo.clave())) {
                throw invalida("la clave " + campo.clave() + " está repetida");
            }
        }
        return claves;
    }

    private FilaLeida fila(FilaJson json, Set<String> claves, int paginas) {
        int pagina = positivo(json.pagina(), "la página");
        int renglon = positivo(json.renglon(), "el renglón");
        if (pagina > paginas) {
            throw invalida("una fila menciona la página " + pagina + " y sólo se enviaron " + paginas);
        }
        Map<String, DatoLeido> datos = new LinkedHashMap<>();
        Map<String, DatoJson> datosJson = json.datos() == null ? Map.of() : json.datos();
        datosJson.forEach((clave, dato) -> datos.put(claveConocida(clave, claves), dato(dato, clave)));
        return new FilaLeida(pagina, renglon, datos);
    }

    private String claveConocida(String clave, Set<String> claves) {
        if (!claves.contains(clave)) {
            throw invalida("una fila usa la clave " + clave + " que no está entre los campos");
        }
        return clave;
    }

    private DatoLeido dato(DatoJson json, String clave) {
        Double confianza = json == null ? null : json.confianza();
        if (confianza == null || confianza < 0 || confianza > 1) {
            throw invalida("la confianza de " + clave + " debe estar entre 0 y 1");
        }
        return new DatoLeido(json.valor(), confianza);
    }

    private int positivo(Integer valor, String nombre) {
        if (valor == null || valor < 1) {
            throw invalida(nombre + " de una fila debe ser 1 o mayor");
        }
        return valor;
    }

    private <E extends Enum<E>> E enumeracion(Class<E> tipo, String valor, String nombre) {
        String texto = textoObligatorio(valor, nombre);
        try {
            return Enum.valueOf(tipo, texto.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw invalida(nombre + " es desconocido: " + texto);
        }
    }

    private String textoObligatorio(String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            throw invalida("falta " + nombre);
        }
        return valor.strip();
    }

    private static <T> List<T> listaOVacia(List<T> lista) {
        return lista == null ? List.of() : lista;
    }

    private static LecturaCuadernoException invalida(String detalle) {
        return new LecturaCuadernoException(PREFIJO_ERROR + detalle);
    }
}
