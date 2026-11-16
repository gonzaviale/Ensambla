package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.cartel.FuentesDePromocion;
import com.ensambla.backend.domain.port.DibujanteDeCartelPort;
import com.ensambla.backend.domain.port.ExportadorDeCartelPort;
import com.ensambla.backend.domain.port.RedaccionDeCartelIaPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.marca.KitDeMarcaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.cartel.armarOpcionesDeCartel.ArmarOpcionesDeCartel;
import com.ensambla.backend.domain.useCase.cartel.exportarCartel.ExportarCartel;
import com.ensambla.backend.domain.useCase.cartel.redactarTextosDeCartel.RedactarTextosDeCartel;
import com.ensambla.backend.domain.useCase.cartel.sugerirPromociones.SugerirPromociones;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;

@Configuration
public class CartelUseCaseConfig {

    @Bean
    public ArmarOpcionesDeCartel armarOpcionesDeCartel(
            NegocioRepository negocioRepository,
            KitDeMarcaRepository kitDeMarcaRepository,
            DibujanteDeCartelPort dibujanteDeCartelPort,
            Clock clock) {
        return new ArmarOpcionesDeCartel(negocioRepository, kitDeMarcaRepository, dibujanteDeCartelPort, clock);
    }

    @Bean
    public RedactarTextosDeCartel redactarTextosDeCartel(
            NegocioRepository negocioRepository,
            KitDeMarcaRepository kitDeMarcaRepository,
            ControlPlanService controlPlanService,
            RedaccionDeCartelIaPort redaccionDeCartelIaPort,
            Clock clock) {
        return new RedactarTextosDeCartel(
                negocioRepository, kitDeMarcaRepository, controlPlanService, redaccionDeCartelIaPort, clock);
    }

    @Bean
    public SugerirPromociones sugerirPromociones(
            NegocioRepository negocioRepository,
            AvisoRepository avisoRepository) {
        return new SugerirPromociones(negocioRepository, avisoRepository, FuentesDePromocion.todas());
    }

    @Bean
    public ExportarCartel exportarCartel(
            NegocioRepository negocioRepository,
            KitDeMarcaRepository kitDeMarcaRepository,
            List<ExportadorDeCartelPort> exportadores,
            Clock clock) {
        return new ExportarCartel(negocioRepository, kitDeMarcaRepository, exportadores, clock);
    }
}
