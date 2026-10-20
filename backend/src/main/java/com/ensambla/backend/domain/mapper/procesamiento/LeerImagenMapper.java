package com.ensambla.backend.domain.mapper.procesamiento;

import com.ensambla.backend.domain.dto.procesamiento.LeerImagenItem;
import com.ensambla.backend.domain.dto.procesamiento.LeerImagenItem.CampoItem;
import com.ensambla.backend.domain.dto.procesamiento.LeerImagenItem.DatoItem;
import com.ensambla.backend.domain.dto.procesamiento.LeerImagenRequest;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.ImagenCuaderno;
import com.ensambla.backend.domain.model.procesamiento.BloqueDeImagen;
import com.ensambla.backend.domain.model.procesamiento.BloqueDeImagen.CampoDeImagen;
import com.ensambla.backend.domain.model.procesamiento.LecturaImagen;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
public class LeerImagenMapper {

    /** Debajo de este valor de confianza un dato se marca como dudoso. */
    private final double umbralConfianza;

    public ImagenCuaderno toImagen(LeerImagenRequest request) {
        return ImagenCuaderno.de(request.contenido());
    }

    public BloqueDeImagen toBloque(LeerImagenRequest request) {
        return BloqueDeImagen.de(request.nombreBloque(), request.campos().stream()
                .map(campo -> CampoDeImagen.nuevo(
                        campo.clave(), campo.nombre(), TipoCampo.valueOf(campo.tipo()), campo.opciones()))
                .toList());
    }

    public LeerImagenItem toResponse(LecturaImagen lectura) {
        return new LeerImagenItem(
                lectura.filas().stream().map(this::fila).toList(),
                lectura.camposNuevos().stream().map(this::campo).toList(),
                lectura.advertencias());
    }

    private Map<String, DatoItem> fila(Map<String, DatoLeido> datos) {
        Map<String, DatoItem> fila = new LinkedHashMap<>();
        datos.forEach((clave, dato) -> fila.put(clave, new DatoItem(dato.valor(), dato.esDudoso(umbralConfianza))));
        return fila;
    }

    private CampoItem campo(CampoPropuesto campo) {
        return new CampoItem(campo.clave(), campo.nombre(), campo.tipo().name(), campo.opciones());
    }
}
