package com.ensambla.backend.domain.useCase.marca.subirLogoDeMarca;

import com.ensambla.backend.domain.model.marca.ColoresSugeridos;
import com.ensambla.backend.domain.model.marca.KitDeMarca;
import com.ensambla.backend.domain.useCase.marca.KitDeMarcaItem;

/** El kit con el logo ya guardado y los colores que se sugieren a partir de él (la persona decide si los usa). */
public record SubirLogoDeMarcaResponse(KitDeMarcaItem kit, String colorPrincipalSugerido,
                                       String colorSecundarioSugerido) {

    public static SubirLogoDeMarcaResponse de(KitDeMarca kit, ColoresSugeridos sugeridos) {
        return new SubirLogoDeMarcaResponse(
                KitDeMarcaItem.de(kit),
                sugeridos.principal().valor(),
                sugeridos.secundario().valor());
    }
}
