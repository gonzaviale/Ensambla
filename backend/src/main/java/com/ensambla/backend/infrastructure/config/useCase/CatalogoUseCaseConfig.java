package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.CatalogoProductosPort;
import com.ensambla.backend.domain.useCase.catalogo.buscarProducto.BuscarProducto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class CatalogoUseCaseConfig {

    @Bean
    public BuscarProducto buscarProducto(CatalogoProductosPort catalogoProductosPort, Clock clock) {
        return new BuscarProducto(catalogoProductosPort, clock);
    }
}
