package com.ensambla.backend.domain.service.zona;

import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ZonaService {

    private final ZonaRepository zonaRepository;

    /** Zona cuyo radio contiene el punto; si hay varias, la de centro más cercano. */
    public Optional<Zona> resolver(BigDecimal latitud, BigDecimal longitud) {
        return zonaRepository.findAll().stream()
                .filter(zona -> zona.contiene(latitud, longitud))
                .min(Comparator.comparingDouble(zona -> zona.distanciaMetros(latitud, longitud)));
    }

    /** Zona cuyo nombre o localidad coincide con lo escrito ("San Justo", "villa crespo"). */
    public Optional<Zona> buscarPorTexto(String texto) {
        return zonaRepository.findAll().stream()
                .filter(zona -> zona.coincideCon(texto))
                .findFirst();
    }

    public Optional<Zona> obtener(UUID zonaId) {
        if (zonaId == null) {
            return Optional.empty();
        }
        return zonaRepository.findById(zonaId);
    }
}
