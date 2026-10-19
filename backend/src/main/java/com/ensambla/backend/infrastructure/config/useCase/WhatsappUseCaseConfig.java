package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.whatsapp.GenerarCodigoWhatsappMapper;
import com.ensambla.backend.domain.mapper.whatsapp.RecibirMensajesWhatsappMapper;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.whatsapp.MensajeWhatsappService;
import com.ensambla.backend.domain.service.whatsapp.VinculoWhatsappService;
import com.ensambla.backend.domain.useCase.whatsapp.GenerarCodigoWhatsappUseCase;
import com.ensambla.backend.domain.useCase.whatsapp.RecibirMensajesWhatsappUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class WhatsappUseCaseConfig {

    @Bean
    public GenerarCodigoWhatsappMapper generarCodigoWhatsappMapper() {
        return new GenerarCodigoWhatsappMapper();
    }

    @Bean
    public RecibirMensajesWhatsappMapper recibirMensajesWhatsappMapper() {
        return new RecibirMensajesWhatsappMapper();
    }

    @Bean
    public GenerarCodigoWhatsappUseCase generarCodigoWhatsappUseCase(
            NegocioService negocioService,
            VinculoWhatsappService vinculoWhatsappService,
            GenerarCodigoWhatsappMapper generarCodigoWhatsappMapper,
            Clock clock) {
        return new GenerarCodigoWhatsappUseCase(
                negocioService,
                vinculoWhatsappService,
                generarCodigoWhatsappMapper,
                clock);
    }

    @Bean
    public RecibirMensajesWhatsappUseCase recibirMensajesWhatsappUseCase(
            MensajeWhatsappService mensajeWhatsappService,
            RecibirMensajesWhatsappMapper recibirMensajesWhatsappMapper,
            Clock clock) {
        return new RecibirMensajesWhatsappUseCase(
                mensajeWhatsappService,
                recibirMensajesWhatsappMapper,
                clock);
    }
}
