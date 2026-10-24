package com.ensambla.backend.domain.service.sami;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.sami.ConsultaIaSami;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.model.sami.DiccionarioSami;
import com.ensambla.backend.domain.model.sami.InterpretacionSami;
import com.ensambla.backend.domain.model.sami.PreguntaDiccionario;
import com.ensambla.backend.domain.model.sami.PropuestaIaSami;
import com.ensambla.backend.domain.model.sami.RespuestaSami;
import com.ensambla.backend.domain.port.SamiIaPort;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tercera etapa de Sami (T-117): consulta a la IA sólo cuando el diccionario y las preguntas no alcanzaron.
 * Reserva la consulta del día del negocio antes de llamar (así un rechazo por límite no cuesta nada), registra
 * los tokens gastados y valida que la respuesta referencie una plantilla existente.
 * <p>
 * El día del límite es UTC, igual que el de la lectura de cuadernos.
 */
public class RespaldoIaSamiService {

    static final String LIMITE_ALCANZADO =
            "Hoy ya usaste todas las consultas con IA. Probá mañana o armá tu sistema a mano.";
    static final String IA_NO_DISPONIBLE = "Sami no pudo pensar la propuesta ahora. Probá de nuevo en un rato.";
    static final String RESPUESTA_INVALIDA =
            "Sami no pudo armar una propuesta con lo que contaste. Sumá más detalle o armá tu sistema a mano.";

    private final SamiIaPort samiIaPort;
    private final ConsumoSamiRepository consumoSamiRepository;
    private final Map<String, PreguntaDiccionario> preguntas;

    public RespaldoIaSamiService(SamiIaPort samiIaPort, ConsumoSamiRepository consumoSamiRepository,
                                 DiccionarioSami diccionario) {
        this.samiIaPort = samiIaPort;
        this.consumoSamiRepository = consumoSamiRepository;
        this.preguntas = diccionario.preguntas().stream()
                .collect(Collectors.toMap(PreguntaDiccionario::id, pregunta -> pregunta));
    }

    /**
     * @param limiteDiario consultas con IA por negocio y por día
     * @throws BusinessRuleException si se agotó el límite, la IA falló o su respuesta no es utilizable
     */
    public InterpretacionSami proponer(UUID negocioId, String texto, List<RespuestaSami> respuestas,
                                       List<PlantillaProyecto> plantillas, OffsetDateTime ahora, int limiteDiario) {
        LocalDate fecha = ahora.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
        if (!consumoSamiRepository.reservar(negocioId, fecha, limiteDiario, ahora)) {
            throw new BusinessRuleException(LIMITE_ALCANZADO);
        }
        PropuestaIaSami propuesta = consultar(new ConsultaIaSami(texto, aclaraciones(respuestas), plantillas));
        ConsumoIaSami consumo = propuesta.consumo();
        consumoSamiRepository.registrar(negocioId, fecha, consumo, ahora);
        return propuesta.aInterpretacion(plantillas)
                .orElseThrow(() -> new BusinessRuleException(RESPUESTA_INVALIDA));
    }

    private PropuestaIaSami consultar(ConsultaIaSami consulta) {
        try {
            return samiIaPort.proponer(consulta);
        } catch (RuntimeException e) {
            throw new BusinessRuleException(IA_NO_DISPONIBLE);
        }
    }

    private List<String> aclaraciones(List<RespuestaSami> respuestas) {
        return respuestas.stream()
                .map(this::aclaracion)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<String> aclaracion(RespuestaSami respuesta) {
        return Optional.ofNullable(preguntas.get(respuesta.preguntaId()))
                .flatMap(pregunta -> pregunta.opciones().stream()
                        .filter(opcion -> opcion.id().equals(respuesta.opcionId()))
                        .findFirst()
                        .map(opcion -> pregunta.texto() + " → " + opcion.texto()));
    }
}
