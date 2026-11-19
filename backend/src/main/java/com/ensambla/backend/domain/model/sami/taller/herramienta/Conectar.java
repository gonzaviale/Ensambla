package com.ensambla.backend.domain.model.sami.taller.herramienta;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.ArgumentosDeHerramienta;
import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.CatalogoComponentes;
import com.ensambla.backend.domain.model.sami.taller.CatalogoTaller;
import com.ensambla.backend.domain.model.sami.taller.ComponenteCatalogo;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.NombresDePiezas;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezasDelTurno;

import java.util.Optional;

/**
 * Suma en {@code desde} un dato vínculo que apunta a {@code hacia}. Sin etiqueta usa el singular del componente
 * ({@code Cliente}) o, si {@code hacia} es una tabla a medida, su nombre. Como en el taller, si {@code desde} ya tiene
 * un dato común con esa etiqueta (que no sea el primero) ese dato pasa a ser el vínculo, y las piezas sin datos
 * (resumen, nota) no se conectan. Lo que el taller ya vincula solo ({@link CatalogoTaller#seVinculaSolo}) se descarta
 * para no duplicar el vínculo.
 */
public final class Conectar extends HerramientaSobreElSistema {

    private final CatalogoComponentes catalogo;

    Conectar(CatalogoComponentes catalogo) {
        super(HerramientaTaller.CONECTAR);
        this.catalogo = catalogo;
    }

    @Override
    public Optional<AccionTaller> aplicar(ArgumentosDeHerramienta argumentos, PiezasDelTurno piezas) {
        Optional<Integer> desde = piezas.indiceDe(argumentos.de("desde"));
        Optional<Integer> hacia = piezas.indiceDe(argumentos.de("hacia"));
        if (!sonDistintas(desde, hacia)) {
            return Optional.empty();
        }
        return conectar(desde.get(), hacia.get(), argumentos, piezas);
    }

    @Override
    protected String frase(ArgumentosDeHerramienta argumentos, NombresDePiezas nombres) {
        return "Conecté " + nombres.de(argumentos.de("desde").valor())
                + " con " + nombres.de(argumentos.de("hacia").valor());
    }

    private boolean sonDistintas(Optional<Integer> desde, Optional<Integer> hacia) {
        return desde.isPresent() && hacia.isPresent() && !desde.get().equals(hacia.get());
    }

    private Optional<AccionTaller> conectar(int indiceOrigen, int indiceDestino, ArgumentosDeHerramienta argumentos,
                                            PiezasDelTurno piezas) {
        PiezaTaller origen = piezas.en(indiceOrigen);
        PiezaTaller destino = piezas.en(indiceDestino);
        if (!seConectan(origen, destino)) {
            return Optional.empty();
        }
        String etiqueta = argumentos.de("etiqueta").texto(CatalogoTaller.MAX_LARGO_NOMBRE)
                .orElseGet(() -> etiquetaDeVinculo(destino));
        Optional<CampoTaller> existente = origen.listaDeCampos().campoDe(etiqueta);
        if (!hayDondeVincular(origen, existente)) {
            return Optional.empty();
        }
        CampoTaller vinculo = new CampoTaller(
                existente.map(CampoTaller::etiqueta).orElse(etiqueta), CatalogoTaller.TIPO_VINCULO);
        piezas.reemplazar(indiceOrigen, origen.conCampos(origen.listaDeCampos().vinculando(existente, vinculo)));
        return Optional.of(accionDeConexion(origen, destino, vinculo));
    }

    private boolean seConectan(PiezaTaller origen, PiezaTaller destino) {
        return !origen.sinCampos() && !destino.sinCampos()
                && !CatalogoTaller.seVinculaSolo(origen.tipo(), destino.tipo());
    }

    private boolean hayDondeVincular(PiezaTaller origen, Optional<CampoTaller> existente) {
        return existente.map(campo -> sePuedeVolverVinculo(origen, campo))
                .orElseGet(origen::tieneLugarParaOtroCampo);
    }

    private boolean sePuedeVolverVinculo(PiezaTaller origen, CampoTaller campo) {
        return !CatalogoTaller.TIPO_VINCULO.equals(campo.tipo()) && !origen.listaDeCampos().esElPrimero(campo);
    }

    private String etiquetaDeVinculo(PiezaTaller destino) {
        return catalogo.buscar(destino.tipo())
                .filter(componente -> !componente.repetible())
                .map(ComponenteCatalogo::etiquetaDeVinculo)
                .orElse(destino.nombre());
    }

    private AccionTaller accionDeConexion(PiezaTaller origen, PiezaTaller destino, CampoTaller vinculo) {
        return AccionTaller.de(herramienta())
                .con("desde", origen.referencia())
                .con("hacia", destino.referencia())
                .con("etiqueta", vinculo.etiqueta())
                .armar();
    }
}
