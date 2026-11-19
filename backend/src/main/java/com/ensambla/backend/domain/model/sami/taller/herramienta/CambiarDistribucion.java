package com.ensambla.backend.domain.model.sami.taller.herramienta;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.ArgumentosDeHerramienta;
import com.ensambla.backend.domain.model.sami.taller.CatalogoTaller;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.NombresDePiezas;
import com.ensambla.backend.domain.model.sami.taller.PiezasDelTurno;

import java.util.Optional;

public final class CambiarDistribucion extends HerramientaSobreElSistema {

    CambiarDistribucion() {
        super(HerramientaTaller.CAMBIAR_DISTRIBUCION);
    }

    @Override
    public Optional<AccionTaller> aplicar(ArgumentosDeHerramienta argumentos, PiezasDelTurno piezas) {
        return argumentos.de("layout").clave()
                .filter(CatalogoTaller.DISTRIBUCIONES::contains)
                .map(layout -> accion("layout", layout));
    }

    @Override
    protected String frase(ArgumentosDeHerramienta argumentos, NombresDePiezas nombres) {
        return "Cambié la distribución";
    }
}
