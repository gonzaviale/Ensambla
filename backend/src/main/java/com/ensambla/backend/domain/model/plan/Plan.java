package com.ensambla.backend.domain.model.plan;

import com.ensambla.backend.domain.model.enums.CodigoPlan;
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

    private BigDecimal precioMensual;

    private String moneda;

    private Integer limiteProyectos;

    private Long limiteRegistros;

    private Integer limitePaginasIaMensuales;

    private List<String> funciones;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;

}
