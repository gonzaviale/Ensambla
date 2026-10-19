package com.ensambla.backend.domain.service.sami;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.CatalogoTaller;
import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.MensajeTaller;
import com.ensambla.backend.domain.model.sami.taller.RespuestaTaller;
import com.ensambla.backend.domain.model.sami.taller.ResultadoTallerIa;
import com.ensambla.backend.domain.model.sami.taller.ValidacionTaller;
import com.ensambla.backend.domain.port.TallerIaPort;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Un turno de la charla con Sami en el taller: el modelo conversa y pide herramientas, el dominio valida lo que pidió
 * y devuelve acciones para que el taller las aplique. Reserva la consulta del día del negocio antes de llamar (un
 * rechazo por límite no cuesta nada) y registra los tokens gastados. El día del límite es UTC.
 */
@Service
public class TallerSamiService {

    static final String LIMITE_ALCANZADO =
            "Hoy ya usaste todas las consultas con IA. Probá mañana o seguí armándolo a mano.";
    static final String IA_NO_DISPONIBLE = "Sami no pudo pensar la respuesta ahora. Probá de nuevo en un rato.";
    static final String SIN_CAMBIOS = "No pude hacer eso con lo que tengo. ¿Me lo contás de otra forma?";

    private interface Descripcion extends BiFunction<Map<String, Object>, EstadoTaller, String> {
    }

    private final TallerIaPort tallerIaPort;
    private final ConsumoSamiRepository consumoSamiRepository;
    private final HerramientasTallerService herramientasTallerService;
    private final Map<HerramientaTaller, Descripcion> descripciones = new EnumMap<>(HerramientaTaller.class);

    public TallerSamiService(TallerIaPort tallerIaPort, ConsumoSamiRepository consumoSamiRepository,
                             HerramientasTallerService herramientasTallerService) {
        this.tallerIaPort = tallerIaPort;
        this.consumoSamiRepository = consumoSamiRepository;
        this.herramientasTallerService = herramientasTallerService;
        descripciones.put(HerramientaTaller.AGREGAR_PIEZA,
                (args, estado) -> "Sumé " + CatalogoTaller.TIPOS_DE_PIEZA.get(args.get("tipo")).toLowerCase());
        descripciones.put(HerramientaTaller.QUITAR_PIEZA,
                (args, estado) -> "Quité " + pieza(args, estado));
        descripciones.put(HerramientaTaller.RENOMBRAR_PIEZA,
                (args, estado) -> "Renombré " + pieza(args, estado) + " a " + args.get("nombre"));
        descripciones.put(HerramientaTaller.RENOMBRAR_SISTEMA,
                (args, estado) -> "Llamé al sistema " + args.get("nombre"));
        descripciones.put(HerramientaTaller.AGREGAR_CAMPO,
                (args, estado) -> "Sumé " + args.get("etiqueta") + " a " + pieza(args, estado));
        descripciones.put(HerramientaTaller.QUITAR_CAMPO,
                (args, estado) -> "Quité " + args.get("campo") + " de " + pieza(args, estado));
        descripciones.put(HerramientaTaller.SEPARAR_EN_SECCION,
                (args, estado) -> "Pasé " + pieza(args, estado) + " a la sección " + args.get("seccion"));
        descripciones.put(HerramientaTaller.CAMBIAR_DISTRIBUCION,
                (args, estado) -> "Cambié la distribución");
    }

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
        return new RespuestaTaller(
                texto(resultado.texto(), validacion, estado), validacion.acciones(), validacion.opciones());
    }

    private ResultadoTallerIa consultar(ConsultaTaller consulta) {
        try {
            return tallerIaPort.conversar(consulta);
        } catch (RuntimeException e) {
            throw new BusinessRuleException(IA_NO_DISPONIBLE);
        }
    }

    /**
     * Si el modelo no escribió nada (a veces sólo invoca herramientas), Sami cuenta lo que quedó hecho o hace la
     * pregunta de la botonera.
     */
    private String texto(String texto, ValidacionTaller validacion, EstadoTaller estado) {
        if (texto != null && !texto.isBlank()) {
            return texto.strip();
        }
        if (validacion.opciones() != null) {
            return validacion.opciones().pregunta();
        }
        if (validacion.acciones().isEmpty()) {
            return SIN_CAMBIOS;
        }
        String hechas = validacion.acciones().stream()
                .map(accion -> descripciones.get(HerramientaTaller.deNombre(accion.herramienta()).orElseThrow())
                        .apply(accion.argumentos(), estado))
                .collect(Collectors.joining(". "));
        return hechas + ". Mirá cómo queda.";
    }

    /** El nombre de la pieza de la acción: la del estado o, si es nueva en el turno, la del catálogo. */
    private String pieza(Map<String, Object> argumentos, EstadoTaller estado) {
        Object referencia = argumentos.get("pieza");
        return estado.piezas().stream()
                .filter(pieza -> referencia.equals(pieza.id()))
                .map(pieza -> pieza.nombre().toLowerCase(Locale.ROOT))
                .findFirst()
                .orElseGet(() -> CatalogoTaller.TIPOS_DE_PIEZA.get(referencia.toString()).toLowerCase(Locale.ROOT));
    }
}
