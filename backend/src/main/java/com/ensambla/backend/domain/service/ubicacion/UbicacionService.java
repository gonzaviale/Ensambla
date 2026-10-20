package com.ensambla.backend.domain.service.ubicacion;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.ubicacion.DireccionEncontrada;
import com.ensambla.backend.domain.port.GeocodificadorPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UbicacionService {

    private final GeocodificadorPort geocodificadorPort;

    public List<DireccionEncontrada> buscar(String consulta, Instant instante) {
        String normalizada = consulta.strip().replaceAll("\\s+", " ");
        if (normalizada.length() < 5 || normalizada.length() > 200) {
            throw new BusinessRuleException("Escribí una dirección de entre 5 y 200 caracteres, con localidad.");
        }
        return geocodificadorPort.buscar(normalizada, instante);
    }
}
