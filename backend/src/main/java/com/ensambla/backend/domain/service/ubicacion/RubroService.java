package com.ensambla.backend.domain.service.ubicacion;

import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RubroService {

    public List<RubroNegocio> listar() {
        return List.of(RubroNegocio.values());
    }
}
