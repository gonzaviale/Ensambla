package com.ensambla.backend.domain.model.plan;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plan {

    private static final String MONEDA_DE_COBRO = "ARS";

    private PlanId id;

    private CodigoPlan codigo;

    private String nombre;

    private Boolean configurado;

    private Boolean limitesConfigurados;

    private BigDecimal precioMensual;

    private String moneda;

    private Integer limiteProyectos;

    private Long limiteRegistros;

    private Integer limitePaginasIaMensuales;

    private Integer limiteCartelesIaMensuales;
    private Integer limiteObjetivosIaMensuales;

    private List<String> funciones;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;

    public void validarLimitesConfigurados() {
        if (!Boolean.TRUE.equals(limitesConfigurados)) {
            throw new RestriccionPlanException("PLAN_NO_CONFIGURADO", "Los límites del plan no están configurados");
        }
    }

    public void validarPrecioMensualConfigurado() {
        if (!tienePrecioMensualEnPesos()) {
            throw new BusinessRuleException("El precio de Premium todavía no está configurado.");
        }
    }

    public void validarProyectos(long existentes) {
        validarCupo(existentes, 1, limiteProyectos, "LIMITE_SISTEMAS", "sistemas");
    }

    public void validarRegistros(long existentes, long nuevos) {
        validarCupo(existentes, nuevos, limiteRegistros, "LIMITE_REGISTROS", "registros");
    }

    public void validarPaginasIa(long usadasYReservadas, int nuevas) {
        validarCupo(usadasYReservadas, nuevas, limitePaginasIaMensuales, "LIMITE_IA_MENSUAL",
                "análisis de foto por mes");
    }

    public void validarCartelesIa(long usadosEnElMes) {
        validarCupo(usadosEnElMes, 1, limiteCartelesIaMensuales, "LIMITE_CARTELES_IA", "carteles con IA por mes");
    }

    /**
     * Si el mes ya tiene todos los objetivos con IA que permite el plan, no entra otro.
     *
     * @throws RestriccionPlanException si se agotó la cuota mensual
     */
    public void validarObjetivosIa(long usados) {
        if (new CuotaObjetivosIa(usados, limiteObjetivosIaMensuales).agotada()) {
            throw new RestriccionPlanException("LIMITE_OBJETIVOS_IA",
                    "Ya armaste los " + limiteObjetivosIaMensuales + " objetivos con Sami que incluye tu plan este "
                            + "mes. Mirá los planes en «Planes» para armar más. Tus datos se conservan.");
        }
    }

    public void validarFuncion(FuncionPlan funcion) {
        if (!funciones.contains(funcion.name())) {
            throw new RestriccionPlanException("FUNCION_PREMIUM",
                    "Esta función requiere un plan premium activo. Tus datos se conservan.");
        }
    }

    private boolean tienePrecioMensualEnPesos() {
        return Boolean.TRUE.equals(configurado)
                && precioMensual != null
                && precioMensual.signum() > 0
                && MONEDA_DE_COBRO.equals(moneda);
    }

    private void validarCupo(long usadas, long nuevas, Number limite, String codigo, String recurso) {
        if (limite != null && nuevas > Math.max(0, limite.longValue() - usadas)) {
            throw new RestriccionPlanException(codigo,
                    "Tu plan permite " + limite + " " + recurso + ". Tus datos se conservan.");
        }
    }
}
