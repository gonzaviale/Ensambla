package com.ensambla.backend.domain.model.plan;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plan {

    private UUID id;

    private CodigoPlan codigo;

    private String nombre;

    private Boolean configurado;

    private Boolean limitesConfigurados;

    private BigDecimal precioMensual;

    private String moneda;

    private Integer limiteProyectos;

    private Long limiteRegistros;

    private Integer limitePaginasIaMensuales;

    private List<String> funciones;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;

    public void validarLimitesConfigurados() {
        if (!Boolean.TRUE.equals(limitesConfigurados)) {
            throw new RestriccionPlanException("PLAN_NO_CONFIGURADO", "Los límites del plan no están configurados");
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
                "páginas de IA por mes");
    }

    public void validarFuncion(FuncionPlan funcion) {
        if (!funciones.contains(funcion.name())) {
            throw new RestriccionPlanException("FUNCION_PREMIUM",
                    "Esta función requiere un plan premium activo. Tus datos se conservan.");
        }
    }

    private void validarCupo(long usadas, long nuevas, Number limite, String codigo, String recurso) {
        if (limite != null && nuevas > Math.max(0, limite.longValue() - usadas)) {
            throw new RestriccionPlanException(codigo,
                    "Tu plan permite " + limite + " " + recurso + ". Tus datos se conservan.");
        }
    }
}
