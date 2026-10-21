package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.enums.TipoBloque;
import com.ensambla.backend.domain.model.registro.Registro;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Inventario de ejemplo para las pruebas de las reglas de aviso. */
public final class AvisoTestData {

    public static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    public static final UUID PROYECTO_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    public static final UUID INVENTARIO_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    public static final UUID ENTRADAS_ID = UUID.fromString("b0000000-0000-4000-8000-000000000002");
    public static final UUID CAMPO_NOMBRE_ID = UUID.fromString("f0000000-0000-4000-8000-000000000001");
    public static final UUID CAMPO_CANTIDAD_ID = UUID.fromString("f0000000-0000-4000-8000-000000000002");
    public static final UUID CAMPO_MINIMO_ID = UUID.fromString("f0000000-0000-4000-8000-000000000003");

    private AvisoTestData() {
    }

    public static Bloque bloqueInventario() {
        return Bloque.builder()
                .id(INVENTARIO_ID).negocioId(NEGOCIO_ID).proyectoId(PROYECTO_ID).tipo(TipoBloque.INVENTARIO)
                .campos(List.of(campo(CAMPO_NOMBRE_ID, "name"), campo(CAMPO_CANTIDAD_ID, "quantity"),
                        campo(CAMPO_MINIMO_ID, "minimum")))
                .build();
    }

    public static Campo campo(UUID id, String clave) {
        return Campo.builder().id(id).negocioId(NEGOCIO_ID).bloqueId(INVENTARIO_ID).clave(clave).build();
    }

    public static Map<String, Campo> camposPorClave(Bloque bloque) {
        Map<String, Campo> porClave = new LinkedHashMap<>();
        bloque.getCampos().forEach(campo -> porClave.put(campo.getClave(), campo));
        return porClave;
    }

    public static Registro registro(UUID id, Object nombre, Object cantidad, Object minimo) {
        Map<String, Object> valores = new LinkedHashMap<>();
        if (nombre != null) {
            valores.put(CAMPO_NOMBRE_ID.toString(), nombre);
        }
        if (cantidad != null) {
            valores.put(CAMPO_CANTIDAD_ID.toString(), cantidad);
        }
        if (minimo != null) {
            valores.put(CAMPO_MINIMO_ID.toString(), minimo);
        }
        return Registro.builder().id(id).negocioId(NEGOCIO_ID).bloqueId(INVENTARIO_ID).valores(valores).build();
    }

    public static BloqueInventario inventario(UUID entradasId, Registro... registros) {
        Bloque bloque = bloqueInventario();
        return new BloqueInventario(bloque, entradasId, camposPorClave(bloque), List.of(registros));
    }
}
