package com.ensambla.backend.infrastructure.ia;

import software.amazon.awssdk.core.document.Document;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Conversión entre los objetos comunes (mapas, listas, textos…) y {@link Document}, el JSON del SDK de AWS. */
final class DocumentosAws {

    private DocumentosAws() {
    }

    static Document aDocumento(Object valor) {
        if (valor instanceof Map<?, ?> mapa) {
            Map<String, Document> campos = new LinkedHashMap<>();
            mapa.forEach((clave, contenido) -> campos.put(String.valueOf(clave), aDocumento(contenido)));
            return Document.fromMap(campos);
        }
        if (valor instanceof List<?> lista) {
            return Document.fromList(lista.stream().map(DocumentosAws::aDocumento).toList());
        }
        if (valor instanceof Boolean booleano) {
            return Document.fromBoolean(booleano);
        }
        if (valor instanceof Number numero) {
            return Document.fromNumber(numero.doubleValue());
        }
        return valor == null ? Document.fromNull() : Document.fromString(valor.toString());
    }

    /** Los números llegan como {@code Double}; el resto queda como texto, booleano, lista o mapa. */
    static Object aObjeto(Document documento) {
        if (documento == null || documento.isNull()) {
            return null;
        }
        if (documento.isMap()) {
            Map<String, Object> mapa = new LinkedHashMap<>();
            documento.asMap().forEach((clave, contenido) -> mapa.put(clave, aObjeto(contenido)));
            return mapa;
        }
        if (documento.isList()) {
            List<Object> lista = new ArrayList<>();
            documento.asList().forEach(contenido -> lista.add(aObjeto(contenido)));
            return lista;
        }
        if (documento.isBoolean()) {
            return documento.asBoolean();
        }
        if (documento.isNumber()) {
            return documento.asNumber().doubleValue();
        }
        return documento.asString();
    }
}
