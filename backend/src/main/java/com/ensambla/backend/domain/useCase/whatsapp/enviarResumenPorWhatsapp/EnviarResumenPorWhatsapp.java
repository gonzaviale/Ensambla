package com.ensambla.backend.domain.useCase.whatsapp.enviarResumenPorWhatsapp;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.ResumenMatinal;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.preferencia.PreferenciaUsuario;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.preferencia.PreferenciaRepository;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Le manda por WhatsApp, una vez por día y desde la hora configurada, el resumen de lo que Sami encontró en su negocio
 * a quien lo pidió y tiene un número vinculado. Si no hay nada que contar no manda nada, y si falla la entrega
 * vuelve a intentar en la próxima vuelta.
 */
@Slf4j
@RequiredArgsConstructor
public class EnviarResumenPorWhatsapp {

    private final PreferenciaRepository preferenciaRepository;

    private final VinculoWhatsappRepository vinculoWhatsappRepository;

    private final NegocioRepository negocioRepository;

    private final AvisoRepository avisoRepository;

    private final ResumenMatinal resumenMatinal;

    private final MensajeriaWhatsappPort mensajeriaWhatsappPort;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    /** Hora argentina (0 a 23) desde la que se manda el resumen del día. */
    private final int horaDelResumen;

    /** Cuántos resúmenes se mandaron en esta vuelta. */
    public int execute() {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        if (!llegoLaHora(ahora)) {
            return 0;
        }
        LocalDate hoy = ZonaNegocio.dia(ahora);
        int enviados = 0;
        for (PreferenciaUsuario preferencia : sinResumenDe(hoy)) {
            if (enviarA(preferencia, hoy, ahora)) {
                enviados++;
            }
        }
        return enviados;
    }

    private boolean llegoLaHora(OffsetDateTime ahora) {
        return ahora.atZoneSameInstant(ZonaNegocio.ZONA).getHour() >= horaDelResumen;
    }

    private List<PreferenciaUsuario> sinResumenDe(LocalDate hoy) {
        return preferenciaRepository.findAllConAvisosPorWhatsapp().stream()
                .filter(preferencia -> !preferencia.yaRecibioElResumenDe(hoy))
                .toList();
    }

    /** Un problema con una persona no frena a las demás: se anota y se sigue. */
    private boolean enviarA(PreferenciaUsuario preferencia, LocalDate hoy, OffsetDateTime ahora) {
        try {
            return intentarEnviarA(preferencia, hoy, ahora);
        } catch (RuntimeException e) {
            log.warn("No se pudo mandar el resumen de la mañana a la cuenta {}", preferencia.getUsuarioId(), e);
            return false;
        }
    }

    private boolean intentarEnviarA(PreferenciaUsuario preferencia, LocalDate hoy, OffsetDateTime ahora) {
        Optional<VinculoWhatsapp> vinculo = vinculoWhatsappRepository.findByUsuarioId(preferencia.getUsuarioId());
        if (vinculo.isEmpty()) {
            return false;
        }
        Optional<String> mensaje = mensajeDe(preferencia);
        if (mensaje.isEmpty()) {
            return false;
        }
        boolean entregado = mensajeriaWhatsappPort.enviarTexto(vinculo.get().getTelefono(), mensaje.get());
        if (entregado) {
            registrarEnvio(preferencia, hoy, ahora);
        }
        return entregado;
    }

    private Optional<String> mensajeDe(PreferenciaUsuario preferencia) {
        Negocio negocio = negocioRepository.obtenerNegocioDelUsuario(preferencia.getUsuarioId());
        List<Aviso> pendientes = avisosQueQuiereVer(preferencia, negocio.getId());
        return resumenMatinal.armar(preferencia.getNombre(), negocio.getNombre(), pendientes);
    }

    private List<Aviso> avisosQueQuiereVer(PreferenciaUsuario preferencia, NegocioId negocioId) {
        return avisoRepository.findByNegocioIdAndEstado(negocioId, EstadoAviso.PENDIENTE).stream()
                .filter(aviso -> preferencia.recibeAvisosDelGrupo(aviso.getGrupo()))
                .toList();
    }

    private void registrarEnvio(PreferenciaUsuario preferencia, LocalDate hoy, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> guardarEnvio(preferencia, hoy, ahora));
    }

    private void guardarEnvio(PreferenciaUsuario preferencia, LocalDate hoy, OffsetDateTime ahora) {
        preferencia.registrarResumenEnviado(hoy, ahora);
        preferenciaRepository.save(preferencia);
    }
}
