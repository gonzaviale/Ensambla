package com.ensambla.backend.domain.model.sami.taller.herramienta;

import com.ensambla.backend.domain.model.sami.taller.ArgumentosDeHerramienta;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.TurnoDelTaller;

/** Una herramienta que el modelo puede invocar en el taller: sabe validar lo que pidió y sumarlo al turno. */
public abstract class HerramientaDeTaller {

    private final HerramientaTaller herramienta;

    protected HerramientaDeTaller(HerramientaTaller herramienta) {
        this.herramienta = herramienta;
    }

    public HerramientaTaller herramienta() {
        return herramienta;
    }

    public abstract void aplicarEn(TurnoDelTaller turno, ArgumentosDeHerramienta argumentos);
}
