package com.ensambla.backend.domain.useCase.caja.cerrarCaja;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.caja.CierreCajaId;
import com.ensambla.backend.domain.model.common.Huella;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.math.BigDecimal;

public record CerrarCajaRequest(UsuarioId usuarioId, CierreCajaId id, ProyectoId proyectoId,
                                 BloqueId inventarioId, String revision, BigDecimal fondoInicial,
                                 BigDecimal entradas, BigDecimal retiros, BigDecimal contado, String nota) {

    public String huella() {
        return Huella.de(proyectoId + ":" + inventarioId + ":" + revision + ":" + fondoInicial + ":" +
                entradas + ":" + retiros + ":" + contado + ":" + nota);
    }
}
