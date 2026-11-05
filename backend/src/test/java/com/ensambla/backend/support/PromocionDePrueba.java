package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.MetricaProducto;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.promocion.ContextoPromocion;
import com.ensambla.backend.domain.model.promocion.ProductoDeInventario;
import com.ensambla.backend.domain.model.promocion.ProductoPromocionado;
import com.ensambla.backend.domain.model.promocion.Promocion;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Productos, ventas y promociones de ejemplo para las pruebas de promociones. */
public final class PromocionDePrueba {

    public static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    public static final ProyectoId PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000001");
    public static final BloqueId INVENTARIO_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    public static final RegistroId YERBA_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    public static final RegistroId FIDEOS_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");
    public static final RegistroId ACEITE_ID = RegistroId.de("d0000000-0000-4000-8000-000000000003");
    public static final RegistroId SAL_ID = RegistroId.de("d0000000-0000-4000-8000-000000000004");
    public static final RegistroId ARROZ_ID = RegistroId.de("d0000000-0000-4000-8000-000000000005");

    private static final CampoId CAMPO_NOMBRE_ID = CampoId.de("f0000000-0000-4000-8000-000000000001");
    private static final CampoId CAMPO_PRECIO_ID = CampoId.de("f0000000-0000-4000-8000-000000000002");
    private static final CampoId CAMPO_CANTIDAD_ID = CampoId.de("f0000000-0000-4000-8000-000000000003");
    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);

    private PromocionDePrueba() {
    }

    /** Inventario con las columnas nombre, precio y cantidad y las filas dadas. */
    public static BloqueInventario inventarioCon(Registro... registros) {
        Bloque bloque = Bloque.builder()
                .id(INVENTARIO_ID).negocioId(NEGOCIO_ID).proyectoId(PROYECTO_ID).tipo(TipoBloque.INVENTARIO)
                .campos(List.of(campo(CAMPO_NOMBRE_ID, "name"), campo(CAMPO_PRECIO_ID, "price"),
                        campo(CAMPO_CANTIDAD_ID, "quantity")))
                .build();
        Map<String, Campo> porClave = new LinkedHashMap<>();
        bloque.getCampos().forEach(campo -> porClave.put(campo.getClave(), campo));
        return new BloqueInventario(bloque, null, porClave, List.of(registros));
    }

    /** Fila del inventario; los valores en {@code null} quedan sin cargar. */
    public static Registro fila(RegistroId id, Object nombre, Object precio, Object cantidad) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put(CAMPO_NOMBRE_ID.toString(), nombre);
        valores.put(CAMPO_PRECIO_ID.toString(), precio);
        valores.put(CAMPO_CANTIDAD_ID.toString(), cantidad);
        return Registro.builder().id(id).negocioId(NEGOCIO_ID).bloqueId(INVENTARIO_ID).valores(valores).build();
    }

    public static ProductoDeInventario producto(RegistroId id, String nombre, String precio, String cantidad) {
        return new ProductoDeInventario(id, nombre, new BigDecimal(precio), new BigDecimal(cantidad));
    }

    /** Cuántas unidades vendió un producto en el último período. */
    public static MetricaProducto ventas(RegistroId id, String unidades) {
        return new MetricaProducto(id, new BigDecimal(unidades), new BigDecimal(unidades), null, HOY);
    }

    public static ContextoPromocion contexto(List<ProductoDeInventario> productos, MetricaProducto... metricas) {
        Map<RegistroId, MetricaProducto> porProducto = new LinkedHashMap<>();
        for (MetricaProducto metrica : metricas) {
            porProducto.put(metrica.productoId(), metrica);
        }
        return new ContextoPromocion(productos, porProducto);
    }

    public static Promocion promocionDeYerba() {
        return new Promocion("estrella", "Tu producto estrella", "Lo que más se vendió este mes",
                List.of(new ProductoPromocionado("Yerba Playadito 1 kg", new BigDecimal("3600"))));
    }

    private static Campo campo(CampoId id, String clave) {
        return Campo.builder().id(id).negocioId(NEGOCIO_ID).bloqueId(INVENTARIO_ID).clave(clave).build();
    }
}
