package com.ensambla.backend.domain.model.sami.taller.herramienta;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.ArgumentosDeHerramienta;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.TurnoDelTaller;

import java.util.List;

/** Una herramienta que hace acciones: cuentan contra el tope de acciones del modelo en el turno. */
public abstract class HerramientaQueActua extends HerramientaDeTaller {

    protected HerramientaQueActua(HerramientaTaller herramienta) {
        super(herramienta);
    }

    @Override
    public void aplicarEn(TurnoDelTaller turno, ArgumentosDeHerramienta argumentos) {
        if (turno.admiteOtraDelModelo()) {
            turno.sumarDelModelo(accionesDe(argumentos, turno));
        }
    }

    protected abstract List<AccionTaller> accionesDe(ArgumentosDeHerramienta argumentos, TurnoDelTaller turno);
}
