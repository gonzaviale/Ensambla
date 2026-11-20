package com.ensambla.backend.domain.model.sami.taller.herramienta;

import com.ensambla.backend.domain.model.sami.taller.ArgumentosDeHerramienta;
import com.ensambla.backend.domain.model.sami.taller.CatalogoTaller;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.OpcionesTaller;
import com.ensambla.backend.domain.model.sami.taller.TurnoDelTaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** No cambia el sistema: es una pregunta con respuestas rápidas, y no cuenta contra el tope de acciones. */
public final class OfrecerOpciones extends HerramientaDeTaller {

    private static final Logger log = LoggerFactory.getLogger(OfrecerOpciones.class);

    OfrecerOpciones() {
        super(HerramientaTaller.OFRECER_OPCIONES);
    }

    @Override
    public void aplicarEn(TurnoDelTaller turno, ArgumentosDeHerramienta argumentos) {
        List<OpcionesTaller> dudas = dudasDe(argumentos);
        turno.sumarPreguntas(dudas);
        if (dudas.isEmpty()) {
            log.warn("ofrecer_opciones descartada por inválida: {}", argumentos.comoMapa());
        }
    }

    /**
     * Las dudas de una llamada: una sola ({@code pregunta} y {@code opciones} en los argumentos) o varias
     * ({@code preguntas}: una lista de ellas). Las que no valen se descartan.
     */
    private List<OpcionesTaller> dudasDe(ArgumentosDeHerramienta argumentos) {
        return Stream.concat(argumentos.de("preguntas").objetos().stream(), Stream.of(argumentos))
                .flatMap(duda -> opciones(duda).stream())
                .toList();
    }

    private Optional<OpcionesTaller> opciones(ArgumentosDeHerramienta duda) {
        List<String> opciones = duda.de("opciones").textos().stream().limit(CatalogoTaller.MAX_OPCIONES).toList();
        return duda.de("pregunta").texto(CatalogoTaller.MAX_LARGO_PREGUNTA)
                .filter(pregunta -> opciones.size() >= CatalogoTaller.MIN_OPCIONES)
                .map(pregunta -> new OpcionesTaller(temaDe(duda), pregunta, opciones));
    }

    private String temaDe(ArgumentosDeHerramienta duda) {
        return duda.de("tema").texto(CatalogoTaller.MAX_LARGO_TEMA).orElse(null);
    }
}
