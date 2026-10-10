package com.ensambla.backend.infrastructure.ia;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "ensambla.ia.procesamiento")
public record ProcesamientoIaProperties(
        @DefaultValue("3") @Min(1) @Max(100) int maxIntentos,
        @DefaultValue("100") @Min(1) int limiteDiario,
        @DefaultValue("30s") Duration demoraReintento,
        @DefaultValue("120s") Duration vigenciaReclamo,
        @DefaultValue("../datos/ia/fotos-privadas") @NotBlank String archivosRaiz,
        @DecimalMin("0") BigDecimal precioEntradaMillon,
        @DecimalMin("0") BigDecimal precioSalidaMillon
) {
}
