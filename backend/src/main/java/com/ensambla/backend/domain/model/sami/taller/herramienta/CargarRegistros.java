package com.ensambla.backend.domain.model.sami.taller.herramienta;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.ArgumentosDeHerramienta;
import com.ensambla.backend.domain.model.sami.taller.CatalogoTaller;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.NombresDePiezas;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezasDelTurno;
import com.ensambla.backend.domain.model.sami.taller.TurnoDelTaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/** {@code cargar_registros}: registros reales (leídos de una foto, por ejemplo) que se suman a los que haya. */
public final class CargarRegistros extends HerramientaSobreElSistema {

    private static final Logger log = LoggerFactory.getLogger(CargarRegistros.class);

    CargarRegistros() {
        super(HerramientaTaller.CARGAR_REGISTROS);
    }

    @Override
    public Optional<AccionTaller> aplicar(ArgumentosDeHerramienta argumentos, PiezasDelTurno piezas) {
        return piezas.indiceDe(argumentos.de("pieza")).flatMap(indice -> cargar(piezas.en(indice), argumentos));
    }

    @Override
    protected String frase(ArgumentosDeHerramienta argumentos, NombresDePiezas nombres) {
        int cargados = argumentos.de("filas").elementos().size();
        return "Cargué " + cantidadDeRegistros(cargados) + " en " + pieza(argumentos, nombres);
    }

    @Override
    protected List<AccionTaller> accionesDe(ArgumentosDeHerramienta argumentos, TurnoDelTaller turno) {
        List<AccionTaller> hechas = super.accionesDe(argumentos, turno);
        contarRegistrosDescartados(argumentos, hechas, turno);
        return hechas;
    }

    private Optional<AccionTaller> cargar(PiezaTaller pieza, ArgumentosDeHerramienta argumentos) {
        return Optional.of(argumentos.de("filas").filas(pieza.listaDeCampos(), CatalogoTaller.MAX_REGISTROS))
                .filter(filas -> !filas.isEmpty())
                .map(filas -> AccionTaller.de(herramienta())
                        .con("pieza", pieza.referencia())
                        .con("filas", filas)
                        .armar());
    }

    /** Las filas pedidas que no llegaron a cargarse se cuentan (y se loguean) para que Sami no diga que cargó todo. */
    private void contarRegistrosDescartados(ArgumentosDeHerramienta argumentos, List<AccionTaller> hechas,
                                            TurnoDelTaller turno) {
        int cargadas = hechas.stream().mapToInt(this::filasDeLaAccion).sum();
        int descartadas = argumentos.de("filas").elementos().size() - cargadas;
        if (descartadas > 0) {
            log.warn("cargar_registros: se descartaron {} filas de {} pedidas (sin el dato principal, con datos que"
                    + " no son de la pieza o pasadas del tope de {})", descartadas, descartadas + cargadas,
                    CatalogoTaller.MAX_REGISTROS);
            turno.sumarRegistrosDescartados(descartadas);
        }
    }

    private int filasDeLaAccion(AccionTaller accion) {
        return new ArgumentosDeHerramienta(accion.argumentos()).de("filas").elementos().size();
    }
}
