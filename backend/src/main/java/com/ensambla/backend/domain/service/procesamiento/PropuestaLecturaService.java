package com.ensambla.backend.domain.service.procesamiento;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.lectura.BloquePropuesto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.PropuestaLectura;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PropuestaLecturaService {

    public PropuestaLectura proponer(LecturaCuaderno lectura, List<BloqueLectura> bloques, UUID destino) {
        List<BloqueLectura> compatibles = bloques.stream()
                .filter(bloque -> compatible(lectura.bloque(), bloque))
                .filter(bloque -> destino == null || bloque.id().equals(destino))
                .toList();
        if (destino != null && compatibles.size() != 1) {
            throw new BusinessRuleException("La estructura leída no es compatible con el bloque de destino");
        }
        UUID bloqueId = null;
        Map<String, UUID> mapeo = new LinkedHashMap<>();
        if (compatibles.size() == 1) {
            BloqueLectura bloque = compatibles.getFirst();
            bloqueId = bloque.id();
            for (CampoPropuesto campo : lectura.bloque().campos()) {
                mapeo.put(campo.clave(), coincidencias(campo, bloque).getFirst().id());
            }
        }
        BloquePropuesto propuesta = new BloquePropuesto(
                lectura.bloque().nombre(), bloqueId, lectura.bloque().campos());
        LecturaCuaderno resultado = new LecturaCuaderno(
                lectura.tipoRegistro(), propuesta, lectura.filas(), lectura.advertencias(), lectura.consumo());
        return new PropuestaLectura(resultado, mapeo);
    }

    private boolean compatible(BloquePropuesto propuesta, BloqueLectura bloque) {
        return normalizar(propuesta.nombre()).equals(normalizar(bloque.nombre()))
                && propuesta.campos().stream().allMatch(campo -> coincidencias(campo, bloque).size() == 1);
    }

    private List<CampoConocido> coincidencias(CampoPropuesto campo, BloqueLectura bloque) {
        return bloque.campos().stream()
                .filter(conocido -> normalizar(conocido.nombre()).equals(normalizar(campo.nombre())))
                .filter(conocido -> conocido.tipo() == campo.tipo())
                .filter(conocido -> conocido.opciones().containsAll(campo.opciones()))
                .toList();
    }

    private String normalizar(String valor) {
        return TextoNormalizado.de(valor);
    }
}
