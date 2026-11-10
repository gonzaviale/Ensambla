package com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir;

import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.FactorDondeAbrir;
import com.ensambla.backend.domain.model.zona.PublicoDondeAbrir;
import com.ensambla.backend.domain.model.zona.ZonaId;

import java.math.BigDecimal;
import java.util.Map;

public record ExplorarDondeAbrirRequest(
        UsuarioId usuarioId,
        RubroNegocio rubro,
        PublicoDondeAbrir publico,
        Map<FactorDondeAbrir, Integer> importancia,
        Limites limites,
        Map<ZonaId, BigDecimal> alquileres) {

    public record Limites(BigDecimal distanciaMaxKm, Integer competenciaMax, Integer escuelasMin,
                          Integer parquesMin, Integer transporteMin, BigDecimal alquilerMax) {
    }
}
