package com.ensambla.backend.domain.service.aviso.regla;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.ContextoNegocio;
import com.ensambla.backend.domain.model.aviso.MetricaProducto;
import com.ensambla.backend.domain.model.aviso.ParProductos;
import com.ensambla.backend.domain.model.aviso.ResultadoRegla;
import com.ensambla.backend.domain.model.aviso.VentaProducto;
import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.enums.TipoAviso;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.aviso.CanastasVentasService;
import com.ensambla.backend.domain.service.aviso.MetricasVentasService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.ensambla.backend.domain.service.aviso.regla.FormatoAviso.CLAVE_COSTO;
import static com.ensambla.backend.domain.service.aviso.regla.FormatoAviso.CLAVE_NOMBRE;
import static com.ensambla.backend.domain.service.aviso.regla.FormatoAviso.CLAVE_PRECIO;
import static com.ensambla.backend.domain.service.aviso.regla.FormatoAviso.aDiez;
import static com.ensambla.backend.domain.service.aviso.regla.FormatoAviso.nombreDe;
import static com.ensambla.backend.domain.service.aviso.regla.FormatoAviso.numero;
import static com.ensambla.backend.domain.service.aviso.regla.FormatoAviso.pesos;

/**
 * Dos productos de la misma tabla que el mismo cliente se lleva el mismo día una y otra vez, y de los que al menos uno
 * rota poco: sugiere armar un combo con un 10 % de descuento sobre la suma de sus precios. El combo se crea como un
 * producto nuevo del inventario. No avisa si ya hay un combo de esos dos productos.
 */
@Component
public class ReglaCombo implements ReglaAviso {

    private static final BigDecimal DESCUENTO = new BigDecimal("0.9");
    private static final short PRIORIDAD = 1;
    private static final Set<String> PALABRAS_VACIAS = Set.of("del", "los", "las", "con", "sin", "por", "para", "kg",
            "lts", "unidad", "unidades", "pack", "docena");

    private final MetricasVentasService metricasVentasService;

    private final CanastasVentasService canastasVentasService;

    private final int minimoVeces;

    public ReglaCombo(MetricasVentasService metricasVentasService, CanastasVentasService canastasVentasService,
                      @Value("${ensambla.avisos.combo-minimo-veces:3}") int minimoVeces) {
        this.metricasVentasService = metricasVentasService;
        this.canastasVentasService = canastasVentasService;
        this.minimoVeces = minimoVeces;
    }

    @Override
    public TipoAviso tipo() {
        return TipoAviso.COMBO;
    }

    @Override
    public List<ResultadoRegla> evaluar(ContextoNegocio contexto) {
        LocalDate hoy = contexto.hoy();
        if (hoy == null) {
            return List.of();
        }
        int dias = metricasVentasService.periodoDias();
        List<VentaProducto> ventas = metricasVentasService.ventasValidas(contexto.ventas(), hoy);
        List<ParProductos> pares = canastasVentasService.paresFrecuentes(ventas, hoy, dias, minimoVeces);
        if (pares.isEmpty()) {
            return List.of();
        }
        Map<UUID, MetricaProducto> metricas = metricasVentasService.calcular(contexto.ventas(), hoy, dias);
        return pares.stream()
                .map(par -> evaluarPar(contexto, par, metricas))
                .filter(Objects::nonNull)
                .toList();
    }

    private ResultadoRegla evaluarPar(ContextoNegocio contexto, ParProductos par, Map<UUID, MetricaProducto> metricas) {
        if (!rotaPoco(par.productoA(), metricas) && !rotaPoco(par.productoB(), metricas)) {
            return null;
        }
        for (BloqueInventario inventario : contexto.inventarios()) {
            // En el orden del inventario, para que el aviso se lea siempre igual.
            List<Registro> productos = inventario.registros().stream()
                    .filter(registro -> registro.getId().equals(par.productoA()) || registro.getId().equals(par.productoB()))
                    .toList();
            if (productos.size() == 2) {
                return resultadoDelCombo(inventario, par, productos.get(0), productos.get(1));
            }
        }
        return null;
    }

    private boolean rotaPoco(UUID productoId, Map<UUID, MetricaProducto> metricas) {
        MetricaProducto metrica = metricas.getOrDefault(productoId, MetricaProducto.sinVentas(productoId));
        return metricasVentasService.esBajaRotacion(metrica, metricas.values());
    }

