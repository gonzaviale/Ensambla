package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.aviso.AccionAbrirRegistro;
import com.ensambla.backend.domain.model.aviso.AccionCrearRegistro;
import com.ensambla.backend.domain.model.aviso.AccionDeAviso;
import com.ensambla.backend.domain.model.aviso.AccionEditarRegistro;
import com.ensambla.backend.domain.model.aviso.AccionRegistrar;
import com.ensambla.backend.domain.model.aviso.AccionVerZona;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static com.ensambla.backend.support.AvisoTestData.ENTRADAS_ID;
import static com.ensambla.backend.support.AvisoTestData.INVENTARIO_ID;
import static com.ensambla.backend.support.AvisoTestData.PROYECTO_ID;

/** Las acciones de aviso que las reglas ofrecen sobre el inventario de prueba, para compararlas por su significado. */
public final class AccionesDePrueba {

    private AccionesDePrueba() {
    }

    public static AccionDeAviso abrirProducto(UUID producto) {
        return new AccionAbrirRegistro("Ver producto", PROYECTO_ID, INVENTARIO_ID, RegistroId.de(producto));
    }

    public static AccionDeAviso abrirProducto(RegistroId producto) {
        return new AccionAbrirRegistro("Ver producto", PROYECTO_ID, INVENTARIO_ID, producto);
    }

    public static AccionDeAviso registrarEntrada() {
        return new AccionRegistrar("Registrar entrada", PROYECTO_ID, ENTRADAS_ID);
    }

    public static AccionDeAviso cambiarPrecio(String texto, UUID producto, String precio) {
        return new AccionEditarRegistro(texto, PROYECTO_ID, INVENTARIO_ID, RegistroId.de(producto),
                Map.of("price", new BigDecimal(precio)));
    }

    public static AccionDeAviso crearEnElInventario(String texto, Map<String, Object> valores) {
        return new AccionCrearRegistro(texto, PROYECTO_ID, INVENTARIO_ID, valores);
    }

    public static AccionDeAviso verEnLaZona(String producto) {
        return new AccionVerZona("Ver en Mi zona", producto);
    }
}
