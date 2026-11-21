package com.ensambla.backend.domain.model.sami.taller.herramienta;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.ArgumentosDeHerramienta;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.NombresDePiezas;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezasDelTurno;
import com.ensambla.backend.domain.model.sami.taller.TurnoDelTaller;

import java.util.List;
import java.util.Optional;

/** Una herramienta que cambia el sistema: devuelve la acción que el taller tiene que aplicar y sabe contarla en una frase. */
public abstract class HerramientaSobreElSistema extends HerramientaQueActua {

    protected HerramientaSobreElSistema(HerramientaTaller herramienta) {
        super(herramienta);
    }

    /** La acción validada, o nada si lo que pidió el modelo no vale; deja las piezas del turno como quedarían. */
    public abstract Optional<AccionTaller> aplicar(ArgumentosDeHerramienta argumentos, PiezasDelTurno piezas);

    protected abstract String frase(ArgumentosDeHerramienta argumentos, NombresDePiezas nombres);

    /** Las piezas que se suman en el turno se anotan con su nombre, así las acciones siguientes las nombran bien. */
    protected void anotarPiezaNueva(ArgumentosDeHerramienta argumentos, NombresDePiezas nombres) {
    }

    public final String describir(ArgumentosDeHerramienta argumentos, NombresDePiezas nombres) {
        anotarPiezaNueva(argumentos, nombres);
        return frase(argumentos, nombres);
    }

    @Override
    protected List<AccionTaller> accionesDe(ArgumentosDeHerramienta argumentos, TurnoDelTaller turno) {
        return aplicar(argumentos.sinEjemplos(), turno.piezas()).stream().toList();
    }

    protected AccionTaller accion(String argumento, String valor) {
        return AccionTaller.de(herramienta()).con(argumento, valor).armar();
    }

    protected AccionTaller accionSobre(PiezaTaller pieza, String argumento, String valor) {
        return AccionTaller.de(herramienta()).con("pieza", pieza.referencia()).con(argumento, valor).armar();
    }

    protected String pieza(ArgumentosDeHerramienta argumentos, NombresDePiezas nombres) {
        return nombres.de(argumentos.de("pieza").valor());
    }

    protected String cantidadDeRegistros(int cantidad) {
        return cantidad == 1 ? "1 registro" : cantidad + " registros";
    }
}
