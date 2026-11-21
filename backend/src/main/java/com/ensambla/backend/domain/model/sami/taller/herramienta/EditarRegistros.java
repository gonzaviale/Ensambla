package com.ensambla.backend.domain.model.sami.taller.herramienta;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.ArgumentosDeHerramienta;
import com.ensambla.backend.domain.model.sami.taller.CatalogoTaller;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.NombresDePiezas;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezasDelTurno;
import com.ensambla.backend.domain.model.sami.taller.RegistroTaller;
import com.ensambla.backend.domain.model.sami.taller.ValorDeArgumento;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code editar_registros} corrige registros que la pieza ya tiene (los que Sami cargó o los que el negocio ya tenía guardados), nombrados por
 * su id: {@code registros} es una lista de {@code {id, datos: {Etiqueta: valor}}}. Sólo valen los ids que mandó el
 * taller y los datos de la pieza con valores simples; lo que no cambia nada se cae, y dos pedidos sobre el mismo
 * registro se juntan. Así Sami corrige lo que propuso sin volver a cargarlo.
 */
public final class EditarRegistros extends HerramientaSobreElSistema {

    EditarRegistros() {
        super(HerramientaTaller.EDITAR_REGISTROS);
    }

    @Override
    public Optional<AccionTaller> aplicar(ArgumentosDeHerramienta argumentos, PiezasDelTurno piezas) {
        return piezas.indiceDe(argumentos.de("pieza")).flatMap(indice -> editar(indice, argumentos, piezas));
    }

    @Override
    protected String frase(ArgumentosDeHerramienta argumentos, NombresDePiezas nombres) {
        int corregidos = argumentos.de("registros").elementos().size();
        return "Corregí " + cantidadDeRegistros(corregidos) + " en " + pieza(argumentos, nombres);
    }

    private Optional<AccionTaller> editar(int indice, ArgumentosDeHerramienta argumentos, PiezasDelTurno piezas) {
        PiezaTaller pieza = piezas.en(indice);
        Map<String, Map<String, Object>> cambios = cambiosPorRegistro(argumentos.de("registros"), pieza);
        if (cambios.isEmpty()) {
            return Optional.empty();
        }
        piezas.reemplazar(indice, pieza.conRegistros(registrosCorregidos(pieza, cambios)));
        return Optional.of(accionDeEdicion(pieza, cambios));
    }

    private Map<String, Map<String, Object>> cambiosPorRegistro(ValorDeArgumento pedidos, PiezaTaller pieza) {
        Map<String, Map<String, Object>> cambios = new LinkedHashMap<>();
        for (ArgumentosDeHerramienta pedido : pedidos.objetos()) {
            pedido.de("id").texto(CatalogoTaller.MAX_LARGO_ID)
                    .flatMap(pieza::registroDe)
                    .ifPresent(registro -> juntar(cambios, registro, cambiosDeRegistro(pedido.de("datos"), registro, pieza)));
        }
        return cambios;
    }

    private void juntar(Map<String, Map<String, Object>> cambios, RegistroTaller registro,
                        Map<String, Object> nuevos) {
        nuevos.forEach((etiqueta, valor) -> cambios
                .computeIfAbsent(registro.id(), id -> new LinkedHashMap<>())
                .put(etiqueta, valor));
    }

    /** Los datos pedidos con las etiquetas de la pieza, salvo los que ya tienen ese valor. */
    private Map<String, Object> cambiosDeRegistro(ValorDeArgumento datos, RegistroTaller registro, PiezaTaller pieza) {
        Map<String, Object> cambios = new LinkedHashMap<>();
        datos.unObjeto().forEach(pedido -> pedido.comoFila(pieza.listaDeCampos())
                .forEach((etiqueta, valor) -> anotarSiCambia(cambios, registro, etiqueta, valor)));
        return cambios;
    }

    private void anotarSiCambia(Map<String, Object> cambios, RegistroTaller registro, String etiqueta, Object valor) {
        if (!new ValorDeArgumento(registro.datos().get(etiqueta)).equivaleA(valor)) {
            cambios.put(etiqueta, valor);
        }
    }

    private List<RegistroTaller> registrosCorregidos(PiezaTaller pieza, Map<String, Map<String, Object>> cambios) {
        return pieza.registros().stream()
                .map(registro -> registro.conCambios(cambios.getOrDefault(registro.id(), Map.of())))
                .toList();
    }

    private AccionTaller accionDeEdicion(PiezaTaller pieza, Map<String, Map<String, Object>> cambios) {
        List<Map<String, Object>> registros = cambios.entrySet().stream()
                .<Map<String, Object>>map(cambio -> registroCorregido(cambio.getKey(), cambio.getValue()))
                .toList();
        return AccionTaller.de(herramienta())
                .con("pieza", pieza.referencia())
                .con("registros", registros)
                .armar();
    }

    private Map<String, Object> registroCorregido(String id, Map<String, Object> datos) {
        Map<String, Object> registro = new LinkedHashMap<>();
        registro.put("id", id);
        registro.put("datos", datos);
        return registro;
    }
}
