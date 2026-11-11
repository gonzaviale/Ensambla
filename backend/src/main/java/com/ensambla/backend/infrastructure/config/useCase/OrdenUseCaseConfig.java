package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.pedido.ClienteWhatsappRepository;
import com.ensambla.backend.domain.repository.pedido.OrdenCompraRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.useCase.orden.cambiarEstadoOrden.CambiarEstadoOrden;
import com.ensambla.backend.domain.useCase.orden.listarOrdenes.ListarOrdenes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class OrdenUseCaseConfig {

    @Bean
    public ListarOrdenes listarOrdenes(
            NegocioRepository negocioRepository,
            OrdenCompraRepository ordenRepository,
            ClienteWhatsappRepository clienteRepository) {
        return new ListarOrdenes(
                negocioRepository,
                ordenRepository,
                clienteRepository);
    }

    @Bean
    public CambiarEstadoOrden cambiarEstadoOrden(
            NegocioRepository negocioRepository,
            OrdenCompraRepository ordenRepository,
            ClienteWhatsappRepository clienteRepository,
            RegistroRepository registroRepository,
            CampoRepository campoRepository,
            MensajeriaWhatsappPort mensajeria,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new CambiarEstadoOrden(
                negocioRepository,
                ordenRepository,
                clienteRepository,
                registroRepository,
                campoRepository,
                mensajeria,
                transaccionPort,
                clock);
    }
}
