package com.ensambla.backend.domain.useCase.caja.cobrarVenta;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.caja.VentaCajaId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.math.BigDecimal;
import java.util.List;

public record CobrarVentaRequest(UsuarioId usuarioId, VentaCajaId id, ProyectoId proyectoId,
                                 BloqueId inventarioId, BloqueId ventasId, List<LineaRequest> lineas,
                                 String medio, BigDecimal recibido) {

    public record LineaRequest(RegistroId productoId, BigDecimal cantidad, BigDecimal precio, String modo) {
    }

    public String huella() {
        return proyectoId + ":" + inventarioId + ":" + ventasId + ":" + medio + ":" + recibido + ":" + lineas;
    }
}
