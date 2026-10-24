package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.Registro;

import com.ensambla.backend.domain.model.registro.RegistroId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Inventario de ejemplo para las pruebas de las reglas de aviso. */
public final class AvisoTestData {

    public static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    public static final ProyectoId PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000001");
    public static final BloqueId INVENTARIO_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    public static final BloqueId ENTRADAS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    public static final CampoId CAMPO_NOMBRE_ID = CampoId.de("f0000000-0000-4000-8000-000000000001");
    public static final CampoId CAMPO_CANTIDAD_ID = CampoId.de("f0000000-0000-4000-8000-000000000002");
    public static final CampoId CAMPO_MINIMO_ID = CampoId.de("f0000000-0000-4000-8000-000000000003");

    private AvisoTestData() {
    }

    public static Bloque bloqueInventario() {
        return Bloque.builder()
                .id(INVENTARIO_ID).negocioId(NEGOCIO_ID).proyectoId(PROYECTO_ID).tipo(TipoBloque.INVENTARIO)
                .campos(List.of(campo(CAMPO_NOMBRE_ID.valor(), "name"), campo(CAMPO_CANTIDAD_ID.valor(), "quantity"),
                        campo(CAMPO_MINIMO_ID.valor(), "minimum")))
                .build();
    }

    public static Campo campo(UUID id, String clave) {
        return Campo.builder().id(CampoId.de(id)).negocioId(NEGOCIO_ID).bloqueId(INVENTARIO_ID).clave(clave).build();
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
        return Registro.builder().id(RegistroId.de(id)).negocioId(NEGOCIO_ID).bloqueId(INVENTARIO_ID).valores(valores)
                .build();
    }

    public static BloqueInventario inventario(BloqueId entradasId, Registro... registros) {
        Bloque bloque = bloqueInventario();
        return new BloqueInventario(bloque, entradasId, camposPorClave(bloque), List.of(registros));
    }
}
