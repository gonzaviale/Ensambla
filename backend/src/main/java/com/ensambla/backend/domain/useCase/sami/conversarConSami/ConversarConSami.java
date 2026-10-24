package com.ensambla.backend.domain.useCase.sami.conversarConSami;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla.BloquePlantilla;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.sami.ColumnaDiccionario;
import com.ensambla.backend.domain.model.sami.ColumnaSugerida;
import com.ensambla.backend.domain.model.sami.ConsultaIaSami;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.model.sami.DiccionarioSami;
import com.ensambla.backend.domain.model.sami.InterpretacionSami;
import com.ensambla.backend.domain.model.sami.OpcionDiccionario;
import com.ensambla.backend.domain.model.sami.OpcionSami;
import com.ensambla.backend.domain.model.sami.PasoConversacionSami;
import com.ensambla.backend.domain.model.sami.PlantillaDiccionario;
import com.ensambla.backend.domain.model.sami.PreguntaDiccionario;
import com.ensambla.backend.domain.model.sami.PreguntaSami;
import com.ensambla.backend.domain.model.sami.PropuestaIaSami;
import com.ensambla.backend.domain.model.sami.PuntajePlantilla;
import com.ensambla.backend.domain.model.sami.RespuestaSami;
import com.ensambla.backend.domain.model.sami.TerminoDiccionario;
import com.ensambla.backend.domain.model.sami.TipoPaso;
import com.ensambla.backend.domain.port.SamiIaPort;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Sami propone una plantilla a partir de lo que cuenta la persona, en tres etapas:
 * <ol>
 *     <li>Deduce plantilla y columnas extra con el diccionario, sin costo (T-115). La confianza es
 *     {@code fuerza × separación}: la fuerza crece con los puntos de la mejor plantilla hasta
 *     {@link #PUNTAJE_SUFICIENTE}, y la separación cae si la segunda anda cerca. Elegir una opción de una
 *     pregunta suma {@link #PUNTAJE_SUFICIENTE} a su plantilla y descarta a las demás plantillas que esa pregunta
 *     separaba.</li>
 *     <li>Si el diccionario no alcanza, pregunta lo que mejor separa a las plantillas candidatas, con respuestas
 *     rápidas (T-116).</li>
 *     <li>Sin preguntas útiles o agotado el máximo, consulta a la IA (T-117): reserva la consulta del día del
 *     negocio antes de llamar (así un rechazo por límite no cuesta nada), registra los tokens gastados y valida que
 *     la respuesta referencie una plantilla existente. El día del límite es UTC.</li>
 * </ol>
 * No guarda estado: el texto y las respuestas dadas hasta ahora alcanzan para reconstruirlo.
 */
public class ConversarConSami {

    static final int PUNTAJE_SUFICIENTE = 5;
    static final String LIMITE_ALCANZADO =
            "Hoy ya usaste todas las consultas con IA. Probá mañana o armá tu sistema a mano.";
    static final String IA_NO_DISPONIBLE = "Sami no pudo pensar la propuesta ahora. Probá de nuevo en un rato.";
    static final String RESPUESTA_INVALIDA =
            "Sami no pudo armar una propuesta con lo que contaste. Sumá más detalle o armá tu sistema a mano.";

    private final NegocioService negocioService;
    private final PlantillaRepository plantillaRepository;
    private final SamiIaPort samiIaPort;
    private final ConsumoSamiRepository consumoSamiRepository;
    private final Clock clock;
    private final double umbralConfianza;
    private final int maxPreguntas;
    private final int limiteDiarioIa;
    private final List<PlantillaNormalizada> plantillas;
    private final List<ColumnaNormalizada> columnas;
    private final List<PreguntaDiccionario> preguntas;
    private final Map<String, PreguntaDiccionario> preguntasPorId;

    public ConversarConSami(NegocioService negocioService, PlantillaRepository plantillaRepository,
                            DiccionarioSami diccionario, SamiIaPort samiIaPort,
                            ConsumoSamiRepository consumoSamiRepository, Clock clock, double umbralConfianza,
                            int maxPreguntas, int limiteDiarioIa) {
        this.negocioService = negocioService;
        this.plantillaRepository = plantillaRepository;
        this.samiIaPort = samiIaPort;
        this.consumoSamiRepository = consumoSamiRepository;
        this.clock = clock;
        this.umbralConfianza = umbralConfianza;
        this.maxPreguntas = maxPreguntas;
        this.limiteDiarioIa = limiteDiarioIa;
        this.plantillas = diccionario.plantillas().stream().map(ConversarConSami::normalizar).toList();
        this.columnas = diccionario.columnas().stream().map(ConversarConSami::normalizar).toList();
        this.preguntas = diccionario.preguntas();
        this.preguntasPorId = preguntas.stream()
                .collect(Collectors.toMap(PreguntaDiccionario::id, pregunta -> pregunta));
    }

    public ConversarConSamiResponse execute(ConversarConSamiRequest request) {
        List<PlantillaProyecto> disponibles = plantillaRepository.findAllParaProyectoActivas();
        List<RespuestaSami> respuestas = request.respuestasSami();
        PasoConversacionSami paso = siguientePaso(request.texto(), respuestas, disponibles);
        if (paso.tipo() != TipoPaso.RESPALDO_IA) {
            return ConversarConSamiResponse.de(paso);
        }
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        InterpretacionSami propuesta = proponerConIa(
                negocioId, request.texto(), respuestas, disponibles, OffsetDateTime.now(clock));
        return ConversarConSamiResponse.de(propuesta, PropuestaSamiItem.ORIGEN_IA, paso.preguntasHechas());
    }

    // --- segunda etapa: preguntas ---

    private PasoConversacionSami siguientePaso(String texto, List<RespuestaSami> respuestas,
                                               List<PlantillaProyecto> disponibles) {
        InterpretacionSami interpretacion = interpretar(texto, disponibles, respuestas);
        if (interpretacion.alcanza(umbralConfianza)) {
            return new PasoConversacionSami(TipoPaso.PROPUESTA, null, interpretacion, respuestas.size());
        }
        boolean puedePreguntar = respuestas.size() < maxPreguntas && !respondioNingunaDeEstas(respuestas);
        Optional<PreguntaDiccionario> pregunta = puedePreguntar
                ? elegirPregunta(interpretacion, respuestas, disponibles)
                : Optional.empty();
        return pregunta
                .map(elegida -> new PasoConversacionSami(
                        TipoPaso.PREGUNTA, aPregunta(elegida), interpretacion, respuestas.size()))
                .orElseGet(() -> new PasoConversacionSami(
                        TipoPaso.RESPALDO_IA, null, interpretacion, respuestas.size()));
    }

    /**
     * Si la persona dijo «otra cosa» ninguna plantilla de la pregunta le sirve: seguir preguntando por ellas no
     * ayuda y corresponde pensarlo con la IA.
     */
    private boolean respondioNingunaDeEstas(List<RespuestaSami> respuestas) {
        return respuestas.stream().anyMatch(respuesta -> preguntas.stream()
                .filter(pregunta -> pregunta.id().equals(respuesta.preguntaId()))
                .flatMap(pregunta -> pregunta.opciones().stream())
                .anyMatch(opcion -> opcion.id().equals(respuesta.opcionId()) && opcion.plantilla() == null));
    }

    /** La no hecha que separa a más candidatas; a igualdad, la más específica (menos opciones) y luego la 1.ª. */
    private Optional<PreguntaDiccionario> elegirPregunta(InterpretacionSami interpretacion,
                                                         List<RespuestaSami> respuestas,
                                                         List<PlantillaProyecto> disponibles) {
        Set<String> hechas = respuestas.stream().map(RespuestaSami::preguntaId).collect(Collectors.toSet());
        Set<String> candidatas = candidatas(interpretacion, disponibles);
        return preguntas.stream()
                .filter(pregunta -> !hechas.contains(pregunta.id()))
                .filter(pregunta -> separadas(pregunta, candidatas) > 0)
                .max(Comparator.<PreguntaDiccionario>comparingLong(pregunta -> separadas(pregunta, candidatas))
                        .thenComparing(Comparator.<PreguntaDiccionario>comparingInt(
                                pregunta -> pregunta.opciones().size()).reversed()));
    }

    private Set<String> candidatas(InterpretacionSami interpretacion, List<PlantillaProyecto> disponibles) {
        if (interpretacion.puntajes().isEmpty()) {
            return disponibles.stream().map(plantilla -> TextoNormalizado.clave(plantilla.nombre()))
                    .collect(Collectors.toSet());
        }
        return interpretacion.puntajes().stream()
                .map(PuntajePlantilla::plantilla)
                .map(TextoNormalizado::clave)
                .collect(Collectors.toSet());
    }

    private long separadas(PreguntaDiccionario pregunta, Set<String> candidatas) {
        return pregunta.opciones().stream()
                .map(OpcionDiccionario::plantilla)
                .filter(plantilla -> plantilla != null)
                .map(TextoNormalizado::clave)
                .filter(candidatas::contains)
                .count();
    }

    private PreguntaSami aPregunta(PreguntaDiccionario pregunta) {
        return new PreguntaSami(
                pregunta.id(),
                pregunta.texto(),
                pregunta.opciones().stream().map(opcion -> new OpcionSami(opcion.id(), opcion.texto())).toList());
    }

    // --- primera etapa: diccionario ---

    /**
     * @param disponibles plantillas activas; sólo se sugiere una de ellas y no se repiten sus columnas
     * @param respuestas  opciones que la persona fue eligiendo en las preguntas de Sami
     * @throws BusinessRuleException si una respuesta no corresponde a ninguna pregunta u opción conocida
     */
    private InterpretacionSami interpretar(String texto, List<PlantillaProyecto> disponibles,
                                           List<RespuestaSami> respuestas) {
        Ajustes ajustes = ajustesDe(respuestas);
        List<String> palabras = TextoNormalizado.palabras(texto);
        List<PuntajePlantilla> puntajes = puntuar(palabras, disponibles, ajustes);
        if (puntajes.isEmpty()) {
            return new InterpretacionSami(null, null, List.of(), 0.0, List.of());
        }
        PuntajePlantilla mejor = puntajes.getFirst();
        PlantillaProyecto elegida = disponibles.stream()
                .filter(plantilla -> TextoNormalizado.clave(plantilla.nombre()).equals(
                        TextoNormalizado.clave(mejor.plantilla())))
                .findFirst()
                .orElseThrow();
        return new InterpretacionSami(
                elegida.id(),
                elegida.nombre(),
                columnasExtra(palabras, elegida),
                confianza(puntajes),
                puntajes);
    }

    private Ajustes ajustesDe(List<RespuestaSami> respuestas) {
        Map<String, Integer> bonus = new HashMap<>();
        Set<String> descartadas = new HashSet<>();
        for (RespuestaSami respuesta : respuestas) {
            PreguntaDiccionario pregunta = preguntasPorId.get(respuesta.preguntaId());
            OpcionDiccionario opcion = opcionDe(pregunta, respuesta);
            pregunta.opciones().stream()
                    .map(OpcionDiccionario::plantilla)
                    .filter(plantilla -> plantilla != null)
                    .map(TextoNormalizado::clave)
                    .forEach(descartadas::add);
            if (opcion.plantilla() != null) {
                String clave = TextoNormalizado.clave(opcion.plantilla());
                descartadas.remove(clave);
                bonus.merge(clave, PUNTAJE_SUFICIENTE, Integer::sum);
            }
        }
        return new Ajustes(bonus, descartadas);
    }

    private OpcionDiccionario opcionDe(PreguntaDiccionario pregunta, RespuestaSami respuesta) {
        if (pregunta == null) {
            throw new BusinessRuleException("La pregunta " + respuesta.preguntaId() + " no existe");
        }
        return pregunta.opciones().stream()
                .filter(opcion -> opcion.id().equals(respuesta.opcionId()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "La opción " + respuesta.opcionId() + " no existe en la pregunta " + pregunta.id()));
    }

    private List<PuntajePlantilla> puntuar(List<String> palabras, List<PlantillaProyecto> disponibles,
                                           Ajustes ajustes) {
        Set<String> nombres = disponibles.stream()
                .map(plantilla -> TextoNormalizado.clave(plantilla.nombre()))
                .collect(Collectors.toSet());
        return plantillas.stream()
                .filter(plantilla -> nombres.contains(TextoNormalizado.clave(plantilla.nombre())))
                .filter(plantilla -> !ajustes.descartadas().contains(TextoNormalizado.clave(plantilla.nombre())))
                .map(plantilla -> new PuntajePlantilla(plantilla.nombre(), puntaje(palabras, plantilla, ajustes)))
                .filter(puntaje -> puntaje.puntaje() > 0)
                .sorted(Comparator.comparingInt(PuntajePlantilla::puntaje).reversed())
                .toList();
    }

    private int puntaje(List<String> palabras, PlantillaNormalizada plantilla, Ajustes ajustes) {
        int deTexto = plantilla.terminos().stream()
                .filter(termino -> TextoNormalizado.contiene(palabras, termino.palabras()))
                .mapToInt(TerminoNormalizado::peso)
                .sum();
        return deTexto + ajustes.bonus().getOrDefault(TextoNormalizado.clave(plantilla.nombre()), 0);
    }

    private double confianza(List<PuntajePlantilla> puntajes) {
        int mejor = puntajes.getFirst().puntaje();
        int segunda = puntajes.size() > 1 ? puntajes.get(1).puntaje() : 0;
        double fuerza = Math.min(1.0, (double) mejor / PUNTAJE_SUFICIENTE);
        double separacion = (double) (mejor - segunda) / mejor;
        return Math.round(fuerza * separacion * 100.0) / 100.0;
    }

    private List<ColumnaSugerida> columnasExtra(List<String> palabras, PlantillaProyecto plantilla) {
        Set<String> existentes = plantilla.estructura().bloques().stream()
                .flatMap(bloque -> columnasDe(bloque).stream())
                .collect(Collectors.toSet());
        return columnas.stream()
                .filter(columna -> columna.aplicaA(plantilla.nombre()))
                .filter(columna -> !existentes.contains(TextoNormalizado.clave(columna.nombre())))
                .filter(columna -> columna.disparadores().stream()
                        .anyMatch(disparador -> TextoNormalizado.contiene(palabras, disparador)))
                .map(columna -> new ColumnaSugerida(columna.nombre(), columna.tipo(), columna.opciones()))
                .toList();
    }

    private static List<String> columnasDe(BloquePlantilla bloque) {
        return bloque.campos().stream().map(campo -> TextoNormalizado.clave(campo.nombre())).toList();
    }

    private static PlantillaNormalizada normalizar(PlantillaDiccionario plantilla) {
        return new PlantillaNormalizada(plantilla.plantilla(), sinRepetidos(plantilla.terminos()));
    }

    /** Dos formas que comparten raíz ({@code vendo}, {@code vendí}) cuentan una sola vez, con el mayor peso. */
    private static List<TerminoNormalizado> sinRepetidos(List<TerminoDiccionario> terminos) {
        Map<List<String>, Integer> pesos = new LinkedHashMap<>();
        terminos.forEach(termino -> pesos.merge(
                TextoNormalizado.palabras(termino.termino()),
                termino.peso(),
                Math::max));
        return pesos.entrySet().stream()
                .map(entrada -> new TerminoNormalizado(entrada.getKey(), entrada.getValue()))
                .toList();
    }

    private static ColumnaNormalizada normalizar(ColumnaDiccionario columna) {
        return new ColumnaNormalizada(
                columna.nombre(),
                columna.tipo(),
                columna.opciones(),
                columna.disparadores().stream().map(TextoNormalizado::palabras).toList(),
                columna.plantillas().stream().map(TextoNormalizado::clave).collect(Collectors.toSet()));
    }

    // --- tercera etapa: respaldo con IA ---

    /** @throws BusinessRuleException si se agotó el límite, la IA falló o su respuesta no es utilizable */
    private InterpretacionSami proponerConIa(UUID negocioId, String texto, List<RespuestaSami> respuestas,
                                             List<PlantillaProyecto> disponibles, OffsetDateTime ahora) {
        LocalDate fecha = ahora.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
        if (!consumoSamiRepository.reservar(negocioId, fecha, limiteDiarioIa, ahora)) {
            throw new BusinessRuleException(LIMITE_ALCANZADO);
        }
        PropuestaIaSami propuesta = consultarIa(new ConsultaIaSami(texto, aclaraciones(respuestas), disponibles));
        ConsumoIaSami consumo = propuesta.consumo();
        consumoSamiRepository.registrar(negocioId, fecha, consumo, ahora);
        return propuesta.aInterpretacion(disponibles)
                .orElseThrow(() -> new BusinessRuleException(RESPUESTA_INVALIDA));
    }

    private PropuestaIaSami consultarIa(ConsultaIaSami consulta) {
        try {
            return samiIaPort.proponer(consulta);
        } catch (RuntimeException e) {
            throw new BusinessRuleException(IA_NO_DISPONIBLE);
        }
    }

    /** Cada respuesta como «pregunta → opción»; ya se validaron al interpretar el texto. */
    private List<String> aclaraciones(List<RespuestaSami> respuestas) {
        return respuestas.stream()
                .map(respuesta -> {
                    PreguntaDiccionario pregunta = preguntasPorId.get(respuesta.preguntaId());
                    return pregunta.texto() + " → " + opcionDe(pregunta, respuesta).texto();
                })
                .toList();
    }

    private record Ajustes(Map<String, Integer> bonus, Set<String> descartadas) {
    }

    private record PlantillaNormalizada(String nombre, List<TerminoNormalizado> terminos) {
    }

    private record TerminoNormalizado(List<String> palabras, int peso) {
    }

    private record ColumnaNormalizada(
            String nombre,
            TipoCampo tipo,
            List<String> opciones,
            List<List<String>> disparadores,
            Set<String> plantillas
    ) {

        boolean aplicaA(String plantilla) {
            return plantillas.isEmpty() || plantillas.contains(TextoNormalizado.clave(plantilla));
        }
    }
}
