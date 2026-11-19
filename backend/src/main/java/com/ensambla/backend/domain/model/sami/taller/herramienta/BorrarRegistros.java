package com.ensambla.backend.domain.model.sami.taller.herramienta;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.ArgumentosDeHerramienta;
import com.ensambla.backend.domain.model.sami.taller.CatalogoTaller;
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
import java.util.stream.Collectors;

/**
 * {@code borrar_registros} quita de una pieza los registros que coinciden con lo que se indica en {@code donde}: un
 * criterio ({@code {Cliente: "Anto Solari"}}, todos sus datos tienen que coincidir) o una lista de criterios (basta con
 * que coincida uno). Nunca borra "todo": un criterio vacío, con un dato que la pieza no tiene o con un valor que no es
 * simple se descarta entero, porque ignorarlo ampliaría lo que se borra. Sami no conoce los ids de las filas, así que
 * las identifica por sus datos; las filas coincidentes las busca el taller.
 */
public final class BorrarRegistros extends HerramientaSobreElSistema {

    BorrarRegistros() {
        super(HerramientaTaller.BORRAR_REGISTROS);
    }

    @Override
    public Optional<AccionTaller> aplicar(ArgumentosDeHerramienta argumentos, PiezasDelTurno piezas) {
        return piezas.indiceDe(argumentos.de("pieza")).flatMap(indice -> borrar(indice, argumentos, piezas));
    }

    /** Un borrado por id, por datos o por las dos cosas a la vez. */
    @Override
    protected String frase(ArgumentosDeHerramienta argumentos, NombresDePiezas nombres) {
        String pieza = pieza(argumentos, nombres);
        List<String> partes = new ArrayList<>();
        if (argumentos.contiene("ids")) {
            partes.add(cantidadDeRegistros(argumentos.de("ids").elementos().size()) + " de " + pieza);
        }
        if (argumentos.contiene("donde")) {
            partes.add("los registros de " + pieza + " donde " + criteriosEnPalabras(argumentos.de("donde")));
        }
        return "Borré " + String.join(" y ", partes);
    }

    private Optional<AccionTaller> borrar(int indice, ArgumentosDeHerramienta argumentos, PiezasDelTurno piezas) {
        PiezaTaller pieza = piezas.en(indice);
        List<Map<String, Object>> criterios = criteriosDeBorrado(argumentos.de("donde"), pieza.listaDeCampos());
        List<String> ids = idsDeRegistros(argumentos.de("ids"), pieza);
        if (criterios.isEmpty() && ids.isEmpty()) {
            return Optional.empty();
        }
        piezas.reemplazar(indice, pieza.conRegistros(pieza.registrosSin(ids)));
        return Optional.of(accionDeBorrado(pieza, ids, criterios));
    }

    private List<Map<String, Object>> criteriosDeBorrado(ValorDeArgumento donde, ListaDeCampos campos) {
        return donde.objetosOUno().stream()
                .map(pedido -> criterioDeBorrado(pedido, campos))
                .flatMap(Optional::stream)
                .limit(CatalogoTaller.MAX_CRITERIOS_BORRADO)
                .toList();
    }

    /** El criterio con las etiquetas de la pieza; si algún dato no vale, el criterio entero no vale. */
    private Optional<Map<String, Object>> criterioDeBorrado(ArgumentosDeHerramienta pedido, ListaDeCampos campos) {
        Map<String, Object> criterio = pedido.comoFila(campos);
        return Optional.of(criterio).filter(valido -> !pedido.estaVacio() && valido.size() == pedido.cantidad());
    }

    /** Los ids que la pieza tiene de verdad, sin repetir: uno que no está se ignora, nunca amplía lo que se borra. */
    private List<String> idsDeRegistros(ValorDeArgumento valor, PiezaTaller pieza) {
        return valor.elementos().stream()
                .flatMap(elemento -> elemento.texto(CatalogoTaller.MAX_LARGO_ID).stream())
                .filter(id -> pieza.registroDe(id).isPresent())
                .distinct()
                .limit(CatalogoTaller.MAX_REGISTROS_POR_PIEZA)
                .toList();
    }

    private AccionTaller accionDeBorrado(PiezaTaller pieza, List<String> ids, List<Map<String, Object>> criterios) {
        return AccionTaller.de(herramienta())
                .con("pieza", pieza.referencia())
                .conSiNoEstaVacia("ids", ids)
                .conSiNoEstaVacia("donde", criterios)
                .armar();
    }

    /** Los criterios de un borrado en palabras: {@code Cliente es Ana y Fecha es 2026-10-18, o …}. */
    private String criteriosEnPalabras(ValorDeArgumento donde) {
        return donde.objetos().stream()
                .map(this::criterioEnPalabras)
                .collect(Collectors.joining(", o "));
    }

    private String criterioEnPalabras(ArgumentosDeHerramienta criterio) {
        return criterio.comoMapa().entrySet().stream()
                .map(dato -> dato.getKey() + " es " + dato.getValue())
                .collect(Collectors.joining(" y "));
    }
}
