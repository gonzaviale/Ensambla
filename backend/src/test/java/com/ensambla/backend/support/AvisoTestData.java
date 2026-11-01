package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.BloqueVentas;
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

/** Inventario y ventas de ejemplo para las pruebas de las reglas de aviso. */
public final class AvisoTestData {

    public static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    public static final ProyectoId PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000001");
    public static final BloqueId INVENTARIO_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    public static final BloqueId ENTRADAS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    public static final CampoId CAMPO_NOMBRE_ID = CampoId.de("f0000000-0000-4000-8000-000000000001");
    public static final CampoId CAMPO_CANTIDAD_ID = CampoId.de("f0000000-0000-4000-8000-000000000002");
    public static final CampoId CAMPO_MINIMO_ID = CampoId.de("f0000000-0000-4000-8000-000000000003");

    public static final BloqueId VENTAS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000003");
    public static final CampoId CAMPO_PRODUCTO_ID = CampoId.de("f0000000-0000-4000-8000-000000000011");
    public static final CampoId CAMPO_VENTA_CANTIDAD_ID = CampoId.de("f0000000-0000-4000-8000-000000000012");
    public static final CampoId CAMPO_VENTA_PRECIO_ID = CampoId.de("f0000000-0000-4000-8000-000000000013");
    public static final CampoId CAMPO_FECHA_ID = CampoId.de("f0000000-0000-4000-8000-000000000014");
    public static final CampoId CAMPO_CLIENTE_ID = CampoId.de("f0000000-0000-4000-8000-000000000015");
    public static final CampoId CAMPO_PRECIO_ID = CampoId.de("f0000000-0000-4000-8000-000000000004");
    public static final CampoId CAMPO_COSTO_ID = CampoId.de("f0000000-0000-4000-8000-000000000005");

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

    public static Bloque bloqueVentas() {
        return Bloque.builder()
                .id(VENTAS_ID).negocioId(NEGOCIO_ID).proyectoId(PROYECTO_ID).tipo(TipoBloque.VENTAS)
                .campos(List.of(campoDe(CAMPO_PRODUCTO_ID, "product"), campoDe(CAMPO_VENTA_CANTIDAD_ID, "quantity"),
                        campoDe(CAMPO_VENTA_PRECIO_ID, "price"), campoDe(CAMPO_FECHA_ID, "date"),
                        campoDe(CAMPO_CLIENTE_ID, "customer")))
                .build();
    }

    private static Campo campoDe(CampoId id, String clave) {
        return Campo.builder().id(id).negocioId(NEGOCIO_ID).bloqueId(VENTAS_ID).clave(clave).build();
    }

    /** Una fila de ventas; los valores nulos no se cargan, como cuando la persona deja el casillero vacío. */
    public static Registro venta(Object producto, Object cantidad, Object precio, Object fecha, Object cliente) {
        Map<String, Object> valores = new LinkedHashMap<>();
        ponerSiHay(valores, CAMPO_PRODUCTO_ID, producto);
        ponerSiHay(valores, CAMPO_VENTA_CANTIDAD_ID, cantidad);
        ponerSiHay(valores, CAMPO_VENTA_PRECIO_ID, precio);
        ponerSiHay(valores, CAMPO_FECHA_ID, fecha);
        ponerSiHay(valores, CAMPO_CLIENTE_ID, cliente);
        return Registro.builder().id(RegistroId.nuevo()).negocioId(NEGOCIO_ID).bloqueId(VENTAS_ID).valores(valores)
                .build();
    }

    public static BloqueVentas ventas(Registro... registros) {
        Bloque bloque = bloqueVentas();
        return new BloqueVentas(bloque, camposPorClave(bloque), List.of(registros));
    }

    /** Inventario con nombre, cantidad, precio y costo, para las reglas de ventas. */
    public static Bloque bloqueInventarioConPrecios() {
        return Bloque.builder()
                .id(INVENTARIO_ID).negocioId(NEGOCIO_ID).proyectoId(PROYECTO_ID).tipo(TipoBloque.INVENTARIO)
                .campos(List.of(campo(CAMPO_NOMBRE_ID.valor(), "name"), campo(CAMPO_CANTIDAD_ID.valor(), "quantity"),
                        campo(CAMPO_MINIMO_ID.valor(), "minimum"), campo(CAMPO_PRECIO_ID.valor(), "price"),
                        campo(CAMPO_COSTO_ID.valor(), "cost")))
                .build();
    }

    /** Un producto del inventario con precios; precio y costo nulos no se cargan. */
    public static Registro producto(UUID id, Object nombre, Object cantidad, Object precio, Object costo) {
        Map<String, Object> valores = new LinkedHashMap<>();
        ponerSiHay(valores, CAMPO_NOMBRE_ID, nombre);
        ponerSiHay(valores, CAMPO_CANTIDAD_ID, cantidad);
        ponerSiHay(valores, CAMPO_PRECIO_ID, precio);
        ponerSiHay(valores, CAMPO_COSTO_ID, costo);
        return Registro.builder().id(RegistroId.de(id)).negocioId(NEGOCIO_ID).bloqueId(INVENTARIO_ID).valores(valores)
                .build();
    }

    public static BloqueInventario inventarioConPrecios(Registro... registros) {
        Bloque bloque = bloqueInventarioConPrecios();
        return new BloqueInventario(bloque, null, camposPorClave(bloque), List.of(registros));
    }

    private static void ponerSiHay(Map<String, Object> valores, CampoId campoId, Object valor) {
        if (valor != null) {
            valores.put(campoId.toString(), valor);
        }
    }
}
