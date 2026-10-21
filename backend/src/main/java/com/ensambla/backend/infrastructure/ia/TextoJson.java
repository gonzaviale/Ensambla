package com.ensambla.backend.infrastructure.ia;

/**
 * Lo común a los clientes que piden JSON: el esquema va dentro de las instrucciones (no como salida estructurada del
 * proveedor, que no admite todo lo que usan los esquemas) y de la respuesta se toma sólo el objeto JSON. La validación
 * estricta la hace quien lo recibe.
 */
final class TextoJson {

    private TextoJson() {
    }

    static String instruccionesConEsquema(String instrucciones, String esquemaJson) {
        return instrucciones
                + "\n\nRespondé únicamente con un objeto JSON válido que cumpla este JSON Schema, sin texto"
                + " antes ni después y sin bloques de código:\n" + esquemaJson;
    }

    /** Descarta cualquier cerca de código o comentario alrededor del objeto JSON; sin objeto, devuelve el texto. */
    static String extraer(String texto) {
        int inicio = texto.indexOf('{');
        int fin = texto.lastIndexOf('}');
        return inicio >= 0 && fin > inicio ? texto.substring(inicio, fin + 1) : texto;
    }
}
