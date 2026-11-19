package com.ensambla.backend.domain.model.sami.taller.herramienta;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.ArgumentosDeHerramienta;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.NombresDePiezas;
import com.ensambla.backend.domain.model.sami.taller.PiezasDelTurno;

import java.util.Optional;

public final class QuitarPieza extends HerramientaSobreElSistema {

    QuitarPieza() {
        super(HerramientaTaller.QUITAR_PIEZA);
    }

    @Override
    public Optional<AccionTaller> aplicar(ArgumentosDeHerramienta argumentos, PiezasDelTurno piezas) {
        return piezas.indiceDe(argumentos.de("pieza")).map(indice -> quitar(indice, piezas));
    }

    @Override
    protected String frase(ArgumentosDeHerramienta argumentos, NombresDePiezas nombres) {
        return "Quité " + pieza(argumentos, nombres);
    }

    private AccionTaller quitar(int indice, PiezasDelTurno piezas) {
        return accion("pieza", piezas.quitar(indice).referencia());
    }
}
