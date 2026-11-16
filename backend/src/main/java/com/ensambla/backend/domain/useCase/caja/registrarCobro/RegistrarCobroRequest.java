package com.ensambla.backend.domain.useCase.caja.registrarCobro;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.caja.CobroCajaId;
import com.ensambla.backend.domain.model.caja.VentaCajaId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.math.BigDecimal;
import java.util.List;

public record RegistrarCobroRequest(UsuarioId usuarioId, CobroCajaId id, ProyectoId proyectoId,
                                    BloqueId inventarioId, List<AplicacionRequest> aplicaciones) {

    public record AplicacionRequest(VentaCajaId ventaId, List<PagoRequest> pagos) {
    }

    public record PagoRequest(String medio, BigDecimal importe) {
    }

    public String huella() {
        return proyectoId + ":" + inventarioId + ":" + aplicaciones;
    }
}
