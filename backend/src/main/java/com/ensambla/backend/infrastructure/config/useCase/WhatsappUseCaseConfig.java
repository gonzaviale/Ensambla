package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.whatsapp.WhatsappItemMapper;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.whatsapp.RecepcionWhatsappService;
import com.ensambla.backend.domain.service.whatsapp.VinculacionWhatsappService;
import com.ensambla.backend.domain.useCase.whatsapp.ConsultarVinculacionWhatsappUseCase;
import com.ensambla.backend.domain.useCase.whatsapp.DesvincularWhatsappUseCase;
import com.ensambla.backend.domain.useCase.whatsapp.GenerarCodigoVinculacionUseCase;
import com.ensambla.backend.domain.useCase.whatsapp.ProcesarMensajeEntranteUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class WhatsappUseCaseConfig {

    @Bean
    public WhatsappItemMapper whatsappItemMapper() {
        return new WhatsappItemMapper();
    }

    @Bean
    public GenerarCodigoVinculacionUseCase generarCodigoVinculacionUseCase(
            NegocioService negocioService,
            VinculacionWhatsappService vinculacionService,
            MensajeriaWhatsappPort mensajeria,
            WhatsappItemMapper mapper,
            Clock clock) {
        return new GenerarCodigoVinculacionUseCase(negocioService, vinculacionService, mensajeria, mapper, clock);
    }

    @Bean
    public ConsultarVinculacionWhatsappUseCase consultarVinculacionWhatsappUseCase(
            VinculacionWhatsappService vinculacionService,
            WhatsappItemMapper mapper) {
        return new ConsultarVinculacionWhatsappUseCase(vinculacionService, mapper);
    }

    @Bean
    public DesvincularWhatsappUseCase desvincularWhatsappUseCase(VinculacionWhatsappService vinculacionService) {
        return new DesvincularWhatsappUseCase(vinculacionService);
    }

    @Bean
    public ProcesarMensajeEntranteUseCase procesarMensajeEntranteUseCase(
            VinculacionWhatsappService vinculacionService,
            RecepcionWhatsappService recepcionService,
            MensajeriaWhatsappPort mensajeria,
            Clock clock) {
        return new ProcesarMensajeEntranteUseCase(vinculacionService, recepcionService, mensajeria, clock);
    }
}
