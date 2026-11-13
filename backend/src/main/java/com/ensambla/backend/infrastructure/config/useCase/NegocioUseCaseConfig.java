package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.GeocodificadorPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.negocio.actualizarNegocio.ActualizarNegocio;
import com.ensambla.backend.domain.useCase.negocio.obtenerConfiguracionNegocio.ObtenerConfiguracionNegocio;
import com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta.ObtenerResumenDeLaCuenta;
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
    public ObtenerResumenDeLaCuenta obtenerResumenDeLaCuenta(
            NegocioRepository negocioRepository,
            SistemaRepository sistemaRepository,
            DatosDeProyectoService datosDeProyectoService,
            AvisoRepository avisoRepository,
            Clock clock) {
        return new ObtenerResumenDeLaCuenta(
                negocioRepository, sistemaRepository, datosDeProyectoService, avisoRepository, clock);
    }
}
