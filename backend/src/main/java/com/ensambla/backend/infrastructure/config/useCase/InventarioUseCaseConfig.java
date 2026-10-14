package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.inventario.ConsultarStockMapper;
import com.ensambla.backend.domain.mapper.inventario.ListarMovimientosMapper;
import com.ensambla.backend.domain.mapper.inventario.MovimientoItemMapper;
import com.ensambla.backend.domain.mapper.inventario.RegistrarMovimientoMapper;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.inventario.InventarioService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.domain.useCase.inventario.ConsultarStockUseCase;
import com.ensambla.backend.domain.useCase.inventario.ListarMovimientosUseCase;
import com.ensambla.backend.domain.useCase.inventario.RegistrarMovimientoUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class InventarioUseCaseConfig {

    @Bean
    public MovimientoItemMapper movimientoItemMapper() {
        return new MovimientoItemMapper();
    }

    @Bean
    public RegistrarMovimientoMapper registrarMovimientoMapper(MovimientoItemMapper movimientoItemMapper) {
        return new RegistrarMovimientoMapper(movimientoItemMapper);
    }

    @Bean
    public ListarMovimientosMapper listarMovimientosMapper(MovimientoItemMapper movimientoItemMapper) {
        return new ListarMovimientosMapper(movimientoItemMapper);
    }

    @Bean
    public ConsultarStockMapper consultarStockMapper() {
        return new ConsultarStockMapper();
    }

    @Bean
    public RegistrarMovimientoUseCase registrarMovimientoUseCase(NegocioService negocioService,
                                                                 BloqueService bloqueService,
                                                                 CampoService campoService,
                                                                 RegistroDatosService registroDatosService,
                                                                 InventarioService inventarioService,
                                                                 RegistrarMovimientoMapper registrarMovimientoMapper,
                                                                 Clock clock) {
        return new RegistrarMovimientoUseCase(negocioService, bloqueService, campoService, registroDatosService,
                inventarioService, registrarMovimientoMapper, clock);
    }

    @Bean
    public ListarMovimientosUseCase listarMovimientosUseCase(NegocioService negocioService,
                                                             BloqueService bloqueService,
                                                             CampoService campoService,
                                                             RegistroDatosService registroDatosService,
                                                             InventarioService inventarioService,
                                                             ListarMovimientosMapper listarMovimientosMapper) {
        return new ListarMovimientosUseCase(negocioService, bloqueService, campoService, registroDatosService,
                inventarioService, listarMovimientosMapper);
    }

    @Bean
    public ConsultarStockUseCase consultarStockUseCase(NegocioService negocioService,
                                                       BloqueService bloqueService,
                                                       CampoService campoService,
                                                       RegistroDatosService registroDatosService,
                                                       InventarioService inventarioService,
                                                       ConsultarStockMapper consultarStockMapper) {
        return new ConsultarStockUseCase(negocioService, bloqueService, campoService, registroDatosService,
                inventarioService, consultarStockMapper);
    }
}
