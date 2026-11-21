package com.ensambla.backend.domain.model.sami.taller.herramienta;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.ArgumentosDeHerramienta;
import com.ensambla.backend.domain.model.sami.taller.CatalogoTaller;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.NombresDePiezas;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezasDelTurno;

import java.util.Optional;

public final class RenombrarPieza extends HerramientaSobreElSistema {

    RenombrarPieza() {
        super(HerramientaTaller.RENOMBRAR_PIEZA);
    }

    @Override
    public Optional<AccionTaller> aplicar(ArgumentosDeHerramienta argumentos, PiezasDelTurno piezas) {
        Optional<String> nombre = argumentos.de("nombre").texto(CatalogoTaller.MAX_LARGO_NOMBRE);
        return piezas.indiceDe(argumentos.de("pieza")).flatMap(indice -> renombrar(indice, nombre, piezas));
    }

    @Override
    protected String frase(ArgumentosDeHerramienta argumentos, NombresDePiezas nombres) {
        return "Renombré " + pieza(argumentos, nombres) + " a " + argumentos.de("nombre").comoTexto();
    }

    /** Renombrar al mismo nombre no es un cambio: el modelo a veces lo pide de paso. */
    private Optional<AccionTaller> renombrar(int indice, Optional<String> nombre, PiezasDelTurno piezas) {
        PiezaTaller pieza = piezas.en(indice);
        return nombre.filter(nuevo -> !nuevo.equals(pieza.nombre())).map(nuevo -> {
            piezas.reemplazar(indice, pieza.conNombre(nuevo));
            return accionSobre(pieza, "nombre", nuevo);
        });
    }
}
