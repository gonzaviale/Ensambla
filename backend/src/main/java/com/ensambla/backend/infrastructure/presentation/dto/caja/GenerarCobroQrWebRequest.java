package com.ensambla.backend.infrastructure.presentation.dto.caja;

import com.ensambla.backend.domain.model.caja.CobroQrId;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record GenerarCobroQrWebRequest(@NotNull CobroQrId id,
                                       @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2)
                                       BigDecimal importe) {
}
