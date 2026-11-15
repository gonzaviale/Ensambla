package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.objetivo.MedidorMiniObjetivo.Gasto;
import com.ensambla.backend.domain.model.objetivo.MedidorMiniObjetivo.Producto;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.zona.AnalisisZona;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Arma los mini-objetivos posibles de un negocio con sus datos reales y elige los mejores. Cada tipo tiene su cuenta
 * (ver {@code docs/objetivos-autogestionados.md}); un candidato sin datos suficientes o que no suma plata no se arma:
 * no se inventa nada. Los criterios de producto (margen bajo, parado, lo que más deja) son los de
 * {@link PasosObjetivo}. Es lógica pura.
 */
public final class CandidatosMiniObjetivo {

    /** Cuántas alternativas se le ofrecen a la persona para cambiar un mini-objetivo. */
    public static final int ALTERNATIVAS = 3;

    /** Sin tope: todos los candidatos que no chocan. */
    public static final int TODOS = Integer.MAX_VALUE;

    private static final BigDecimal MARGEN_BAJO = new BigDecimal("0.30");
    private static final BigDecimal PRECIO_PASO_GRANDE = new BigDecimal("1500");
    private static final BigDecimal PASO_GRANDE = new BigDecimal("50");
    private static final BigDecimal PASO_CHICO = new BigDecimal("10");
    private static final BigDecimal AUMENTO_DE_PRECIO = new BigDecimal("0.05");
    /** Cuánto cae la venta por cada punto que sube el precio (sensibilidad «algunos compran menos»). */
    private static final BigDecimal SENSIBILIDAD = BigDecimal.valueOf(2);
    private static final BigDecimal PARTE_DEL_STOCK_A_MOVER = new BigDecimal("0.25");
    private static final BigDecimal SEMANAS_PARA_MOVER = BigDecimal.valueOf(4);
    private static final BigDecimal SUBA_A_LA_VISTA = new BigDecimal("1.2");
    private static final BigDecimal BAJA_DEL_GASTO = new BigDecimal("0.9");
    private static final BigDecimal PUNTOS_DE_MARGEN = BigDecimal.valueOf(2);
    private static final BigDecimal TOPE_DE_MARGEN = BigDecimal.valueOf(60);
    private static final BigDecimal SUBA_DE_VENTAS = new BigDecimal("0.10");
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal DIAS_DEL_MES = BigDecimal.valueOf(MedidorMiniObjetivo.DIAS_MES);
    private static final BigDecimal DIAS_DE_LA_SEMANA = BigDecimal.valueOf(MedidorMiniObjetivo.DIAS_SEMANA);

    /** Mayor aporte primero; a igualdad, por tipo y por referencia, para que el orden no cambie entre corridas. */
    private static final Comparator<CandidatoMiniObjetivo> POR_APORTE = Comparator
            .comparing(CandidatoMiniObjetivo::aporteEstimado, Comparator.reverseOrder())
            .thenComparing(CandidatoMiniObjetivo::tipo)
            .thenComparing(candidato -> Objects.toString(candidato.referencia(), ""));

    private CandidatosMiniObjetivo() {
    }

    private record Lugar(TipoMiniObjetivo tipo, RegistroId registroId) {

        boolean choca(TipoMiniObjetivo otroTipo, RegistroId otroRegistroId) {
            return tipo == otroTipo || (otroRegistroId != null && otroRegistroId.equals(registroId));
        }
    }

    /** Todos los candidatos con datos, del que más suma al que menos; vacío si no hubo ventas en 30 días. */
    public static List<CandidatoMiniObjetivo> armar(MedidorMiniObjetivo medidor, AnalisisZona zona) {
        if (!medidor.hayVentas()) {
            return List.of();
        }
        List<CandidatoMiniObjetivo> candidatos = new ArrayList<>();
        for (Producto producto : medidor.productos()) {
            subirPrecio(medidor, zona, producto).ifPresent(candidatos::add);
            moverParado(medidor, producto).ifPresent(candidatos::add);
            ponerALaVista(medidor, producto).ifPresent(candidatos::add);
        }
        medidor.gastos().forEach(gasto -> candidatos.add(recortarGasto(gasto)));
        mejorarMargen(medidor).ifPresent(candidatos::add);
        ventasSemanales(medidor).ifPresent(candidatos::add);
        candidatos.sort(POR_APORTE);
        return List.copyOf(candidatos);
    }

    /**
     * Hasta {@code cuantos} candidatos, los de mayor aporte primero, sin repetir tipo ni producto entre sí ni con los
     * mini-objetivos que ya ocupan un lugar.
     */
    public static List<CandidatoMiniObjetivo> elegir(
            List<CandidatoMiniObjetivo> candidatos, Collection<MiniObjetivo> ocupados, int cuantos) {
        List<Lugar> lugares = new ArrayList<>(ocupados.stream()
                .map(mini -> new Lugar(mini.getTipo(), mini.getRegistroId()))
                .toList());
        List<CandidatoMiniObjetivo> elegidos = new ArrayList<>();
        for (CandidatoMiniObjetivo candidato : candidatos) {
            if (elegidos.size() == cuantos) {
                break;
            }
            if (lugares.stream().noneMatch(lugar -> lugar.choca(candidato.tipo(), candidato.registroId()))) {
                elegidos.add(candidato);
                lugares.add(new Lugar(candidato.tipo(), candidato.registroId()));
            }
        }
        return List.copyOf(elegidos);
    }

