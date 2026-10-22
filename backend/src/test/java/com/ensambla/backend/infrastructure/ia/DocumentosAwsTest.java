package com.ensambla.backend.infrastructure.ia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.document.Document;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentosAwsTest {

    @Test
    @DisplayName("Convierte mapas, listas, textos, números, booleanos y nulos a documentos")
    void aDocumentoShouldConvertEveryKindOfValue() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("texto", "hola");
        mapa.put("entero", 3);
        mapa.put("decimal", 2.5);
        mapa.put("si", true);
        mapa.put("nada", null);
        mapa.put("lista", List.of("a", 1));
        mapa.put("anidado", Map.of("clave", "valor"));
        mapa.put("otro", new StringBuilder("raro"));

        Document documento = DocumentosAws.aDocumento(mapa);

        Map<String, Document> campos = documento.asMap();
        assertThat(campos.get("texto").asString()).isEqualTo("hola");
        assertThat(campos.get("entero").asNumber().intValue()).isEqualTo(3);
        assertThat(campos.get("decimal").asNumber().doubleValue()).isEqualTo(2.5);
        assertThat(campos.get("si").asBoolean()).isTrue();
        assertThat(campos.get("nada").isNull()).isTrue();
        assertThat(campos.get("lista").asList()).hasSize(2);
        assertThat(campos.get("anidado").asMap().get("clave").asString()).isEqualTo("valor");
        assertThat(campos.get("otro").asString()).isEqualTo("raro");
    }

    @Test
    @DisplayName("Convierte un documento de vuelta: los números vuelven como Double")
    void aObjetoShouldConvertBack() {
        Document documento = DocumentosAws.aDocumento(Map.of(
                "tipo", "agregar_pieza",
                "cantidad", 2,
                "activo", false,
                "opciones", List.of("A", "B"),
                "interno", Map.of("x", "y")));

        Object result = DocumentosAws.aObjeto(documento);

        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> mapa = (Map<String, Object>) result;
        assertThat(mapa).containsEntry("tipo", "agregar_pieza")
                .containsEntry("cantidad", 2.0)
                .containsEntry("activo", false)
                .containsEntry("opciones", List.of("A", "B"))
                .containsEntry("interno", Map.of("x", "y"));
    }

    @Test
    @DisplayName("Un documento nulo o ausente vuelve como null")
    void aObjetoShouldReturnNullForNullDocuments() {
        assertThat(DocumentosAws.aObjeto(null)).isNull();
        assertThat(DocumentosAws.aObjeto(Document.fromNull())).isNull();
    }
}
