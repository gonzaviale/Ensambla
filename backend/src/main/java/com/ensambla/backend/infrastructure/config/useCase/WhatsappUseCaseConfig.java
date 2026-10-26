package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.whatsapp.CodigoVinculacionRepository;
import com.ensambla.backend.domain.repository.whatsapp.MensajeWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import com.ensambla.backend.domain.useCase.whatsapp.consultarVinculacionWhatsapp.ConsultarVinculacionWhatsapp;
import com.ensambla.backend.domain.useCase.whatsapp.desvincularWhatsapp.DesvincularWhatsapp;
import com.ensambla.backend.domain.useCase.whatsapp.generarCodigoVinculacion.GenerarCodigoVinculacion;
import com.ensambla.backend.domain.useCase.whatsapp.procesarMensajeEntrante.ProcesarMensajeEntrante;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class WhatsappUseCaseConfig {

    @Bean
    public GenerarCodigoVinculacion generarCodigoVinculacion(
            NegocioRepository negocioRepository,
            CodigoVinculacionRepository codigoRepository,
            MensajeriaWhatsappPort mensajeria,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new GenerarCodigoVinculacion(
                negocioRepository,
                codigoRepository,
                mensajeria,
                transaccionPort,
                clock);
    }

    @Bean
    public ConsultarVinculacionWhatsapp consultarVinculacionWhatsapp(VinculoWhatsappRepository vinculoRepository) {
        return new ConsultarVinculacionWhatsapp(vinculoRepository);
    }

    @Bean
    public DesvincularWhatsapp desvincularWhatsapp(
            VinculoWhatsappRepository vinculoRepository,
            TransaccionPort transaccionPort) {
        return new DesvincularWhatsapp(
                vinculoRepository,
                transaccionPort);
    }

    @Bean
    public ProcesarMensajeEntrante procesarMensajeEntrante(
            NegocioRepository negocioRepository,
            CodigoVinculacionRepository codigoRepository,
            VinculoWhatsappRepository vinculoRepository,
            MensajeWhatsappRepository mensajeRepository,
            MensajeriaWhatsappPort mensajeria,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new ProcesarMensajeEntrante(
                negocioRepository,
                codigoRepository,
                vinculoRepository,
                mensajeRepository,
                mensajeria,
                transaccionPort,
                clock);
    }
}