    /**
     * Hasta {@code cuantos} candidatos para cambiar {@code actual}: no es el mismo, ni repite el tipo o el producto de
     * los otros mini-objetivos en curso. Puede ser de su mismo tipo con otro producto u otra categoría.
     */
    public static List<CandidatoMiniObjetivo> alternativas(
            List<CandidatoMiniObjetivo> candidatos, MiniObjetivo actual, Collection<MiniObjetivo> enCurso,
            int cuantos) {
        List<MiniObjetivo> ocupados = enCurso.stream()
                .filter(otro -> !otro.getId().equals(actual.getId()))
                .toList();
        List<CandidatoMiniObjetivo> posibles = candidatos.stream()
                .filter(candidato -> !candidato.esElMismo(
                        actual.getTipo(), actual.getRegistroId(), actual.getReferencia()))
                .toList();
        return elegir(posibles, ocupados, cuantos);
    }

    // --- tipos de producto ---

    /** Un producto con margen bajo que se vende: subirle el precio, sin pasar del máximo de la zona. */
    private static Optional<CandidatoMiniObjetivo> subirPrecio(
            MedidorMiniObjetivo medidor, AnalisisZona zona, Producto producto) {
        BigDecimal unidadesMes = medidor.unidades(producto.id(), MedidorMiniObjetivo.DIAS_MES);
        BigDecimal ganancia = producto.gananciaPorUnidad();
        if (unidadesMes.signum() == 0 || ganancia.compareTo(producto.precio().multiply(MARGEN_BAJO)) >= 0) {
            return Optional.empty();
        }
        BigDecimal aumento = aumentoDe(producto, zona);
        if (aumento.signum() <= 0) {
            return Optional.empty();
        }
        BigDecimal caida = aumento.multiply(SENSIBILIDAD).divide(producto.precio(), 4, RoundingMode.HALF_UP);
        BigDecimal unidadesNuevas = unidadesMes.multiply(BigDecimal.ONE.subtract(caida).max(BigDecimal.ZERO));
        BigDecimal aporte = ganancia.add(aumento).multiply(unidadesNuevas)
                .subtract(ganancia.multiply(unidadesMes));
        if (aporte.signum() <= 0) {
            return Optional.empty();
        }
        BigDecimal valorInicial = medidor.gananciaSemanal(producto);
        BigDecimal meta = valorInicial.add(porSemana(aporte));
        TextoMiniObjetivo texto = TextoMiniObjetivo.subirPrecio(
                producto.nombre(), aumento, ganancia, ganancia.multiply(CIEN).divide(producto.precio(), 1,
                        RoundingMode.HALF_UP),
                porSemana(unidadesMes), aporte);
        return Optional.of(candidato(TipoMiniObjetivo.SUBIR_PRECIO, producto, valorInicial, meta, aporte, texto));
    }

    /** El 5 % del precio, hacia arriba al paso de un cartel; con rango de zona, sin pasar de su máximo. */
    private static BigDecimal aumentoDe(Producto producto, AnalisisZona zona) {
        BigDecimal paso = pasoDePrecio(producto);
        BigDecimal aumento = producto.precio().multiply(AUMENTO_DE_PRECIO)
                .divide(paso, 0, RoundingMode.CEILING)
                .multiply(paso);
        return zona.productoConRango(producto.nombre())
                .map(rango -> aumento.min(rango.precioMaximo().subtract(producto.precio())))
                .orElse(aumento);
    }

    /** Un producto con stock que no se vendió en 30 días y que admite una rebaja: venderlo a un ritmo semanal. */
    private static Optional<CandidatoMiniObjetivo> moverParado(MedidorMiniObjetivo medidor, Producto producto) {
        BigDecimal paso = pasoDePrecio(producto);
        BigDecimal ganancia = producto.gananciaPorUnidad();
        boolean parado = medidor.unidades(producto.id(), MedidorMiniObjetivo.DIAS_MES).signum() == 0
                && producto.stock().signum() > 0;
        if (!parado || ganancia.compareTo(paso.multiply(BigDecimal.valueOf(2))) < 0) {
            return Optional.empty();
        }
        BigDecimal meta = producto.stock().multiply(PARTE_DEL_STOCK_A_MOVER)
                .divide(SEMANAS_PARA_MOVER, 0, RoundingMode.CEILING)
                .max(BigDecimal.ONE);
        BigDecimal aporte = porMes(ganancia.subtract(paso).multiply(meta));
        TextoMiniObjetivo texto = TextoMiniObjetivo.moverParado(
                producto.nombre(), producto.stock(), producto.costo().multiply(producto.stock()), paso, meta);
        return Optional.of(candidato(
                TipoMiniObjetivo.MOVER_PARADO, producto, BigDecimal.ZERO, meta, aporte, texto));
    }

