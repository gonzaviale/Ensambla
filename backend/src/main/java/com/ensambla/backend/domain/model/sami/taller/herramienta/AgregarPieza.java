package com.ensambla.backend.domain.model.sami.taller.herramienta;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.ArgumentosDeHerramienta;
import com.ensambla.backend.domain.model.sami.taller.CampoNuevo;
import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.CatalogoComponentes;
import com.ensambla.backend.domain.model.sami.taller.CatalogoTaller;
import com.ensambla.backend.domain.model.sami.taller.ComponenteCatalogo;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.ListaDeCampos;
import com.ensambla.backend.domain.model.sami.taller.NombresDePiezas;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezasDelTurno;
import com.ensambla.backend.domain.model.sami.taller.ValorDeArgumento;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AgregarPieza extends HerramientaSobreElSistema {

    private final CatalogoComponentes catalogo;

    AgregarPieza(CatalogoComponentes catalogo) {
        super(HerramientaTaller.AGREGAR_PIEZA);
        this.catalogo = catalogo;
    }

    @Override
    public Optional<AccionTaller> aplicar(ArgumentosDeHerramienta argumentos, PiezasDelTurno piezas) {
        Optional<String> nombre = argumentos.de("nombre").texto(CatalogoTaller.MAX_LARGO_NOMBRE);
        return argumentos.de("tipo").clave()
                .flatMap(catalogo::buscar)
                .filter(componente -> admiteElTipo(componente, nombre, piezas))
                .filter(componente -> !piezas.tieneNombre(nombre.orElse(componente.nombre())))
                .map(componente -> sumar(componente, nombre, argumentos, piezas));
    }

    @Override
    protected void anotarPiezaNueva(ArgumentosDeHerramienta argumentos, NombresDePiezas nombres) {
        String tipo = argumentos.de("tipo").comoTexto();
        nombres.anotar(referenciaNueva(argumentos), argumentos.contiene("nombre")
                ? argumentos.de("nombre").comoTexto()
                : catalogo.buscar(tipo).map(ComponenteCatalogo::nombre).orElse(tipo));
    }

    @Override
    protected String frase(ArgumentosDeHerramienta argumentos, NombresDePiezas nombres) {
        return "Sumé " + nombres.de(referenciaNueva(argumentos));
    }

    /** Un tipo que se repite necesita nombre; el resto, que todavía no esté. */
    private boolean admiteElTipo(ComponenteCatalogo componente, Optional<String> nombre, PiezasDelTurno piezas) {
        return componente.repetible() ? nombre.isPresent() : !piezas.tieneDelTipo(componente.tipo());
    }

    /** El resumen y la nota no llevan datos: a esos componentes no se les suman datos extra ni ejemplos. */
    private AccionTaller sumar(ComponenteCatalogo componente, Optional<String> nombre,
                               ArgumentosDeHerramienta argumentos, PiezasDelTurno piezas) {
        List<CampoTaller> campos = new ArrayList<>(componente.camposTaller());
        List<Map<String, Object>> extras = campos.isEmpty() ? List.of() : camposExtra(argumentos.de("campos"), campos);
        List<Map<String, Object>> ejemplos = argumentos.de("ejemplos")
                .filas(new ListaDeCampos(campos), CatalogoTaller.MAX_FILAS_EJEMPLO);
        Optional<String> seccion = argumentos.de("seccion").texto(CatalogoTaller.MAX_LARGO_NOMBRE);
        piezas.sumar(new PiezaTaller(
                null, componente.tipo(), nombre.orElse(componente.nombre()), seccion.orElse(null), campos));
        return AccionTaller.de(herramienta())
                .con("tipo", componente.tipo())
                .conSiHay("nombre", nombre)
                .conSiNoEstaVacia("campos", extras)
                .conSiNoEstaVacia("ejemplos", ejemplos)
                .conSiHay("seccion", seccion)
                .armar();
    }

    /** Los datos extra válidos, que además quedan sumados a {@code campos}. */
    private List<Map<String, Object>> camposExtra(ValorDeArgumento pedidos, List<CampoTaller> campos) {
        List<Map<String, Object>> extras = new ArrayList<>();
        for (ArgumentosDeHerramienta pedido : pedidos.objetos()) {
            Optional<CampoNuevo> campo = hayLugarParaOtro(extras, campos)
                    ? pedido.comoCampoNuevo(new ListaDeCampos(campos))
                    : Optional.empty();
            campo.ifPresent(nuevo -> {
                campos.add(nuevo.comoCampo());
                extras.add(nuevo.comoArgumentos());
            });
        }
        return extras;
    }

    private boolean hayLugarParaOtro(List<Map<String, Object>> extras, List<CampoTaller> campos) {
        return extras.size() < CatalogoTaller.MAX_CAMPOS_EXTRA && campos.size() < CatalogoTaller.MAX_CAMPOS_POR_PIEZA;
    }

    /** Cómo nombran las acciones a una pieza recién sumada: por su tipo, o por su nombre si es una tabla a medida. */
    private String referenciaNueva(ArgumentosDeHerramienta argumentos) {
        String tipo = argumentos.de("tipo").comoTexto();
        return CatalogoComponentes.TIPO_A_MEDIDA.equals(tipo) ? argumentos.de("nombre").comoTexto() : tipo;
    }
}
