package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.model.marca.KitDeMarca;

import java.util.function.Supplier;

/**
 * Las tres composiciones distintas que se ofrecen para un mismo cartel. Cada una sabe componerse y si intercambia
 * qué color es fondo y cuál acento.
 */
public enum VarianteDeComposicion {

    BLOQUE(DisenoBloque::new, false),
    FRANJA(DisenoFranja::new, true),
    MARCO(DisenoMarco::new, false);

    private final Supplier<DisenoDeVariante> diseno;
    private final boolean intercambiaColores;

    VarianteDeComposicion(Supplier<DisenoDeVariante> diseno, boolean intercambiaColores) {
        this.diseno = diseno;
        this.intercambiaColores = intercambiaColores;
    }

    public ComposicionDeCartel componer(KitDeMarca kit, FormatoDeCartel formato, ContenidoDeCartel contenido) {
        ContextoDeDiseno contexto = new ContextoDeDiseno(formato, paletaDe(kit), kit.getTipografia(), contenido);
        DisenoDeVariante elegido = diseno.get();
        return new ComposicionDeCartel(formato, this, elegido.fondo(contexto), kit.getTipografia(),
                elegido.componer(contexto));
    }

    private PaletaDeCartel paletaDe(KitDeMarca kit) {
        return intercambiaColores
                ? PaletaDeCartel.de(kit.getColorSecundario(), kit.getColorPrincipal())
                : PaletaDeCartel.de(kit.getColorPrincipal(), kit.getColorSecundario());
    }
}
