package com.ensambla.backend.infrastructure.presentation.dto.reposicion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PedidoProveedorWebRequest(
        @Size(max = 120) String proveedor,
        @NotEmpty @Size(max = 500) List<@Valid LineaPedidoWebRequest> productos
) {
}
