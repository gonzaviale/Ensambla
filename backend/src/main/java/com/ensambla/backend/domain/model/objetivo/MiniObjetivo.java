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

    public boolean estaEnCurso() {
        return estado == EstadoMiniObjetivo.EN_CURSO;
    }

    /** Si el valor actual ya llegó a la meta, según el sentido del tipo. */
    public boolean cumplido() {
        return tipo.sentido() == SentidoMiniObjetivo.SUBE
                ? valorActual.compareTo(meta) >= 0
                : valorActual.compareTo(meta) <= 0;
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

    /** La persona lo cambió por {@code nuevo}; el lugar queda libre para el reemplazo. */
    public void reemplazarPor(MiniObjetivoId nuevo, OffsetDateTime ahora) {
        exigirEnCurso();
        estado = EstadoMiniObjetivo.REEMPLAZADO;
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
