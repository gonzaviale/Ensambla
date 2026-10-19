package com.ensambla.backend.domain.service.sami;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla.BloquePlantilla;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.sami.ColumnaDiccionario;
import com.ensambla.backend.domain.model.sami.ColumnaSugerida;
import com.ensambla.backend.domain.model.sami.DiccionarioSami;
import com.ensambla.backend.domain.model.sami.InterpretacionSami;
import com.ensambla.backend.domain.model.sami.OpcionDiccionario;
import com.ensambla.backend.domain.model.sami.PlantillaDiccionario;
import com.ensambla.backend.domain.model.sami.PreguntaDiccionario;
import com.ensambla.backend.domain.model.sami.PuntajePlantilla;
import com.ensambla.backend.domain.model.sami.RespuestaSami;
import com.ensambla.backend.domain.model.sami.TerminoDiccionario;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Primera etapa de Sami (T-115): deduce plantilla y columnas extra con el diccionario, sin costo.
 * La confianza es {@code fuerza × separación}: la fuerza crece con los puntos de la mejor plantilla hasta
 * {@link #PUNTAJE_SUFICIENTE}, y la separación cae si la segunda anda cerca. Una descripción ambigua queda con
 * poca confianza y es la que Sami aclara con preguntas (T-116) o deriva a la IA (T-117).
 * <p>
 * Elegir una opción de una pregunta suma {@link #PUNTAJE_SUFICIENTE} a su plantilla y descarta a las demás
 * plantillas que esa pregunta separaba.
 */
@Service
public class InterpretacionSamiService {

    static final int PUNTAJE_SUFICIENTE = 5;

    private final List<PlantillaNormalizada> plantillas;
    private final List<ColumnaNormalizada> columnas;
    private final Map<String, PreguntaDiccionario> preguntas;

    public InterpretacionSamiService(DiccionarioSami diccionario) {
        this.plantillas = diccionario.plantillas().stream().map(this::normalizar).toList();
        this.columnas = diccionario.columnas().stream().map(this::normalizar).toList();
        this.preguntas = diccionario.preguntas().stream()
                .collect(Collectors.toMap(PreguntaDiccionario::id, pregunta -> pregunta));
    }

    /**
     * @param disponibles plantillas activas; sólo se sugiere una de ellas y no se repiten sus columnas
     * @param respuestas  opciones que la persona fue eligiendo en las preguntas de Sami
     * @throws BusinessRuleException si una respuesta no corresponde a ninguna pregunta u opción conocida
     */
    public InterpretacionSami interpretar(String texto, List<PlantillaProyecto> disponibles,
                                          List<RespuestaSami> respuestas) {
        Ajustes ajustes = ajustesDe(respuestas);
        List<String> palabras = NormalizadorTexto.palabras(texto);
        List<PuntajePlantilla> puntajes = puntuar(palabras, disponibles, ajustes);
        if (puntajes.isEmpty()) {
            return new InterpretacionSami(null, null, List.of(), 0.0, List.of());
        }
        PuntajePlantilla mejor = puntajes.getFirst();
        PlantillaProyecto elegida = disponibles.stream()
                .filter(plantilla -> NormalizadorTexto.clave(plantilla.nombre()).equals(
                        NormalizadorTexto.clave(mejor.plantilla())))
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
            PreguntaDiccionario pregunta = preguntas.get(respuesta.preguntaId());
            OpcionDiccionario opcion = opcionDe(pregunta, respuesta);
            pregunta.opciones().stream()
                    .map(OpcionDiccionario::plantilla)
                    .filter(plantilla -> plantilla != null)
                    .map(NormalizadorTexto::clave)
                    .forEach(descartadas::add);
            if (opcion.plantilla() != null) {
                String clave = NormalizadorTexto.clave(opcion.plantilla());
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
                .map(plantilla -> NormalizadorTexto.clave(plantilla.nombre()))
                .collect(Collectors.toSet());
        return plantillas.stream()
                .filter(plantilla -> nombres.contains(NormalizadorTexto.clave(plantilla.nombre())))
                .filter(plantilla -> !ajustes.descartadas().contains(NormalizadorTexto.clave(plantilla.nombre())))
                .map(plantilla -> new PuntajePlantilla(plantilla.nombre(), puntaje(palabras, plantilla, ajustes)))
                .filter(puntaje -> puntaje.puntaje() > 0)
                .sorted(Comparator.comparingInt(PuntajePlantilla::puntaje).reversed())
                .toList();
    }

    private int puntaje(List<String> palabras, PlantillaNormalizada plantilla, Ajustes ajustes) {
        int deTexto = plantilla.terminos().stream()
                .filter(termino -> NormalizadorTexto.contiene(palabras, termino.palabras()))
                .mapToInt(TerminoNormalizado::peso)
                .sum();
        return deTexto + ajustes.bonus().getOrDefault(NormalizadorTexto.clave(plantilla.nombre()), 0);
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
                .filter(columna -> !existentes.contains(NormalizadorTexto.clave(columna.nombre())))
                .filter(columna -> columna.disparadores().stream()
                        .anyMatch(disparador -> NormalizadorTexto.contiene(palabras, disparador)))
                .map(columna -> new ColumnaSugerida(columna.nombre(), columna.tipo(), columna.opciones()))
                .toList();
    }

    private List<String> columnasDe(BloquePlantilla bloque) {
        return bloque.campos().stream().map(campo -> NormalizadorTexto.clave(campo.nombre())).toList();
    }

    private PlantillaNormalizada normalizar(PlantillaDiccionario plantilla) {
        return new PlantillaNormalizada(plantilla.plantilla(), sinRepetidos(plantilla.terminos()));
    }

    /** Dos formas que comparten raíz ({@code vendo}, {@code vendí}) cuentan una sola vez, con el mayor peso. */
    private List<TerminoNormalizado> sinRepetidos(List<TerminoDiccionario> terminos) {
        Map<List<String>, Integer> pesos = new LinkedHashMap<>();
        terminos.forEach(termino -> pesos.merge(
                NormalizadorTexto.palabras(termino.termino()),
                termino.peso(),
                Math::max));
        return pesos.entrySet().stream()
                .map(entrada -> new TerminoNormalizado(entrada.getKey(), entrada.getValue()))
                .toList();
    }

    private ColumnaNormalizada normalizar(ColumnaDiccionario columna) {
        return new ColumnaNormalizada(
                columna.nombre(),
                columna.tipo(),
                columna.opciones(),
                columna.disparadores().stream().map(NormalizadorTexto::palabras).toList(),
                columna.plantillas().stream().map(NormalizadorTexto::clave).collect(Collectors.toSet()));
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
            return plantillas.isEmpty() || plantillas.contains(NormalizadorTexto.clave(plantilla));
        }
    }
}
