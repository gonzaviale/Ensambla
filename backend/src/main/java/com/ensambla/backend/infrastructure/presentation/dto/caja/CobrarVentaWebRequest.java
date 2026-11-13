package com.ensambla.backend.infrastructure.presentation.dto.caja;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.caja.VentaCajaId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CobrarVentaWebRequest(@NotNull VentaCajaId id, @NotNull ProyectoId proyectoId,
                                    @NotNull BloqueId inventarioId, @NotNull BloqueId ventasId,
                                    @NotEmpty @Size(max = 100) List<@NotNull @Valid LineaWebRequest> lineas,
                                    @NotNull @Pattern(regexp = "EFECTIVO|TRANSFERENCIA|TARJETA") String medio,
                                    @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal recibido) {

    public record LineaWebRequest(@NotNull RegistroId productoId,
                                  @NotNull @DecimalMin("0.001") @Digits(integer = 8, fraction = 3) BigDecimal cantidad,
                                  @NotNull @DecimalMin("0.01") @Digits(integer = 9, fraction = 2) BigDecimal precio,
                                  @NotNull @Pattern(regexp = "UNIDAD|PESO") String modo) {
    }
}
