package com.ensambla.backend.domain.useCase.procesamiento.leerImagen;

import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.procesamiento.LecturaImagen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lo que se leyó de la foto.
 *
 * @param filas        una por renglón; sus datos van por la clave del campo del bloque o la de una columna nueva
 * @param camposNuevos columnas de la foto que el bloque no tiene; vacía si el bloque no existe todavía
 * @param advertencias avisos de la lectura para mostrar a la persona
 */
public record LeerImagenItem(List<Map<String, DatoItem>> filas, List<CampoItem> camposNuevos,
                             List<String> advertencias) {

    /** @param dudoso {@code true} si la IA no leyó el dato con seguridad */
    public record DatoItem(String valor, boolean dudoso) {
    }

    /** @param tipo nombre de un {@code TipoCampo} */
    public record CampoItem(String clave, String nombre, String tipo, List<String> opciones) {

        static CampoItem de(CampoPropuesto campo) {
            return new CampoItem(campo.clave(), campo.nombre(), campo.tipo().name(), campo.opciones());
        }
    }

    /** Debajo de {@code umbralConfianza} un dato se marca como dudoso. */
    public static LeerImagenItem de(LecturaImagen lectura, double umbralConfianza) {
        return new LeerImagenItem(
                lectura.filas().stream().map(datos -> fila(datos, umbralConfianza)).toList(),
                lectura.camposNuevos().stream().map(CampoItem::de).toList(),
                lectura.advertencias());
    }

    private static Map<String, DatoItem> fila(Map<String, DatoLeido> datos, double umbralConfianza) {
        Map<String, DatoItem> fila = new LinkedHashMap<>();
        datos.forEach((clave, dato) -> fila.put(clave, new DatoItem(dato.valor(), dato.esDudoso(umbralConfianza))));
        return fila;
    }
}
