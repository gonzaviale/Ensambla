package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.reporte.PeriodoResumen;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.AvisosResumen;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.ClienteTop;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.Clientes;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.Faltante;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.GastoPorCategoria;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.Gastos;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.ProductoDesalineado;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.ProductoParado;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.ProductoVendido;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.Stock;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.Ventas;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.ZonaResumen;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Resúmenes de negocio ya armados para probar lo que se hace con ellos: el PDF, la exportación. */
public final class ResumenesDePrueba {

    public static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final PeriodoResumen PERIODO = new PeriodoResumen(LocalDate.of(2026, 9, 20), HOY);

    private ResumenesDePrueba() {
    }

    /** Un negocio con datos en todas las secciones. */
    public static ResumenNegocio completo() {
        return new ResumenNegocio("Almacén de Ana", "Almacén de prueba", PERIODO, HOY, ventas(),
                masVendidos(), sinMovimiento(), stockConFaltantes(), clientes(), gastos(), zona(), avisos());
    }

    /** Un negocio sin movimiento: no tiene productos, clientes, gastos ni zona. */
    public static ResumenNegocio vacio() {
        return new ResumenNegocio("Almacén de Ana", "Almacén vacío", PERIODO, HOY,
                new Ventas(BigDecimal.ZERO, BigDecimal.ZERO, 0, BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                List.of(), List.of(), new Stock(0, 0, List.of(), BigDecimal.ZERO, BigDecimal.ZERO),
                new Clientes(List.of(), BigDecimal.ZERO, 0), null, null, sinAvisos());
    }

    /** Diez productos vendidos, {@code parados} parados y 12 faltantes: el stock cae justo en el corte de página. */
    public static ResumenNegocio largo(int parados) {
        return new ResumenNegocio("Almacén de Ana", "Almacén de prueba", PERIODO, HOY, ventas(),
                vendidos(10), parados(parados), new Stock(15, 12, faltantes(12), new BigDecimal("480000"),
                new BigDecimal("690000")), new Clientes(List.of(), BigDecimal.ZERO, 0), null, null, sinAvisos());
    }

    public static ResumenNegocio conAjustes(Stock stock, Gastos gastos, ZonaResumen zona) {
        ResumenNegocio base = completo();
        return new ResumenNegocio(base.negocio(), base.sistema(), base.periodo(), base.emitidoEl(), base.ventas(),
                base.masVendidos(), base.sinMovimiento(), stock, base.clientes(), gastos, zona, base.avisos());
    }

    public static Faltante faltante(String nombre, String cantidad, String minimo, String pedir) {
        return new Faltante(nombre, new BigDecimal(cantidad), minimo == null ? null : new BigDecimal(minimo),
                pedir == null ? null : new BigDecimal(pedir));
    }

    private static Ventas ventas() {
        return new Ventas(new BigDecimal("1250000"), new BigDecimal("640"), 310, new BigDecimal("41666.67"),
                new BigDecimal("1116000"), new BigDecimal("12"), new BigDecimal("310000"));
    }

    private static List<ProductoVendido> masVendidos() {
        return List.of(new ProductoVendido("Yerba Nobleza Gaucha 1 kg", new BigDecimal("60"), new BigDecimal("204000"),
                        new BigDecimal("42000")),
                new ProductoVendido("Producto sin costo", new BigDecimal("5"), new BigDecimal("5000"), null));
    }

    private static List<ProductoParado> sinMovimiento() {
        return List.of(new ProductoParado("Mermelada BC La Campagnola 454 g", new BigDecimal("20"),
                        new BigDecimal("36000")),
                new ProductoParado("Sin costo", new BigDecimal("3"), null));
    }

    private static Stock stockConFaltantes() {
        List<Faltante> faltantes = List.of(faltante("Aceite Cocinero 900 ml", "0", "6", "12"),
                faltante("Fideos Luchetti 500 g", "8", null, null));
        return new Stock(15, 2, faltantes, new BigDecimal("480000"), new BigDecimal("690000"));
    }

    private static Clientes clientes() {
        return new Clientes(List.of(new ClienteTop("Doña Rosa", 4, new BigDecimal("8000"))), new BigDecimal("12000"), 3);
    }

    private static Gastos gastos() {
        return new Gastos(new BigDecimal("255000"), 6,
                List.of(new GastoPorCategoria("Mercadería", new BigDecimal("200000"))), new BigDecimal("-5000"));
    }

    private static ZonaResumen zona() {
        return new ZonaResumen("San Justo", 8, 1, 3, 1, List.of(
                new ProductoDesalineado("Fideos Luchetti 500 g", new BigDecimal("1700"), new BigDecimal("1100"),
                        new BigDecimal("1500"), new BigDecimal("13")),
                new ProductoDesalineado("Yerba Nobleza Gaucha 1 kg", new BigDecimal("3400"), new BigDecimal("3500"),
                        new BigDecimal("4800"), new BigDecimal("-3"))));
    }

    private static AvisosResumen avisos() {
        Map<TipoAviso, Long> pendientes = new EnumMap<>(TipoAviso.class);
        pendientes.put(TipoAviso.STOCK, 3L);
        pendientes.put(TipoAviso.PRECIO_ZONA, 2L);
        return new AvisosResumen(pendientes, 4, 1, 2, new BigDecimal("15000"));
    }

    private static AvisosResumen sinAvisos() {
        return new AvisosResumen(new EnumMap<>(TipoAviso.class), 0, 0, 0, BigDecimal.ZERO);
    }

    private static List<ProductoVendido> vendidos(int cantidad) {
        List<ProductoVendido> vendidos = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            vendidos.add(new ProductoVendido("Producto vendido " + i, new BigDecimal(50 - i),
                    new BigDecimal(100000 - i * 1000), new BigDecimal(20000)));
        }
        return vendidos;
    }

    private static List<ProductoParado> parados(int cantidad) {
        List<ProductoParado> parados = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            parados.add(new ProductoParado("Producto parado " + i, new BigDecimal(10 + i),
                    new BigDecimal(5000 * (i + 1))));
        }
        return parados;
    }

    private static List<Faltante> faltantes(int cantidad) {
        List<Faltante> faltantes = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            faltantes.add(faltante("Faltante " + i, "0", "6", "12"));
        }
        return faltantes;
    }
}