    private ResultadoRegla resultadoDelCombo(BloqueInventario inventario, ParProductos par, Registro primero,
                                             Registro segundo) {
        String nombrePrimero = nombreDe(inventario, primero);
        String nombreSegundo = nombreDe(inventario, segundo);
        BigDecimal precioPrimero = numero(inventario.valorDe(primero, CLAVE_PRECIO));
        BigDecimal precioSegundo = numero(inventario.valorDe(segundo, CLAVE_PRECIO));
        if (!positivo(precioPrimero) || !positivo(precioSegundo) || yaExiste(inventario, nombrePrimero, nombreSegundo)) {
            return null;
        }
        BigDecimal suma = precioPrimero.add(precioSegundo);
        BigDecimal precio = aDiez(suma.multiply(DESCUENTO));
        if (precio.signum() <= 0 || precio.compareTo(suma) >= 0) {
            return null;
        }
        boolean primeroEsA = primero.getId().equals(par.productoA());
        // Sólo los precios: si cambian, un aviso descartado vuelve a aparecer. Las veces que se vendieron juntos no entran.
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("precioA", primeroEsA ? precioPrimero : precioSegundo);
        datos.put("precioB", primeroEsA ? precioSegundo : precioPrimero);
        datos.put("sugerido", precio);
        return new ResultadoRegla(
                inventario.bloque().getProyectoId(),
                "combo:" + par.productoA() + ":" + par.productoB(),
                PRIORIDAD,
                nombrePrimero + " y " + nombreSegundo + " se venden juntos",
                "Salen poco por separado, pero " + par.veces() + " veces se los llevó el mismo cliente el mismo día.",
                datos,
                List.of("Por separado suman " + pesos(suma) + "; como combo, " + pesos(precio)
                                + " (10 % menos) puede ayudar a que roten.",
                        "El combo se crea como un producto nuevo del inventario; después cargás cuántos armás."),
                List.of(accionCrear(inventario, nombrePrimero, nombreSegundo, precio,
                        costo(inventario, primero, segundo))));
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    /** La suma de los costos que el negocio cargó; {@code null} si no cargó ninguno. */
    private static BigDecimal costo(BloqueInventario inventario, Registro primero, Registro segundo) {
        BigDecimal costoPrimero = numero(inventario.valorDe(primero, CLAVE_COSTO));
        BigDecimal costoSegundo = numero(inventario.valorDe(segundo, CLAVE_COSTO));
        BigDecimal suma = (positivo(costoPrimero) ? costoPrimero : BigDecimal.ZERO)
                .add(positivo(costoSegundo) ? costoSegundo : BigDecimal.ZERO);
        return suma.signum() > 0 ? suma : null;
    }

    private static Map<String, Object> accionCrear(BloqueInventario inventario, String nombrePrimero,
                                                   String nombreSegundo, BigDecimal precio, BigDecimal costo) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put(CLAVE_NOMBRE, "Combo " + nombrePrimero + " + " + nombreSegundo);
        valores.put("quantity", 0);
        valores.put(CLAVE_PRECIO, precio);
        if (costo != null) {
            valores.put(CLAVE_COSTO, costo);
        }
        Map<String, Object> accion = new LinkedHashMap<>();
        accion.put("tipo", "CREAR_REGISTRO");
        accion.put("texto", "Crear el combo");
        accion.put("proyectoId", inventario.bloque().getProyectoId().toString());
        accion.put("bloqueId", inventario.bloque().getId().toString());
        accion.put("valores", valores);
        return accion;
    }

    /** Ya hay un producto "combo" que nombra a los dos: lo reconoce por la primera palabra significativa de cada uno. */
    private static boolean yaExiste(BloqueInventario inventario, String nombrePrimero, String nombreSegundo) {
        String primera = primeraPalabra(nombrePrimero);
        String segunda = primeraPalabra(nombreSegundo);
        if (primera.isEmpty() || segunda.isEmpty()) {
            return false;
        }
        return inventario.registros().stream()
                .map(registro -> TextoNormalizado.de(nombreDe(inventario, registro)))
                .anyMatch(nombre -> nombre.contains("combo") && nombre.contains(primera) && nombre.contains(segunda));
    }

    private static String primeraPalabra(String nombre) {
        return Arrays.stream(TextoNormalizado.de(nombre).replaceAll("[^a-z0-9 ]+", " ").split("\\s+"))
                .filter(palabra -> palabra.length() >= 3)
                .filter(palabra -> !PALABRAS_VACIAS.contains(palabra))
                .filter(palabra -> !Character.isDigit(palabra.charAt(0)))
                .findFirst()
                .orElse("");
    }
}
