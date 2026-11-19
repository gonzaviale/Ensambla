package com.ensambla.backend.domain.model.sami.taller.herramienta;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.ArgumentosDeHerramienta;
import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.CatalogoComponentes;
import com.ensambla.backend.domain.model.sami.taller.CatalogoTaller;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.NombresDePiezas;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezasDelTurno;

import java.util.Optional;

public final class QuitarCampo extends HerramientaSobreElSistema {

    private final CatalogoComponentes catalogo;

    QuitarCampo(CatalogoComponentes catalogo) {
        super(HerramientaTaller.QUITAR_CAMPO);
        this.catalogo = catalogo;
    }

    @Override
    public Optional<AccionTaller> aplicar(ArgumentosDeHerramienta argumentos, PiezasDelTurno piezas) {
        Optional<String> buscado = argumentos.de("campo").texto(CatalogoTaller.MAX_LARGO_NOMBRE);
        return piezas.indiceDe(argumentos.de("pieza")).flatMap(indice -> quitar(indice, buscado, piezas));
    }

    @Override
    protected String frase(ArgumentosDeHerramienta argumentos, NombresDePiezas nombres) {
        return "Quité " + argumentos.de("campo").comoTexto() + " de " + pieza(argumentos, nombres);
    }

    private Optional<AccionTaller> quitar(int indice, Optional<String> buscado, PiezasDelTurno piezas) {
        PiezaTaller pieza = piezas.en(indice);
        return buscado.flatMap(etiqueta -> pieza.listaDeCampos().campoDe(etiqueta))
                .filter(campo -> sePuedeQuitar(pieza, campo))
                .map(campo -> {
                    piezas.reemplazar(indice, pieza.conCampos(pieza.listaDeCampos().sin(campo)));
                    return accionSobre(pieza, "campo", campo.etiqueta());
                });
    }

    /** Los vínculos y los datos obligatorios del componente no se quitan (el taller tampoco lo deja). */
    private boolean sePuedeQuitar(PiezaTaller pieza, CampoTaller campo) {
        return !CatalogoTaller.TIPO_VINCULO.equals(campo.tipo()) && !esObligatorioDelComponente(pieza, campo);
    }

    private boolean esObligatorioDelComponente(PiezaTaller pieza, CampoTaller campo) {
        return catalogo.buscar(pieza.tipo()).stream()
                .flatMap(componente -> componente.campos().stream())
                .anyMatch(base -> base.obligatorio() && base.etiqueta().equalsIgnoreCase(campo.etiqueta()));
    }
}
