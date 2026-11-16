package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.model.marca.KitDeMarca;

import java.util.Arrays;
import java.util.List;

/** Las opciones que ve la persona: la misma marca y los mismos datos en cada una de las tres composiciones. */
public final class OpcionesDeCartel {

    private OpcionesDeCartel() {
    }

    public static List<ComposicionDeCartel> armar(KitDeMarca kit, TipoDeCartel tipo, FormatoDeCartel formato,
                                                  ContenidoDeCartel contenido) {
        tipo.exigirLineasAdmitidas(contenido.lineas().size());
        return Arrays.stream(VarianteDeComposicion.values())
                .map(variante -> variante.componer(kit, formato, contenido))
                .toList();
    }

    /** Una sola de las composiciones, la que la persona eligió. */
    public static ComposicionDeCartel componer(VarianteDeComposicion variante, KitDeMarca kit, TipoDeCartel tipo,
                                               FormatoDeCartel formato, ContenidoDeCartel contenido) {
        tipo.exigirLineasAdmitidas(contenido.lineas().size());
        return variante.componer(kit, formato, contenido);
    }
}
