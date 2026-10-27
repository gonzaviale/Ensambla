package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.common.Huella;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Lo que una regla detecta sobre una entidad del negocio. {@code datos} son los datos de origen: si cambian, el aviso
 * cambia de huella y un aviso descartado vuelve a aparecer.
 */
public record ResultadoRegla(
        ProyectoId proyectoId,
        String entidad,
        short prioridad,
        String titulo,
        String detalle,
        Map<String, Object> datos,
        List<Object> motivos,
        List<Object> acciones) {

    private static final String PREFIJO_REGISTRO = "registro:";

    public static String entidadDeRegistro(RegistroId registroId) {
        return PREFIJO_REGISTRO + registroId;
    }

    /** Identificador estable del aviso: {@code tipo:entidad:hash(datos)}. */
    public String huellaPara(TipoAviso tipo) {
        return tipo + ":" + entidad + ":" + hashDeDatos();
    }

    private String hashDeDatos() {
        String texto = new TreeMap<>(datos).entrySet().stream()
                .map(dato -> dato.getKey() + "=" + normalizar(dato.getValue()))
                .collect(Collectors.joining(";"));
        return Huella.de(texto);
    }

    private static String normalizar(Object valor) {
        return valor instanceof BigDecimal numero ? numero.stripTrailingZeros().toPlainString() : String.valueOf(valor);
    }
}
