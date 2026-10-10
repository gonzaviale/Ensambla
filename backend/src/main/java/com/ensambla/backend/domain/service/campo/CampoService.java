package com.ensambla.backend.domain.service.campo;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampoService {

    private final CampoRepository campoRepository;

    public List<Campo> listarDeBloque(UUID bloqueId, UUID negocioId) {
        return campoRepository.findByBloqueIdAndNegocioId(bloqueId, negocioId);
    }
}
