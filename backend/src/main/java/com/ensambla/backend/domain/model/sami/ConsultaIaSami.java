package com.ensambla.backend.domain.model.sami;

import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;

import java.util.List;

/**
 * Lo que se le manda a la IA: el texto de la persona, lo que ya aclaró contestando y las plantillas entre las
 * que puede elegir (vocabulario cerrado, así la respuesta siempre referencia una plantilla existente).
 *
 * @param aclaraciones pares «pregunta → opción elegida» en lenguaje natural
 */
public record ConsultaIaSami(String texto, List<String> aclaraciones, List<PlantillaProyecto> plantillas) {
}
