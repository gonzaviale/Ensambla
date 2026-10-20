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

    public Optional<Zona> obtener(UUID zonaId) {
        if (zonaId == null) {
            return Optional.empty();
        }
        return zonaRepository.findById(zonaId);
    }
}
