package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.inventario.MovimientoStockRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
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
            NegocioRepository negocioRepository,
            BloqueRepository bloqueRepository,
            CampoRepository campoRepository,
            RegistroRepository registroRepository,
            MovimientoStockRepository movimientoStockRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new RegistrarMovimiento(negocioRepository, bloqueRepository, campoRepository, registroRepository,
                movimientoStockRepository, transaccionPort, clock);
    }

    @Bean
    public ListarMovimientos listarMovimientos(
            NegocioRepository negocioRepository,
            BloqueRepository bloqueRepository,
            CampoRepository campoRepository,
            RegistroRepository registroRepository,
            MovimientoStockRepository movimientoStockRepository) {
        return new ListarMovimientos(
                negocioRepository, bloqueRepository, campoRepository, registroRepository, movimientoStockRepository);
    }

    @Bean
    public ConsultarStock consultarStock(NegocioRepository negocioRepository,
                                         BloqueRepository bloqueRepository,
                                         CampoRepository campoRepository,
                                         RegistroRepository registroRepository,
                                         MovimientoStockRepository movimientoStockRepository) {
        return new ConsultarStock(negocioRepository, bloqueRepository, campoRepository, registroRepository,
                movimientoStockRepository);
    }

}
