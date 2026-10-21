package com.ensambla.backend.domain.service.procesamiento;

import com.ensambla.backend.domain.model.lectura.ImagenCuaderno;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.procesamiento.BloqueDeImagen;
import com.ensambla.backend.domain.model.procesamiento.LecturaImagen;
import com.ensambla.backend.domain.model.procesamiento.PropuestaLectura;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Lee una foto contra un bloque y devuelve los registros y las columnas nuevas para revisar. Es la misma lectura que
 * el lote de fotos (el bloque elegido es el destino y se cruzan columnas por nombre y sinónimos), pero sin guardar
 * nada: la respuesta es inmediata y la persona decide después qué incorporar.
 */
@RequiredArgsConstructor
public class LecturaImagenService {

    private final LectorCuadernoPort lectorCuadernoPort;
    private final PropuestaLecturaService propuestaLecturaService;

    public LecturaImagen leer(ImagenCuaderno imagen, BloqueDeImagen bloque, boolean destinoExistente) {
        LecturaCuaderno lectura = lectorCuadernoPort.leer(
                List.of(imagen.comoPagina()), List.of(bloque.paraElLector()), bloque.id());
        PropuestaLectura propuesta = propuestaLecturaService.proponer(
                lectura, List.of(bloque.paraLaPropuesta()), bloque.id());
        return LecturaImagen.desde(propuesta, bloque, destinoExistente);
    }
}
