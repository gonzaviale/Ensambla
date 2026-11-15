package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.GeocodificadorPort;
import com.ensambla.backend.domain.port.NegocioActivoPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.negocio.actualizarNegocio.ActualizarNegocio;
import com.ensambla.backend.domain.useCase.negocio.cambiarNegocioActivo.CambiarNegocioActivo;
import com.ensambla.backend.domain.useCase.negocio.crearNegocio.CrearNegocio;
import com.ensambla.backend.domain.useCase.negocio.eliminarNegocio.EliminarNegocio;
import com.ensambla.backend.domain.useCase.negocio.listarNegocios.ListarNegocios;
import com.ensambla.backend.domain.useCase.negocio.obtenerConfiguracionNegocio.ObtenerConfiguracionNegocio;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class NegocioUseCaseConfig {

    @Bean
    public ObtenerConfiguracionNegocio obtenerConfiguracionNegocio(
            NegocioRepository negocioRepository, ZonaRepository zonaRepository) {
        return new ObtenerConfiguracionNegocio(negocioRepository, zonaRepository);
    }

    @Bean
    public ActualizarNegocio actualizarNegocio(
            NegocioRepository negocioRepository,
            ZonaRepository zonaRepository,
            GeocodificadorPort geocodificadorPort,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new ActualizarNegocio(negocioRepository, zonaRepository, geocodificadorPort, transaccionPort, clock);
    }

    @Bean
    public ListarNegocios listarNegocios(
            NegocioRepository negocioRepository, ControlPlanService controlPlanService, Clock clock) {
        return new ListarNegocios(negocioRepository, controlPlanService, clock);
    }

    @Bean
    public CrearNegocio crearNegocio(
            NegocioRepository negocioRepository,
            ZonaRepository zonaRepository,
            GeocodificadorPort geocodificadorPort,
            ControlPlanService controlPlanService,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new CrearNegocio(negocioRepository, zonaRepository, geocodificadorPort,
                controlPlanService, transaccionPort, clock);
    }

    @Bean
    public CambiarNegocioActivo cambiarNegocioActivo(
            NegocioRepository negocioRepository, NegocioActivoPort negocioActivoPort) {
        return new CambiarNegocioActivo(negocioRepository, negocioActivoPort);
    }

    @Bean
    public EliminarNegocio eliminarNegocio(
            NegocioRepository negocioRepository, NegocioActivoPort negocioActivoPort, TransaccionPort transaccionPort) {
        return new EliminarNegocio(negocioRepository, negocioActivoPort, transaccionPort);
    }
}
