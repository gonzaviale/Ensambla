package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.inventario.MovimientoStockRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.inventario.InventarioService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.domain.useCase.inventario.consultarStock.ConsultarStock;
import com.ensambla.backend.domain.useCase.inventario.listarMovimientos.ListarMovimientos;
import com.ensambla.backend.domain.useCase.inventario.registrarMovimiento.RegistrarMovimiento;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class InventarioUseCaseConfig {

    @Bean
    public RegistrarMovimiento registrarMovimiento(
            NegocioService negocioService,
            BloqueService bloqueService,
            CampoService campoService,
            RegistroDatosService registroDatosService,
            InventarioService inventarioService,
            MovimientoStockRepository movimientoStockRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new RegistrarMovimiento(
                negocioService, bloqueService, campoService, registroDatosService, inventarioService,
                movimientoStockRepository, transaccionPort, clock);
    }

    @Bean
    public ListarMovimientos listarMovimientos(
            NegocioService negocioService,
            BloqueService bloqueService,
            CampoService campoService,
            RegistroRepository registroRepository,
            MovimientoStockRepository movimientoStockRepository) {
        return new ListarMovimientos(
                negocioService, bloqueService, campoService, registroRepository, movimientoStockRepository);
    }

    @Bean
    public ConsultarStock consultarStock(NegocioService negocioService,
                                         BloqueService bloqueService,
                                         CampoService campoService,
                                         RegistroDatosService registroDatosService,
                                         InventarioService inventarioService) {
        return new ConsultarStock(negocioService, bloqueService, campoService, registroDatosService,
                inventarioService);
    }

    @Bean
    public InventarioService inventarioService(MovimientoStockRepository movimientoStockRepository) {
        return new InventarioService(movimientoStockRepository);
    }
}
