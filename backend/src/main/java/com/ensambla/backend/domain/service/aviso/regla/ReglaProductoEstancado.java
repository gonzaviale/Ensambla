package com.ensambla.backend.domain.service.aviso.regla;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.ContextoNegocio;
import com.ensambla.backend.domain.model.aviso.MetricaProducto;
import com.ensambla.backend.domain.model.aviso.ResultadoRegla;
import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.enums.TipoAviso;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.service.aviso.MetricasVentasService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.ensambla.backend.domain.service.aviso.regla.FormatoAviso.CLAVE_CANTIDAD;
import static com.ensambla.backend.domain.service.aviso.regla.FormatoAviso.CLAVE_PRECIO;
import static com.ensambla.backend.domain.service.aviso.regla.FormatoAviso.aDiez;
import static com.ensambla.backend.domain.service.aviso.regla.FormatoAviso.fechaCorta;
import static com.ensambla.backend.domain.service.aviso.regla.FormatoAviso.nombreDe;
import static com.ensambla.backend.domain.service.aviso.regla.FormatoAviso.numero;
import static com.ensambla.backend.domain.service.aviso.regla.FormatoAviso.pesos;
import static com.ensambla.backend.domain.service.aviso.regla.FormatoAviso.texto;

/**
 * Productos caros que además se mueven poco: el precio está más de un 5 % por encima de lo que cobran los comercios de
 * la zona (o de lo que el propio negocio cobraba) y casi no se vendieron en el período. Sugiere rebajar el precio al de
 * referencia, redondeado a la decena. Sin ventas anotadas en el negocio no hay forma de saber qué se mueve, así que no
 * avisa.
 */
@Component
@RequiredArgsConstructor
public class ReglaProductoEstancado implements ReglaAviso {

    private static final BigDecimal MARGEN = new BigDecimal("1.05");
    private static final short PRIORIDAD = 2;

    private final MetricasVentasService metricasVentasService;

    /** Precio al que conviene acercarse y la razón, ya escrita para el motivo del aviso. */
    private record Referencia(BigDecimal precio, String motivo, boolean esDeLaZona) {
    }

    @Override
    public TipoAviso tipo() {
        return TipoAviso.ESTANCADO;
    }

    @Override
    public List<ResultadoRegla> evaluar(ContextoNegocio contexto) {
        if (contexto.hoy() == null) {
            return List.of();
        }
        Map<UUID, MetricaProducto> metricas = metricasVentasService.calcular(
                contexto.ventas(), contexto.hoy(), metricasVentasService.periodoDias());
        if (!metricasVentasService.hayVentas(metricas)) {
            return List.of();
        }
        return contexto.inventarios().stream()
                .flatMap(inventario -> inventario.registros().stream()
                        .map(registro -> evaluarRegistro(contexto, inventario, registro, metricas))
                        .filter(Objects::nonNull))
                .toList();
    }

    private ResultadoRegla evaluarRegistro(ContextoNegocio contexto, BloqueInventario inventario, Registro registro,
                                           Map<UUID, MetricaProducto> metricas) {
        BigDecimal cantidad = numero(inventario.valorDe(registro, CLAVE_CANTIDAD));
        BigDecimal precio = numero(inventario.valorDe(registro, CLAVE_PRECIO));
        if (cantidad == null || cantidad.signum() <= 0 || precio == null || precio.signum() <= 0) {
            return null;
        }
        MetricaProducto metrica = metricas.getOrDefault(registro.getId(), MetricaProducto.sinVentas(registro.getId()));
        if (!metricasVentasService.esBajaRotacion(metrica, metricas.values())) {
            return null;
        }
        String nombre = nombreDe(inventario, registro);
        Optional<Referencia> referencia = referenciaDe(contexto, nombre, precio, metrica);
        if (referencia.isEmpty()) {
            return null;
        }
        BigDecimal sugerido = aDiez(referencia.get().precio());
        if (sugerido.signum() <= 0 || sugerido.compareTo(precio) >= 0) {
            return null;
        }
        return resultado(inventario, registro, nombre, cantidad, precio, sugerido, metrica, referencia.get());
    }

