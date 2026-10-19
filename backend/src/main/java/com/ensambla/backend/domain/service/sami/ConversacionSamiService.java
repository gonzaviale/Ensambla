package com.ensambla.backend.domain.service.sami;

import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.sami.DiccionarioSami;
import com.ensambla.backend.domain.model.sami.InterpretacionSami;
import com.ensambla.backend.domain.model.sami.OpcionDiccionario;
import com.ensambla.backend.domain.model.sami.OpcionSami;
import com.ensambla.backend.domain.model.sami.PasoConversacionSami;
import com.ensambla.backend.domain.model.sami.PreguntaDiccionario;
import com.ensambla.backend.domain.model.sami.PreguntaSami;
import com.ensambla.backend.domain.model.sami.PuntajePlantilla;
import com.ensambla.backend.domain.model.sami.RespuestaSami;
import com.ensambla.backend.domain.model.sami.TipoPaso;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Segunda etapa de Sami (T-116): si el diccionario no alcanza, pregunta lo que mejor separa a las plantillas
 * candidatas, con respuestas rápidas. Cada respuesta se vuelve a puntuar con {@link InterpretacionSamiService}.
 * Sin preguntas útiles o agotado el máximo, deriva al respaldo con IA (T-117).
 * <p>
 * No guarda estado: el texto y las respuestas dadas hasta ahora alcanzan para reconstruirlo.
 */
@Service
public class ConversacionSamiService {

    private final InterpretacionSamiService interpretacionSamiService;
    private final List<PreguntaDiccionario> preguntas;

    public ConversacionSamiService(InterpretacionSamiService interpretacionSamiService, DiccionarioSami diccionario) {
        this.interpretacionSamiService = interpretacionSamiService;
        this.preguntas = diccionario.preguntas();
    }

    /**
     * @param umbral        confianza mínima para proponer sin preguntar más
     * @param maxPreguntas  cuántas preguntas se le hacen como máximo a la persona
     */
    public PasoConversacionSami siguientePaso(String texto, List<RespuestaSami> respuestas,
                                              List<PlantillaProyecto> disponibles, double umbral, int maxPreguntas) {
        InterpretacionSami interpretacion = interpretacionSamiService.interpretar(texto, disponibles, respuestas);
        if (interpretacion.alcanza(umbral)) {
            return new PasoConversacionSami(TipoPaso.PROPUESTA, null, interpretacion, respuestas.size());
        }
        Optional<PreguntaDiccionario> pregunta = respuestas.size() < maxPreguntas
                ? elegirPregunta(interpretacion, respuestas, disponibles)
                : Optional.empty();
        return pregunta
                .map(elegida -> new PasoConversacionSami(
                        TipoPaso.PREGUNTA, aPregunta(elegida), interpretacion, respuestas.size()))
                .orElseGet(() -> new PasoConversacionSami(
                        TipoPaso.RESPALDO_IA, null, interpretacion, respuestas.size()));
    }

    /** La no hecha que separa a más candidatas; a igualdad, la más específica (menos opciones) y luego la primera. */
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
            return disponibles.stream().map(plantilla -> NormalizadorTexto.clave(plantilla.nombre()))
                    .collect(Collectors.toSet());
        }
        return interpretacion.puntajes().stream()
                .map(PuntajePlantilla::plantilla)
                .map(NormalizadorTexto::clave)
                .collect(Collectors.toSet());
    }

    private long separadas(PreguntaDiccionario pregunta, Set<String> candidatas) {
        return pregunta.opciones().stream()
                .map(OpcionDiccionario::plantilla)
                .filter(plantilla -> plantilla != null)
                .map(NormalizadorTexto::clave)
                .filter(candidatas::contains)
                .count();
    }

    private PreguntaSami aPregunta(PreguntaDiccionario pregunta) {
        return new PreguntaSami(
                pregunta.id(),
                pregunta.texto(),
                pregunta.opciones().stream().map(opcion -> new OpcionSami(opcion.id(), opcion.texto())).toList());
    }
}
