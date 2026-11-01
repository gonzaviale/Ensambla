package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.CampoContexto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * El bloque contra el que se lee una foto suelta: lo describe quien llama y no existe en la base. Cada campo lleva un
 * id propio de esta lectura, para poder cruzar lo que devuelve la IA con la clave que usa quien llama.
 */
public record BloqueDeImagen(BloqueId id, String nombre, List<CampoDeImagen> campos) {

    /** @param clave identificador que usa quien llama; las filas leídas se devuelven indexadas por esta clave */
    public record CampoDeImagen(CampoId id, String clave, String nombre, TipoCampo tipo, List<String> opciones) {

        public CampoDeImagen {
            opciones = opciones == null ? List.of() : List.copyOf(opciones);
        }

        public static CampoDeImagen nuevo(String clave, String nombre, TipoCampo tipo, List<String> opciones) {
            return new CampoDeImagen(CampoId.nuevo(), clave, nombre, tipo, opciones);
        }
    }

    public BloqueDeImagen {
        campos = List.copyOf(campos);
    }

    /** @throws BusinessRuleException si el bloque no tiene ningún dato para leer */
    public static BloqueDeImagen de(String nombre, List<CampoDeImagen> campos) {
        if (campos.isEmpty()) {
            throw new BusinessRuleException("El bloque no tiene datos para leer");
        }
        return new BloqueDeImagen(BloqueId.nuevo(), nombre, campos);
    }

    /** El bloque tal como se le muestra a la IA. */
    public BloqueConocido paraElLector() {
        return new BloqueConocido(id, nombre, campos.stream()
                .map(campo -> new CampoContexto(campo.nombre(), campo.tipo(), campo.opciones()))
                .toList());
    }

    /** El bloque tal como lo cruza la propuesta con las columnas leídas. */
    public BloqueLectura paraLaPropuesta() {
        return new BloqueLectura(id, nombre, campos.stream()
                .map(campo -> new CampoConocido(campo.id(), campo.nombre(), campo.tipo(), campo.opciones()))
                .toList());
    }

    public Map<CampoId, String> clavesPorCampo() {
        Map<CampoId, String> claves = new LinkedHashMap<>();
        campos.forEach(campo -> claves.put(campo.id(), campo.clave()));
        return claves;
    }
}
