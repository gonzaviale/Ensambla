package com.ensambla.backend.domain.model.promocion;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** Los canales donde se puede compartir una promoción; sumar uno es registrar una clase nueva. */
@RequiredArgsConstructor
public class CatalogoDeCanales {

    private final List<CanalPromocion> canales;

    public List<CanalPromocion> todos() {
        return canales;
    }

    /** @throws BusinessRuleException si no hay un canal con esa clave */
    public CanalPromocion porClave(String clave) {
        return canales.stream()
                .filter(canal -> canal.clave().equals(clave))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("No conocemos ese canal para compartir"));
    }
}
