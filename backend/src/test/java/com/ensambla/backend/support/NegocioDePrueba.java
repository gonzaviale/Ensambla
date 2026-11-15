package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.BloqueGastos;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Un cuaderno de comercio armado paso a paso para probar lo que se calcula con los datos del negocio: productos con
 * precio y costo, ventas con fecha y gastos con categoría.
 */
public final class NegocioDePrueba {

    private static final BloqueId GASTOS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000004");
    private static final CampoId GASTO_FECHA_ID = CampoId.de("f0000000-0000-4000-8000-000000000022");
    private static final CampoId GASTO_TOTAL_ID = CampoId.de("f0000000-0000-4000-8000-000000000023");
    private static final CampoId GASTO_CATEGORIA_ID = CampoId.de("f0000000-0000-4000-8000-000000000024");

    public static final UUID YERBA = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    public static final UUID FIDEOS = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    public static final UUID ACEITE = UUID.fromString("d0000000-0000-4000-8000-000000000003");

    private final String nombre;
    private final List<Registro> productos = new ArrayList<>();
    private final List<Registro> ventas = new ArrayList<>();
    private final List<Registro> gastos = new ArrayList<>();
    private boolean conTablaDeGastos = true;

    private NegocioDePrueba(String nombre) {
        this.nombre = nombre;
    }

    public static NegocioDePrueba cuaderno(String nombre) {
        return new NegocioDePrueba(nombre);
    }

    /**
     * Un almacén con ventas el 11/11/2026 como «hoy». Yerba 1 kg (precio $4.200, costo $3.300): 60 unidades en 30
     * días, 14 en los últimos 7. Fideos 500 g: parados. Aceite 900 ml (precio $2.850, costo $1.900): 20 en 30 días,
     * 5 en 7. Gastos: servicios $40.000, alquiler $60.000, mercadería $50.000 y uno de hace más de 30 días.
     * Vendió $309.000, la mercadería costó $236.000 y los gastos son $100.000: el resultado es −$27.000.
     */
    public static DatosDeProyecto almacen() {
        return cuaderno("Almacén")
                .producto(YERBA, "Yerba 1 kg", 40, 4200, 3300)
                .producto(FIDEOS, "Fideos 500 g", 40, 1100, 800)
                .producto(ACEITE, "Aceite 900 ml", 20, 2850, 1900)
                .venta(YERBA, 23, 4200, "2026-10-20")
                .venta(YERBA, 23, 4200, "2026-10-27")
                .venta(YERBA, 14, 4200, "2026-11-08")
                .venta(YERBA, 100, 4200, "2026-09-15")
                .venta(ACEITE, 15, 2850, "2026-10-20")
                .venta(ACEITE, 5, 2850, "2026-11-09")
                .gasto("2026-10-20", 40000, "Servicios")
                .gasto("2026-10-15", 60000, "Alquiler")
                .gasto("2026-10-18", 50000, "Mercadería")
                .gasto("2026-09-01", 9999, "Servicios")
                .build();
    }

    /** Un producto; precio, costo o nombre nulos no se cargan, como cuando la persona deja el casillero vacío. */
    public NegocioDePrueba producto(UUID id, String nombreProducto, Number stock, Number precio, Number costo) {
        productos.add(AvisoTestData.producto(id, nombreProducto, stock, precio, costo));
        return this;
    }

    /** Una venta; sin precio no se carga el precio. */
    public NegocioDePrueba venta(UUID producto, Number cantidad, Number precio, String fecha) {
        ventas.add(AvisoTestData.venta(producto.toString(), cantidad, precio, fecha, null));
        return this;
    }

    public NegocioDePrueba gasto(String fecha, Number total, String categoria) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put(GASTO_FECHA_ID.toString(), fecha);
        valores.put(GASTO_TOTAL_ID.toString(), total);
        valores.put(GASTO_CATEGORIA_ID.toString(), categoria);
        gastos.add(Registro.builder().id(RegistroId.nuevo()).negocioId(AvisoTestData.NEGOCIO_ID).bloqueId(GASTOS_ID)
                .valores(valores).build());
        return this;
    }

    /** Un cuaderno que no tiene tabla de gastos. */
    public NegocioDePrueba sinTablaDeGastos() {
        conTablaDeGastos = false;
        return this;
    }

    public DatosDeProyecto build() {
        BloqueInventario inventario = AvisoTestData.inventarioConPrecios(productos.toArray(Registro[]::new));
        return new DatosDeProyecto(
                nombre,
                List.of(inventario),
                List.of(AvisoTestData.ventas(ventas.toArray(Registro[]::new))),
                conTablaDeGastos ? List.of(bloqueDeGastos()) : List.of());
    }

    private BloqueGastos bloqueDeGastos() {
        Campo fecha = campoDeGasto(GASTO_FECHA_ID, "date");
        Campo total = campoDeGasto(GASTO_TOTAL_ID, "total");
        Campo categoria = campoDeGasto(GASTO_CATEGORIA_ID, "category");
        Bloque bloque = Bloque.builder().id(GASTOS_ID).negocioId(AvisoTestData.NEGOCIO_ID)
                .proyectoId(AvisoTestData.PROYECTO_ID).tipo(TipoBloque.GASTOS)
                .campos(List.of(fecha, total, categoria)).build();
        return new BloqueGastos(bloque, Map.of("date", fecha, "total", total, "category", categoria), gastos);
    }

    private static Campo campoDeGasto(CampoId id, String clave) {
        return Campo.builder().id(id).negocioId(AvisoTestData.NEGOCIO_ID).bloqueId(GASTOS_ID).clave(clave).build();
    }
}
