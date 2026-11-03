package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.precio.CambioPrecio;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.ensambla.backend.domain.model.aviso.FormatoAviso.fechaCorta;
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.nombreDe;
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.pesos;
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.porcentaje;
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.texto;

/**
 * Productos que dejaron de venderse como antes después de un aumento de precio: toma el último cambio de precio de cada
 * producto y, si fue un aumento de los últimos días, compara las unidades por día de la ventana anterior con las de la
 * ventana posterior (el mismo cálculo con el que se mide el impacto de un aviso aplicado). Si cayeron al menos
 * {@code caidaMinima}, sugiere revisar el precio y ofrece volver al anterior. Mientras la ventana posterior no termine
 * no se puede comparar, así que no avisa. Si después del aumento el precio bajó, el último cambio ya no es un aumento y
 * el aviso vence.
 *
 * @param ventanaDias días antes y después del aumento que se comparan
 * @param caidaMinima fracción mínima que tienen que caer las unidades por día (0,2 es una caída del 20 %)
 */
@RequiredArgsConstructor
public class ReglaRevisarPrecioTrasAumento implements ReglaAviso {

    private static final short PRIORIDAD = 2;

    private final MetricasVentas metricasVentas;

    private final int ventanaDias;

    private final BigDecimal caidaMinima;

    @Override
    public TipoAviso tipo() {
        return TipoAviso.REVISAR_PRECIO;
    }

    @Override
    public List<ResultadoRegla> evaluar(ContextoNegocio contexto) {
        if (contexto.hoy() == null || contexto.cambiosDePrecio().isEmpty()) {
            return List.of();
        }
        Map<RegistroId, CambioPrecio> ultimos = ultimoCambioDeCada(contexto.cambiosDePrecio());
        List<VentaProducto> ventas = metricasVentas.ventasValidas(contexto.ventas(), contexto.hoy());
        return contexto.inventarios().stream()
                .flatMap(inventario -> inventario.registros().stream()
                        .map(registro -> evaluarRegistro(contexto.hoy(), inventario, registro, ultimos, ventas))
                        .filter(Objects::nonNull))
                .toList();
    }

    /** Los cambios llegan del más viejo al más nuevo: el último de cada producto es el que queda. */
    private static Map<RegistroId, CambioPrecio> ultimoCambioDeCada(List<CambioPrecio> cambios) {
        Map<RegistroId, CambioPrecio> ultimos = new LinkedHashMap<>();
        cambios.stream()
                .sorted((a, b) -> a.cambiadoEn().compareTo(b.cambiadoEn()))
                .forEach(cambio -> ultimos.put(cambio.registroId(), cambio));
        return ultimos;
    }

    private ResultadoRegla evaluarRegistro(LocalDate hoy, BloqueInventario inventario, Registro registro,
                                           Map<RegistroId, CambioPrecio> ultimos, List<VentaProducto> ventas) {
        CambioPrecio cambio = ultimos.get(registro.getId());
        if (cambio == null || !cambio.esAumento() || cambio.fecha().isBefore(hoy.minusDays(metricasVentas.periodoDias()))) {
            return null;
        }
        List<VentaProducto> delProducto = ventas.stream()
                .filter(venta -> venta.productoId().equals(registro.getId()))
                .toList();
        Optional<ImpactoAviso> impacto = ImpactoAviso.medir(delProducto, cambio.fecha(), hoy, ventanaDias);
        if (impacto.isEmpty() || !cayeron(impacto.get())) {
            return null;
        }
        return resultado(inventario, registro, cambio, impacto.get());
    }

    private boolean cayeron(ImpactoAviso impacto) {
        BigDecimal antes = impacto.unidadesPorDiaAntes();
        return antes.signum() > 0 && caida(impacto).compareTo(caidaMinima) >= 0;
    }

    private static BigDecimal caida(ImpactoAviso impacto) {
        return impacto.unidadesPorDiaAntes().subtract(impacto.unidadesPorDiaDespues())
                .divide(impacto.unidadesPorDiaAntes(), 4, RoundingMode.HALF_UP);
    }

    private ResultadoRegla resultado(BloqueInventario inventario, Registro registro, CambioPrecio cambio,
                                     ImpactoAviso impacto) {
        String nombre = nombreDe(inventario, registro);
        String aumento = pesos(cambio.anterior()) + " a " + pesos(cambio.nuevo());
        String antes = texto(impacto.unidadesPorDiaAntes());
        String despues = texto(impacto.unidadesPorDiaDespues());
        // El precio y la fecha del aumento: un aviso descartado sólo vuelve si hay un aumento nuevo.
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("precioAnterior", cambio.anterior());
        datos.put("precioNuevo", cambio.nuevo());
        datos.put("fecha", cambio.fecha().toString());
        return new ResultadoRegla(
                inventario.bloque().getProyectoId(),
                ResultadoRegla.entidadDeRegistro(registro.getId()),
                PRIORIDAD,
                nombre + " vende menos desde que subiste el precio",
                "Lo subiste de " + aumento + " el " + fechaCorta(cambio.fecha()) + " y desde entonces vendés " + despues
                        + " por día, contra " + antes + " antes. Revisá si el nuevo precio te conviene.",
                datos,
                List.of("Vendías " + antes + " unidades por día y ahora " + despues + " ("
                                + porcentaje(impacto.unidadesPorDiaAntes().subtract(impacto.unidadesPorDiaDespues()),
                                impacto.unidadesPorDiaAntes()) + " % menos).",
                        "El precio pasó de " + aumento + " (" + porcentaje(cambio.nuevo().subtract(cambio.anterior()),
                                cambio.anterior()) + " % más) el " + fechaCorta(cambio.fecha()) + "."),
                List.of(AccionesAviso.cambiarPrecio(inventario, registro.getId(), "Volver a " + pesos(cambio.anterior()),
                                cambio.anterior()),
                        AccionesAviso.abrir(inventario, registro.getId())));
    }
}
