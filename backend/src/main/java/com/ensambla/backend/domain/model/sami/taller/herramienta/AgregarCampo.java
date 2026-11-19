package com.ensambla.backend.domain.model.sami.taller.herramienta;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.ArgumentosDeHerramienta;
import com.ensambla.backend.domain.model.sami.taller.CampoNuevo;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.NombresDePiezas;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezasDelTurno;

import java.util.Optional;

public final class AgregarCampo extends HerramientaSobreElSistema {

    AgregarCampo() {
        super(HerramientaTaller.AGREGAR_CAMPO);
    }

    @Override
    public Optional<AccionTaller> aplicar(ArgumentosDeHerramienta argumentos, PiezasDelTurno piezas) {
        return piezas.indiceDe(argumentos.de("pieza")).flatMap(indice -> agregar(indice, argumentos, piezas));
    }

    @Override
    protected String frase(ArgumentosDeHerramienta argumentos, NombresDePiezas nombres) {
        return "Sumé " + argumentos.de("etiqueta").comoTexto() + " a " + pieza(argumentos, nombres);
    }

    private Optional<AccionTaller> agregar(int indice, ArgumentosDeHerramienta argumentos, PiezasDelTurno piezas) {
        PiezaTaller pieza = piezas.en(indice);
        return argumentos.comoCampoNuevo(pieza.listaDeCampos())
                .filter(nuevo -> pieza.tieneLugarParaOtroCampo())
                .map(nuevo -> sumar(indice, pieza, nuevo, piezas));
    }

    private AccionTaller sumar(int indice, PiezaTaller pieza, CampoNuevo nuevo, PiezasDelTurno piezas) {
        piezas.reemplazar(indice, pieza.conCampo(nuevo.comoCampo()));
        return AccionTaller.de(herramienta())
                .con("pieza", pieza.referencia())
                .conTodos(nuevo.comoArgumentos())
                .armar();
    }
}
