package com.ensambla.backend.infrastructure.presentation.dto.caja;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.caja.CierreCajaId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CerrarCajaWebRequest(@NotNull CierreCajaId id, @NotNull ProyectoId proyectoId,
                                    @NotNull BloqueId inventarioId, @NotBlank @Size(max = 64) String revision,
                                    @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal fondoInicial,
                                    @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal entradas,
                                    @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal retiros,
                                    @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal contado,
                                    @NotNull @Size(max = 1000) String nota) {
}
