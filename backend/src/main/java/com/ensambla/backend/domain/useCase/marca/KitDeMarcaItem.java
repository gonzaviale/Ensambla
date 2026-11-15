package com.ensambla.backend.domain.useCase.marca;

import com.ensambla.backend.domain.model.marca.KitDeMarca;
import com.ensambla.backend.domain.model.marca.Tipografia;
import com.ensambla.backend.domain.model.marca.TonoDeMarca;

public record KitDeMarcaItem(
        String colorPrincipal,
        String colorSecundario,
        Tipografia tipografia,
        TonoDeMarca tono,
        boolean tieneLogo) {

    public static KitDeMarcaItem de(KitDeMarca kit) {
        return new KitDeMarcaItem(
                kit.getColorPrincipal().valor(),
                kit.getColorSecundario().valor(),
                kit.getTipografia(),
                kit.getTono(),
                kit.tieneLogo());
    }
}
