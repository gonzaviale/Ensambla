package com.ensambla.backend.domain.model.sami.taller.herramienta;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.ArgumentosDeHerramienta;
import com.ensambla.backend.domain.model.sami.taller.CatalogoTaller;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.NombresDePiezas;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezasDelTurno;

import java.util.Optional;

public final class SepararEnSeccion extends HerramientaSobreElSistema {

    SepararEnSeccion() {
        super(HerramientaTaller.SEPARAR_EN_SECCION);
    }

    @Override
    public Optional<AccionTaller> aplicar(ArgumentosDeHerramienta argumentos, PiezasDelTurno piezas) {
        Optional<String> seccion = argumentos.de("seccion").texto(CatalogoTaller.MAX_LARGO_NOMBRE);
        return piezas.indiceDe(argumentos.de("pieza"))
                .flatMap(indice -> seccion.map(nueva -> separar(indice, nueva, piezas)));
    }

    @Override
    protected String frase(ArgumentosDeHerramienta argumentos, NombresDePiezas nombres) {
        return "Pasé " + pieza(argumentos, nombres) + " a la sección " + argumentos.de("seccion").comoTexto();
    }

    private AccionTaller separar(int indice, String nueva, PiezasDelTurno piezas) {
        PiezaTaller pieza = piezas.en(indice);
        piezas.reemplazar(indice, pieza.conSeccion(nueva));
        return accionSobre(pieza, "seccion", nueva);
    }
}
