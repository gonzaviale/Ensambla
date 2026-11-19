package com.ensambla.backend.domain.model.sami.taller.herramienta;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.ArgumentosDeHerramienta;
import com.ensambla.backend.domain.model.sami.taller.CatalogoTaller;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.NombresDePiezas;
import com.ensambla.backend.domain.model.sami.taller.PiezasDelTurno;

import java.util.Optional;

public final class RenombrarSistema extends HerramientaSobreElSistema {

    RenombrarSistema() {
        super(HerramientaTaller.RENOMBRAR_SISTEMA);
    }

    @Override
    public Optional<AccionTaller> aplicar(ArgumentosDeHerramienta argumentos, PiezasDelTurno piezas) {
        return argumentos.de("nombre").texto(CatalogoTaller.MAX_LARGO_NOMBRE)
                .map(nombre -> accion("nombre", nombre));
    }

    @Override
    protected String frase(ArgumentosDeHerramienta argumentos, NombresDePiezas nombres) {
        return "Llamé al sistema " + argumentos.de("nombre").comoTexto();
    }
}