    /** Un producto que se vende y deja plata por unidad: venderle una unidad más por semana, o un 20 % más. */
    private static Optional<CandidatoMiniObjetivo> ponerALaVista(MedidorMiniObjetivo medidor, Producto producto) {
        BigDecimal ganancia = producto.gananciaPorUnidad();
        if (medidor.unidades(producto.id(), MedidorMiniObjetivo.DIAS_MES).signum() == 0 || ganancia.signum() <= 0) {
            return Optional.empty();
        }
        BigDecimal valorInicial = medidor.unidades(producto.id(), MedidorMiniObjetivo.DIAS_SEMANA);
        BigDecimal meta = valorInicial.add(BigDecimal.ONE)
                .max(valorInicial.multiply(SUBA_A_LA_VISTA).setScale(0, RoundingMode.CEILING));
        BigDecimal aporte = porMes(ganancia.multiply(meta.subtract(valorInicial)));
        TextoMiniObjetivo texto = TextoMiniObjetivo.ponerALaVista(
                producto.nombre(), ganancia, valorInicial, meta, aporte);
        return Optional.of(candidato(
                TipoMiniObjetivo.PONER_A_LA_VISTA, producto, valorInicial, meta, aporte, texto));
    }

    // --- tipos del negocio ---

    /** Un gasto que recortar: un 10 % menos en la categoría. */
    private static CandidatoMiniObjetivo recortarGasto(Gasto gasto) {
        BigDecimal meta = gasto.total().multiply(BAJA_DEL_GASTO);
        BigDecimal aporte = gasto.total().subtract(meta);
        TextoMiniObjetivo texto = TextoMiniObjetivo.recortarGasto(
                gasto.categoria(), gasto.total(), meta, aporte);
        return new CandidatoMiniObjetivo(TipoMiniObjetivo.RECORTAR_GASTO, null, gasto.categoria(),
                dos(gasto.total()), dos(meta), dos(aporte), texto.titulo(), texto.justificacion(), false);
    }

    /** Dos puntos más de margen sobre lo vendido, hasta un tope razonable. */
    private static Optional<CandidatoMiniObjetivo> mejorarMargen(MedidorMiniObjetivo medidor) {
        return medidor.margen()
                .filter(margen -> margen.compareTo(TOPE_DE_MARGEN) < 0)
                .map(margen -> {
                    BigDecimal meta = margen.add(PUNTOS_DE_MARGEN).min(TOPE_DE_MARGEN);
                    BigDecimal aporte = BigDecimal.valueOf(medidor.vendiste())
                            .multiply(meta.subtract(margen)).divide(CIEN, 2, RoundingMode.HALF_UP);
                    TextoMiniObjetivo texto = TextoMiniObjetivo.mejorarMargen(margen, meta, aporte);
                    return new CandidatoMiniObjetivo(TipoMiniObjetivo.MEJORAR_MARGEN, null, null,
                            dos(margen), dos(meta), dos(aporte), texto.titulo(), texto.justificacion(), false);
                });
    }

    /** Un 10 % más de ventas por semana, lo que deja el margen que tiene el negocio. */
    private static Optional<CandidatoMiniObjetivo> ventasSemanales(MedidorMiniObjetivo medidor) {
        BigDecimal ventas = medidor.ventasSemana();
        return medidor.margen()
                .filter(margen -> margen.signum() > 0 && ventas.signum() > 0)
                .map(margen -> {
                    BigDecimal meta = ventas.multiply(BigDecimal.ONE.add(SUBA_DE_VENTAS));
                    BigDecimal aporte = porMes(meta.subtract(ventas)).multiply(margen).divide(CIEN, 2,
                            RoundingMode.HALF_UP);
                    TextoMiniObjetivo texto = TextoMiniObjetivo.ventasSemanales(ventas, meta, aporte);
                    return new CandidatoMiniObjetivo(TipoMiniObjetivo.VENTAS_SEMANALES, null, null,
                            dos(ventas), dos(meta), dos(aporte), texto.titulo(), texto.justificacion(), false);
                });
    }

    // --- cuentas ---

    private static CandidatoMiniObjetivo candidato(
            TipoMiniObjetivo tipo, Producto producto, BigDecimal valorInicial, BigDecimal meta, BigDecimal aporte,
            TextoMiniObjetivo texto) {
        return new CandidatoMiniObjetivo(tipo, producto.id(), producto.nombre(), dos(valorInicial), dos(meta),
                dos(aporte), texto.titulo(), texto.justificacion(), false);
    }

    /** De $5 a $10 el paso es de a $10; desde $1.500 los carteles van de a $50. */
    private static BigDecimal pasoDePrecio(Producto producto) {
        return producto.precio().compareTo(PRECIO_PASO_GRANDE) >= 0 ? PASO_GRANDE : PASO_CHICO;
    }

    private static BigDecimal porMes(BigDecimal porSemana) {
        return porSemana.multiply(DIAS_DEL_MES).divide(DIAS_DE_LA_SEMANA, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal porSemana(BigDecimal porMes) {
        return porMes.multiply(DIAS_DE_LA_SEMANA).divide(DIAS_DEL_MES, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal dos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