    /** La zona manda sobre el historial propio: es lo que el cliente ve en los comercios de al lado. */
    private Optional<Referencia> referenciaDe(ContextoNegocio contexto, String nombre, BigDecimal precio,
                                              MetricaProducto metrica) {
        Optional<Referencia> deLaZona = contexto.zona().productos().stream()
                .filter(producto -> TextoNormalizado.de(producto.nombre()).equals(TextoNormalizado.de(nombre)))
                .filter(producto -> estaPorEncima(precio, producto.precioPromedio()))
                .findFirst()
                .map(ReglaProductoEstancado::referenciaDeZona);
        if (deLaZona.isPresent()) {
            return deLaZona;
        }
        BigDecimal historico = metrica.precioPromedioHistorico();
        return historico != null && estaPorEncima(precio, historico)
                ? Optional.of(new Referencia(historico,
                        "Está por encima de tu precio promedio histórico de " + pesos(historico) + ".", false))
                : Optional.empty();
    }

    private static Referencia referenciaDeZona(AgregadoZonaProducto producto) {
        return new Referencia(producto.precioPromedio(),
                "En tu zona, " + producto.comercios() + " comercios lo venden entre " + pesos(producto.precioMinimo())
                        + " y " + pesos(producto.precioMaximo()) + ".", true);
    }

    private static boolean estaPorEncima(BigDecimal precio, BigDecimal referencia) {
        return precio.compareTo(referencia.multiply(MARGEN)) > 0;
    }

    private ResultadoRegla resultado(BloqueInventario inventario, Registro registro, String nombre,
                                     BigDecimal cantidad, BigDecimal precio, BigDecimal sugerido,
                                     MetricaProducto metrica, Referencia referencia) {
        // Sólo los precios: si cambian, un aviso descartado vuelve a aparecer. Las ventas del período no entran.
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("precio", precio);
        datos.put("sugerido", sugerido);
        return new ResultadoRegla(
                inventario.bloque().getProyectoId(),
                ResultadoRegla.entidadDeRegistro(registro.getId()),
                PRIORIDAD,
                nombre + " está caro y no se mueve",
                detalle(metrica, precio, sugerido, referencia),
                datos,
                List.of(referencia.motivo(), ultimaVenta(metrica), "Tenés " + texto(cantidad) + " en stock."),
                List.of(accionRebajar(inventario, registro.getId(), sugerido), accionAbrir(inventario, registro.getId())));
    }

    private String detalle(MetricaProducto metrica, BigDecimal precio, BigDecimal sugerido, Referencia referencia) {
        int dias = metricasVentasService.periodoDias();
        String ventas = metrica.unidadesPeriodo().signum() == 0
                ? "No vendiste ninguno en los últimos " + dias + " días"
                : "Vendiste " + texto(metrica.unidadesPeriodo()) + " en los últimos " + dias + " días";
        return ventas + " y lo tenés a " + pesos(precio) + ". Si lo bajás a " + pesos(sugerido)
                + ", queda cerca de " + (referencia.esDeLaZona() ? "lo que cobran en tu zona." : "lo que solías cobrar.");
    }

    private static String ultimaVenta(MetricaProducto metrica) {
        return metrica.ultimaVenta() == null
                ? "Todavía no anotaste ventas de este producto."
                : "La última venta fue el " + fechaCorta(metrica.ultimaVenta()) + ".";
    }

    private static Map<String, Object> accionRebajar(BloqueInventario inventario, UUID registroId, BigDecimal sugerido) {
        Map<String, Object> accion = accionAbrir(inventario, registroId);
        accion.put("tipo", "EDITAR_REGISTRO");
        accion.put("texto", "Rebajar a " + pesos(sugerido));
        accion.put("valores", Map.of(CLAVE_PRECIO, sugerido));
        return accion;
    }

    private static Map<String, Object> accionAbrir(BloqueInventario inventario, UUID registroId) {
        Map<String, Object> accion = new LinkedHashMap<>();
        accion.put("tipo", "ABRIR");
        accion.put("texto", "Ver producto");
        accion.put("proyectoId", inventario.bloque().getProyectoId().toString());
        accion.put("bloqueId", inventario.bloque().getId().toString());
        accion.put("registroId", registroId.toString());
        return accion;
    }
}
