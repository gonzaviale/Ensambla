package com.ensambla.backend.domain.service.sami;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.MensajeTaller;
import com.ensambla.backend.domain.model.sami.taller.RespuestaTaller;
import com.ensambla.backend.domain.model.sami.taller.ResultadoTallerIa;
import com.ensambla.backend.domain.model.sami.taller.ValidacionTaller;
import com.ensambla.backend.domain.port.TallerIaPort;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Un turno de la charla con Sami en el taller: el modelo conversa y pide herramientas, el dominio valida lo que pidió
 * y devuelve acciones para que el taller las aplique. Reserva la consulta del día del negocio antes de llamar (un
 * rechazo por límite no cuesta nada) y registra los tokens gastados. El día del límite es UTC.
 */
@Service
@RequiredArgsConstructor
public class TallerSamiService {

    static final String LIMITE_ALCANZADO =
            "Hoy ya usaste todas las consultas con IA. Probá mañana o seguí armándolo a mano.";
    static final String IA_NO_DISPONIBLE = "Sami no pudo pensar la respuesta ahora. Probá de nuevo en un rato.";
    static final String SIN_CAMBIOS = "No pude hacer eso con lo que tengo. ¿Me lo contás de otra forma?";

    private final TallerIaPort tallerIaPort;
    private final ConsumoSamiRepository consumoSamiRepository;
    private final HerramientasTallerService herramientasTallerService;

    /**
     * @param limiteDiario consultas con IA por negocio y por día (se comparte el contador con el asistente de
     *                     plantillas)
     * @throws BusinessRuleException si se agotó el límite o la IA falló
     */
    public RespuestaTaller conversar(UUID negocioId, String mensaje, EstadoTaller estado,
                                     List<MensajeTaller> historial, OffsetDateTime ahora, int limiteDiario) {
        LocalDate fecha = ahora.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
        if (!consumoSamiRepository.reservar(negocioId, fecha, limiteDiario, ahora)) {
            throw new BusinessRuleException(LIMITE_ALCANZADO);
        }
        ResultadoTallerIa resultado = consultar(new ConsultaTaller(mensaje, estado, historial));
        consumoSamiRepository.registrar(negocioId, fecha, resultado.consumo(), ahora);
        ValidacionTaller validacion = herramientasTallerService.validar(resultado.llamadas(), estado);
        return new RespuestaTaller(texto(resultado.texto(), validacion), validacion.acciones(), validacion.opciones());
    }

    private ResultadoTallerIa consultar(ConsultaTaller consulta) {
        try {
            return tallerIaPort.conversar(consulta);
        } catch (RuntimeException e) {
            throw new BusinessRuleException(IA_NO_DISPONIBLE);
        }
    }

    /** Si el modelo no escribió nada, Sami dice algo razonable a partir de lo que quedó. */
    private String texto(String texto, ValidacionTaller validacion) {
        if (texto != null && !texto.isBlank()) {
            return texto.strip();
        }
        if (validacion.opciones() != null) {
            return validacion.opciones().pregunta();
        }
        int cambios = validacion.acciones().size();
        if (cambios == 0) {
            return SIN_CAMBIOS;
        }
        return cambios == 1
                ? "Listo, hice el cambio. Mirá cómo queda."
                : "Listo, hice " + cambios + " cambios. Mirá cómo queda.";
    }
}
