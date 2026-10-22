package com.ensambla.backend.domain.service.zona;

import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgregadoZonaService {

    private final AgregadoZonaRepository agregadoZonaRepository;

    public AnalisisZona analizar(Zona zona) {
        return new AnalisisZona(
                zona.getNombre(),
                agregadoZonaRepository.contarComerciosQueComparten(zona.getId()),
                agregadoZonaRepository.findByZonaId(zona.getId()));
    }
}
