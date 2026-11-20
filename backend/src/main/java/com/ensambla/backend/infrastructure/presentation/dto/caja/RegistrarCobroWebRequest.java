package com.ensambla.backend.infrastructure.presentation.dto.caja;

import com.ensambla.backend.domain.model.caja.CobroQrId;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.caja.CobroCajaId;
import com.ensambla.backend.domain.model.caja.VentaCajaId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record RegistrarCobroWebRequest(@NotNull CobroCajaId id, @NotNull ProyectoId proyectoId,
                                       @NotNull BloqueId inventarioId,
                                       @NotEmpty @Size(max = 100) List<@NotNull @Valid AplicacionWebRequest> aplicaciones,
                                       CobroQrId cobroQrId) {

    public record AplicacionWebRequest(@NotNull VentaCajaId ventaId,
                                       @NotEmpty @Size(max = 3) List<@NotNull @Valid PagoWebRequest> pagos) {
    }

    public record PagoWebRequest(@NotNull @Pattern(regexp = "EFECTIVO|TRANSFERENCIA|TARJETA") String medio,
                                 @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal importe) {
    }
}
