package com.ensambla.backend.domain.useCase.caja.consultarCaja;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.time.LocalDate;

public record ConsultarCajaRequest(UsuarioId usuarioId, ProyectoId proyectoId, BloqueId inventarioId,
                                    LocalDate fecha, int pagina) {
}
