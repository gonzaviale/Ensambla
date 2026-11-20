package com.ensambla.backend.domain.model.sami.taller.herramienta;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.ArgumentosDeHerramienta;
import com.ensambla.backend.domain.model.sami.taller.CatalogoComponentes;
import com.ensambla.backend.domain.model.sami.taller.ComponenteCatalogo;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.NombresDePiezas;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezasDelTurno;

import java.util.Optional;

public final class QuitarPieza extends HerramientaSobreElSistema {

    private final CatalogoComponentes catalogo;

    QuitarPieza(CatalogoComponentes catalogo) {
        super(HerramientaTaller.QUITAR_PIEZA);
        this.catalogo = catalogo;
    }

    @Override
    public Optional<AccionTaller> aplicar(ArgumentosDeHerramienta argumentos, PiezasDelTurno piezas) {
        return piezas.indiceDe(argumentos.de("pieza"))
                .filter(indice -> sePuedeQuitar(piezas.en(indice), piezas))
                .map(indice -> quitar(indice, piezas));
    }

    /** La última pieza de un tipo que todo negocio necesita no se quita (el taller tampoco lo deja). */
    private boolean sePuedeQuitar(PiezaTaller pieza, PiezasDelTurno piezas) {
        boolean esObligatoria = catalogo.buscar(pieza.tipo()).filter(ComponenteCatalogo::obligatorioEnElNegocio)
                .isPresent();
        return !esObligatoria || !piezas.esLaUnicaDelTipo(pieza);
    }

    @Override
    protected String frase(ArgumentosDeHerramienta argumentos, NombresDePiezas nombres) {
        return "Quité " + pieza(argumentos, nombres);
    }

    private AccionTaller quitar(int indice, PiezasDelTurno piezas) {
        return accion("pieza", piezas.quitar(indice).referencia());
    }
}
