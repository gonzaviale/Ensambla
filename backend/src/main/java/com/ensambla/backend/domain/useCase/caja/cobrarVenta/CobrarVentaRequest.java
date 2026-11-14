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
                                 String medio, BigDecimal recibido, List<PagoRequest> pagos) {

    public CobrarVentaRequest(UsuarioId usuarioId, VentaCajaId id, ProyectoId proyectoId,
            BloqueId inventarioId, BloqueId ventasId, List<LineaRequest> lineas, String medio, BigDecimal recibido) {
        this(usuarioId, id, proyectoId, inventarioId, ventasId, lineas, medio, recibido, null);
    }

    public record PagoRequest(String medio, BigDecimal importe) {
    }

    public record LineaRequest(RegistroId productoId, BigDecimal cantidad, BigDecimal precio, String modo) {
    }

    public String huella() {
        String base = proyectoId + ":" + inventarioId + ":" + ventasId + ":" + medio + ":" + recibido + ":" + lineas;
        return pagos == null ? base : base + ":" + pagos;
    }
}
