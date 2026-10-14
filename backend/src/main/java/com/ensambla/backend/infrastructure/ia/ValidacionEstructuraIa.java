package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import tools.jackson.databind.JsonNode;

import java.util.List;

/** Verifica tipos JSON antes de que Jackson pueda convertir escalares implícitamente. */
class ValidacionEstructuraIa {

    void validar(JsonNode raiz) {
        exigir(raiz.isObject());
        texto(raiz.path("tipoRegistro"));
        exigir(List.of("VENTAS", "GASTOS", "PEDIDOS", "STOCK", "FIADOS", "CLIENTES", "OTRO")
                .contains(raiz.path("tipoRegistro").asString()));
        JsonNode bloque = raiz.path("bloque");
        exigir(bloque.isObject());
        texto(bloque.path("nombre"));
        nullableTexto(bloque.path("bloqueExistenteId"));
        JsonNode campos = bloque.path("campos");
        exigir(campos.isArray());
        campos.forEach(campo -> {
            exigir(campo.isObject());
            for (String clave : List.of("clave", "nombre", "tipo")) {
                texto(campo.path(clave));
            }
            exigir(List.of("TEXTO", "NUMERO", "FECHA", "OPCION", "SI_NO", "RELACION").contains(campo.path("tipo").asString()));
            textos(campo.path("opciones"));
        });
        textos(raiz.path("advertencias"));
        JsonNode filas = raiz.path("filas");
        exigir(filas.isArray());
        filas.forEach(fila -> {
            exigir(fila.isObject());
            exigir(fila.path("pagina").isIntegralNumber());
            exigir(fila.path("renglon").isIntegralNumber());
            JsonNode datos = fila.path("datos");
            exigir(datos.isObject());
            datos.forEach(dato -> {
                exigir(dato.isObject());
                nullableTexto(dato.path("valor"));
                exigir(dato.path("confianza").isNumber());
            });
        });
    }

    private void textos(JsonNode nodo) {
        exigir(nodo.isArray());
        nodo.forEach(this::texto);
    }

    private void texto(JsonNode nodo) {
        exigir(nodo.isString());
    }

    private void nullableTexto(JsonNode nodo) {
        exigir(nodo.isString() || nodo.isNull());
    }

    private void exigir(boolean valido) {
        if (!valido) {
            throw new LecturaCuadernoException(LectorCuadernoIaAdapter.ERROR_JSON, null, false);
        }
    }
}
