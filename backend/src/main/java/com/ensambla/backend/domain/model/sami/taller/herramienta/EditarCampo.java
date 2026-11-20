package com.ensambla.backend.domain.model.sami.taller.herramienta;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.ArgumentosDeHerramienta;
import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.CatalogoComponentes;
import com.ensambla.backend.domain.model.sami.taller.CatalogoTaller;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.ListaDeCampos;
import com.ensambla.backend.domain.model.sami.taller.NombresDePiezas;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezasDelTurno;
import com.ensambla.backend.domain.model.sami.taller.ValorDeArgumento;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Edita un dato que ya existe. Cada parte que llega tiene que valer (si no, se descarta la llamada), salvo el tipo de un
 * vínculo o del primer dato, que no se cambia y se ignora. En la acción viajan sólo las partes que cambian y
 * {@code campo} con la etiqueta exacta que tenía; si no cambia nada, se descarta.
 */
public final class EditarCampo extends HerramientaSobreElSistema {

    private final CatalogoComponentes catalogo;

    EditarCampo(CatalogoComponentes catalogo) {
        super(HerramientaTaller.EDITAR_CAMPO);
        this.catalogo = catalogo;
    }

    @Override
    public Optional<AccionTaller> aplicar(ArgumentosDeHerramienta argumentos, PiezasDelTurno piezas) {
        Optional<String> buscado = argumentos.de("campo").texto(CatalogoTaller.MAX_LARGO_NOMBRE);
        return piezas.indiceDe(argumentos.de("pieza")).flatMap(indice -> editar(indice, buscado, argumentos, piezas));
    }

    @Override
    protected String frase(ArgumentosDeHerramienta argumentos, NombresDePiezas nombres) {
        return "Cambié " + argumentos.de("campo").comoTexto() + " en " + pieza(argumentos, nombres);
    }

    private Optional<AccionTaller> editar(int indice, Optional<String> buscado, ArgumentosDeHerramienta argumentos,
                                          PiezasDelTurno piezas) {
        PiezaTaller pieza = piezas.en(indice);
        ListaDeCampos campos = pieza.listaDeCampos();
        return buscado.flatMap(campos::campoDe)
                .flatMap(campo -> cambiosDe(argumentos, pieza, campos, campo)
                        .map(cambios -> aplicarCambios(indice, pieza, campo, cambios, piezas)));
    }

    private AccionTaller aplicarCambios(int indice, PiezaTaller pieza, CampoTaller campo,
                                        Map<String, Object> cambios, PiezasDelTurno piezas) {
        piezas.reemplazar(indice, pieza.conCampos(pieza.listaDeCampos().editando(campo, cambios)));
        return AccionTaller.de(herramienta())
                .con("pieza", pieza.referencia())
                .con("campo", campo.etiqueta())
                .conTodos(cambios)
                .armar();
    }

    private Optional<Map<String, Object>> cambiosDe(ArgumentosDeHerramienta argumentos, PiezaTaller pieza,
                                                    ListaDeCampos campos, CampoTaller campo) {
        CambiosDeCampo cambios = new CambiosDeCampo();
        etiquetaEditada(argumentos.de("etiqueta"), campos, campo, cambios);
        String tipo = tipoEditado(argumentos.de("tipo"), pieza, campos, campo, cambios);
        opcionesEditadas(argumentos.de("opciones"), tipo, campo, cambios);
        obligatorioEditado(argumentos.de("obligatorio"), cambios);
        posicionEditada(argumentos.de("posicion"), campos, campo, cambios);
        return cambios.validos();
    }

    /** La etiqueta nueva no puede repetir la de otro dato de la pieza; la misma de antes no es un cambio. */
    private void etiquetaEditada(ValorDeArgumento valor, ListaDeCampos campos, CampoTaller campo,
                                 CambiosDeCampo cambios) {
        if (!valor.llego()) {
            return;
        }
        Optional<String> etiqueta = valor.texto(CatalogoTaller.MAX_LARGO_NOMBRE)
                .filter(nueva -> campos.campoDe(nueva).filter(otro -> !otro.equals(campo)).isEmpty());
        cambios.exigir(etiqueta.isPresent());
        etiqueta.filter(nueva -> !nueva.equals(campo.etiqueta())).ifPresent(nueva -> cambios.anotar("etiqueta", nueva));
    }

    /**
     * El tipo con el que queda el dato. Un vínculo, el primer dato (el que nombra cada fila) y los datos que Ensambla
     * necesita para funcionar lo conservan.
     */
    private String tipoEditado(ValorDeArgumento valor, PiezaTaller pieza, ListaDeCampos campos, CampoTaller campo,
                               CambiosDeCampo cambios) {
        if (esDeTipoFijo(pieza, campos, campo) || !valor.llego()) {
            return campo.tipo();
        }
        Optional<String> tipo = valor.clave().filter(CatalogoTaller.TIPOS_DE_CAMPO::contains);
        cambios.exigir(tipo.isPresent());
        tipo.filter(nuevo -> !nuevo.equals(campo.tipo())).ifPresent(nuevo -> cambios.anotar("tipo", nuevo));
        return tipo.orElse(campo.tipo());
    }

    private boolean esDeTipoFijo(PiezaTaller pieza, ListaDeCampos campos, CampoTaller campo) {
        return CatalogoTaller.TIPO_VINCULO.equals(campo.tipo()) || campos.esElPrimero(campo)
                || esFijoDelComponente(pieza, campo);
    }

    private boolean esFijoDelComponente(PiezaTaller pieza, CampoTaller campo) {
        return catalogo.buscar(pieza.tipo()).stream()
                .flatMap(componente -> componente.campos().stream())
                .anyMatch(base -> base.fija() && base.etiqueta().equalsIgnoreCase(campo.etiqueta()));
    }

    /**
     * Las opciones sólo valen si el dato queda como lista: son obligatorias si recién pasa a serlo y, si ya lo era,
     * reemplazan a las que tenía. En otro tipo se ignoran.
     */
    private void opcionesEditadas(ValorDeArgumento valor, String tipo, CampoTaller campo, CambiosDeCampo cambios) {
        if (!pideOpciones(valor, tipo, campo)) {
            return;
        }
        List<String> opciones = valor.textos().stream().limit(CatalogoTaller.MAX_OPCIONES).toList();
        cambios.exigir(opciones.size() >= CatalogoTaller.MIN_OPCIONES);
        cambios.anotar("opciones", opciones);
    }

    private boolean pideOpciones(ValorDeArgumento valor, String tipo, CampoTaller campo) {
        return CatalogoTaller.TIPO_LISTA.equals(tipo) && (valor.llego() || !tipo.equals(campo.tipo()));
    }

    private void obligatorioEditado(ValorDeArgumento valor, CambiosDeCampo cambios) {
        if (valor.llego()) {
            cambios.exigir(valor.esBooleano());
            cambios.anotar("obligatorio", valor.valor());
        }
    }

    /** La posición empieza en 1 y se acota a la cantidad de datos de la pieza. */
    private void posicionEditada(ValorDeArgumento valor, ListaDeCampos campos, CampoTaller campo,
                                 CambiosDeCampo cambios) {
        if (!valor.llego()) {
            return;
        }
        Optional<Long> entera = valor.entero();
        cambios.exigir(entera.isPresent());
        entera.map(posicion -> Math.clamp(posicion, 1, campos.cantidad()))
                .filter(posicion -> posicion != campos.posicionDe(campo))
                .ifPresent(posicion -> cambios.anotar("posicion", posicion));
    }
}
