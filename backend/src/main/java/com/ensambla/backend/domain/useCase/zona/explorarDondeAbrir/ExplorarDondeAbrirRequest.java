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
        Map<ZonaId, BigDecimal> alquileres,
        PuntoElegido punto) {

    /** Sin punto elegido en el mapa: sólo se comparan las zonas precargadas. */
    public ExplorarDondeAbrirRequest(UsuarioId usuarioId, RubroNegocio rubro, PublicoDondeAbrir publico,
                                     Map<FactorDondeAbrir, Integer> importancia, Limites limites,
                                     Map<ZonaId, BigDecimal> alquileres) {
        this(usuarioId, rubro, publico, importancia, limites, alquileres, null);
    }

    /** Un lugar cualquiera del mapa que la persona quiere medir en vivo. */
    public record PuntoElegido(BigDecimal latitud, BigDecimal longitud) {
    }

    public record Limites(BigDecimal distanciaMaxKm, Integer competenciaMax, Integer escuelasMin,
                          Integer parquesMin, Integer transporteMin, BigDecimal alquilerMax) {
    }
}
