package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Un cambio, una métrica o una meta semanal que Sami propone para llegar a un objetivo. Mide un valor
 * ({@code valorActual}) que tiene que llegar a la {@code meta} en el {@link TipoMiniObjetivo#sentido() sentido} de su
 * tipo. Un objetivo tiene como máximo {@link #MAXIMO_EN_CURSO} en curso, uno por posición.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiniObjetivo {

    public static final int MAXIMO_EN_CURSO = 3;

    /** Desde cuántos días sin mejorar un mini-objetivo en curso cuenta como sin avance. */
    public static final int DIAS_SIN_AVANCE = 14;

    /** El margen bruto más alto que se le pide a un mini-objetivo de margen, en puntos. */
    public static final BigDecimal TOPE_DE_MARGEN = BigDecimal.valueOf(60);

    private static final BigDecimal PASO_SI_NO_HAY_RECORRIDO = new BigDecimal("0.10");

    private MiniObjetivoId id;

    private ObjetivoId objetivoId;

    private NegocioId negocioId;

    /** Lugar en la pantalla, de 1 a {@link #MAXIMO_EN_CURSO}. */
    private int posicion;

    private TipoMiniObjetivo tipo;

    /** El producto, si el tipo es de producto. */
    private RegistroId registroId;

    /** El nombre del producto o de la categoría de gasto. */
    private String referencia;

    private BigDecimal meta;

    private BigDecimal valorInicial;

    private BigDecimal valorActual;

    /** Pesos por mes que suma al resultado del negocio si se cumple. */
    private BigDecimal aporteEstimado;

    private EstadoMiniObjetivo estado;

    /** El lunes de la semana en que se propuso. */
    private LocalDate semanaInicio;

    private String titulo;

    private String justificacion;

    private boolean textoConIa;

    /** Desde qué día el valor no mejora; vacío si viene mejorando o ya no está en curso. */
    private LocalDate sinAvanceDesde;

    /** El que lo reemplazó, si fue reemplazado. */
    private MiniObjetivoId reemplazadoPor;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;

    /** Un mini-objetivo en curso que arranca con el valor inicial del candidato; el id se asigna acá. */
    public static MiniObjetivo nuevo(
            Objetivo objetivo, int posicion, CandidatoMiniObjetivo candidato, LocalDate semanaInicio,
            OffsetDateTime ahora) {
        return MiniObjetivo.builder()
                .id(MiniObjetivoId.nuevo())
                .objetivoId(objetivo.getId())
                .negocioId(objetivo.getNegocioId())
                .posicion(posicion)
                .tipo(candidato.tipo())
                .registroId(candidato.registroId())
                .referencia(candidato.referencia())
                .meta(candidato.meta())
                .valorInicial(candidato.valorInicial())
                .valorActual(candidato.valorInicial())
                .aporteEstimado(candidato.aporteEstimado())
                .estado(EstadoMiniObjetivo.EN_CURSO)
                .semanaInicio(semanaInicio)
                .titulo(candidato.titulo())
                .justificacion(candidato.justificacion())
                .textoConIa(candidato.textoConIa())
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build();
    }

    /** Un mini-objetivo en curso por candidato, en las posiciones 1, 2, 3… en el orden recibido. */
    public static List<MiniObjetivo> propuestos(
            Objetivo objetivo, List<CandidatoMiniObjetivo> candidatos, LocalDate semanaInicio, OffsetDateTime ahora) {
        return IntStream.range(0, candidatos.size())
                .mapToObj(indice -> nuevo(objetivo, indice + 1, candidatos.get(indice), semanaInicio, ahora))
                .toList();
    }

    public boolean estaEnCurso() {
        return estado == EstadoMiniObjetivo.EN_CURSO;
    }

    /** Si el valor actual ya llegó a la meta, según el sentido del tipo. */
    public boolean cumplido() {
        return cumpleCon(valorActual);
    }

    /** Si ese valor llega a la meta, según el sentido del tipo. */
    public boolean cumpleCon(BigDecimal valor) {
        return tipo.sentido() == SentidoMiniObjetivo.SUBE
                ? valor.compareTo(meta) >= 0
                : valor.compareTo(meta) <= 0;
    }

    /** Si quedó logrado o quitado y todavía nada ocupó su lugar: el cierre semanal tiene que seguirlo. */
    public boolean esperaReajuste() {
        return (estado == EstadoMiniObjetivo.LOGRADO || estado == EstadoMiniObjetivo.ABANDONADO)
                && reemplazadoPor == null;
    }

    /**
     * La meta que sigue a la lograda: desde el valor al cierre (o la meta, si quedó más cerca) se suma el mismo
     * recorrido que se le pidió al principio, en el sentido del tipo. El margen no pasa de {@link #TOPE_DE_MARGEN} y un
     * gasto no baja de cero; vacío si con eso no se puede pedir más que la meta de ahora.
     */
    public Optional<BigDecimal> metaSiguiente(BigDecimal valor) {
        BigDecimal paso = meta.subtract(valorInicial).abs();
        if (paso.signum() == 0) {
            paso = meta.abs().multiply(PASO_SI_NO_HAY_RECORRIDO);
        }
        BigDecimal nueva = tipo.sentido() == SentidoMiniObjetivo.SUBE
                ? valor.max(meta).add(paso)
                : valor.min(meta).subtract(paso).max(BigDecimal.ZERO);
        if (tipo == TipoMiniObjetivo.MEJORAR_MARGEN) {
            nueva = nueva.min(TOPE_DE_MARGEN);
        }
        boolean pideMas = tipo.sentido() == SentidoMiniObjetivo.SUBE
                ? nueva.compareTo(meta) > 0
                : nueva.compareTo(meta) < 0;
        return pideMas ? Optional.of(nueva.setScale(2, RoundingMode.HALF_UP)) : Optional.empty();
    }

    /** El mismo mini-objetivo con otra meta, que arranca del valor al cierre: es el que sigue a uno logrado. */
    public CandidatoMiniObjetivo conMeta(BigDecimal valor, BigDecimal nuevaMeta, String justificacionNueva) {
        return new CandidatoMiniObjetivo(tipo, registroId, referencia, valor.setScale(2, RoundingMode.HALF_UP),
                nuevaMeta, aporteEstimado, titulo, justificacionNueva, false);
    }

    /** Qué parte del camino del valor inicial a la meta se recorrió, de 0 a 1. */
    public BigDecimal avance() {
        boolean sube = tipo.sentido() == SentidoMiniObjetivo.SUBE;
        BigDecimal total = sube ? meta.subtract(valorInicial) : valorInicial.subtract(meta);
        if (total.signum() <= 0) {
            return cumplido() ? BigDecimal.ONE : BigDecimal.ZERO;
        }
        BigDecimal recorrido = sube ? valorActual.subtract(valorInicial) : valorInicial.subtract(valorActual);
        return recorrido.divide(total, 4, RoundingMode.HALF_UP).max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }

    /**
     * Registra lo que se midió hoy: si el valor mejora (en el sentido de su tipo) deja de estar sin avance; si no, y
     * todavía no lo estaba, empieza a estarlo desde hoy. Si con eso llega a la meta, queda logrado.
     */
    public void medir(BigDecimal valor, LocalDate hoy, OffsetDateTime ahora) {
        exigirEnCurso();
        if (mejora(valor)) {
            sinAvanceDesde = null;
        } else if (sinAvanceDesde == null) {
            sinAvanceDesde = hoy;
        }
        valorActual = valor.setScale(2, RoundingMode.HALF_UP);
        actualizadoEn = ahora;
        if (cumplido()) {
            estado = EstadoMiniObjetivo.LOGRADO;
            sinAvanceDesde = null;
        }
    }

    /** Lo que el monitoreo detecta de este mini-objetivo hoy; vacío si no hay nada para destacar. */
    public Optional<HitoObjetivo> hito(LocalDate hoy) {
        if (estado == EstadoMiniObjetivo.LOGRADO) {
            return Optional.of(HitoObjetivo.MINI_LOGRADO);
        }
        if (estaEnCurso() && sinAvanceDesde != null && !sinAvanceDesde.isAfter(hoy.minusDays(DIAS_SIN_AVANCE))) {
            return Optional.of(HitoObjetivo.SIN_AVANCE);
        }
        return Optional.empty();
    }

    private boolean mejora(BigDecimal valor) {
        return tipo.sentido() == SentidoMiniObjetivo.SUBE
                ? valor.compareTo(valorActual) > 0
                : valor.compareTo(valorActual) < 0;
    }

    /** La persona lo cambió por {@code nuevo}; el lugar queda libre para el reemplazo. */
    public void reemplazarPor(MiniObjetivoId nuevo, OffsetDateTime ahora) {
        exigirEnCurso();
        estado = EstadoMiniObjetivo.REEMPLAZADO;
        reemplazadoPor = nuevo;
        actualizadoEn = ahora;
    }

    /**
     * Lo logró (recién o antes) y lo sigue {@code nuevo}, con una meta más alta.
     *
     * @throws BusinessRuleException si ya lo siguió otro o fue reemplazado o quitado
     */
    public void lograrYSeguirCon(MiniObjetivoId nuevo, OffsetDateTime ahora) {
        if (!estaEnCurso() && !(estado == EstadoMiniObjetivo.LOGRADO && reemplazadoPor == null)) {
            throw new BusinessRuleException("El mini-objetivo ya fue reajustado");
        }
        estado = EstadoMiniObjetivo.LOGRADO;
        sinAvanceDesde = null;
        reemplazadoPor = nuevo;
        actualizadoEn = ahora;
    }

    /**
     * El cierre semanal pone a {@code nuevo} en su lugar: si estaba en curso queda reemplazado; si estaba logrado o
     * quitado, conserva su estado y sólo apunta al que lo siguió.
     *
     * @throws BusinessRuleException si su lugar ya lo ocupó otro
     */
    public void cederLugarA(MiniObjetivoId nuevo, OffsetDateTime ahora) {
        if (estaEnCurso()) {
            reemplazarPor(nuevo, ahora);
            return;
        }
        if (!esperaReajuste()) {
            throw new BusinessRuleException("El mini-objetivo ya fue reajustado");
        }
        reemplazadoPor = nuevo;
        actualizadoEn = ahora;
    }

    /** La persona lo quitó; el lugar queda libre. */
    public void abandonar(OffsetDateTime ahora) {
        exigirEnCurso();
        estado = EstadoMiniObjetivo.ABANDONADO;
        actualizadoEn = ahora;
    }

    /** @throws BusinessRuleException si ya fue logrado, reemplazado o quitado */
    public void exigirEnCurso() {
        if (!estaEnCurso()) {
            throw new BusinessRuleException("El mini-objetivo ya no está en curso");
        }
    }
}
