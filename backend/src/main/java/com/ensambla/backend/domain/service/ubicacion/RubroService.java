package com.ensambla.backend.domain.service.ubicacion;

import com.ensambla.backend.domain.model.negocio.RubroNegocio;

import java.util.List;

public class RubroService {

    public List<RubroNegocio> listar() {
        return List.of(RubroNegocio.values());
    }
}
