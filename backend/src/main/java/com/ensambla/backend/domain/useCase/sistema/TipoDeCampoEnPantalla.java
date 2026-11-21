package com.ensambla.backend.domain.useCase.sistema;

import com.ensambla.backend.domain.model.campo.TipoCampo;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/** Cómo llama la pantalla a cada tipo de campo y qué tipo del dominio crea al guardar. */
enum TipoDeCampoEnPantalla {

    TEXT("text", TipoCampo.TEXTO),
    TEXTAREA("textarea", TipoCampo.TEXTO_LARGO),
    /** La pantalla no tiene columnas calculadas: una fórmula se muestra como número. */
    NUMBER("number", TipoCampo.NUMERO, TipoCampo.FORMULA),
    CURRENCY("currency", TipoCampo.MONEDA),
    DATE("date", TipoCampo.FECHA),
    SELECT("select", TipoCampo.OPCION),
    EMAIL("email", TipoCampo.EMAIL),
    TEL("tel", TipoCampo.TELEFONO),
    CHECKBOX("checkbox", TipoCampo.SI_NO),
    RELATION("relation", TipoCampo.RELACION),
    TIME("time", TipoCampo.HORA);

    private final String clave;

    private final TipoCampo tipoAlGuardar;

    private final Set<TipoCampo> tiposQueMuestra;

    TipoDeCampoEnPantalla(String clave, TipoCampo tipoAlGuardar, TipoCampo... otrosTiposQueMuestra) {
        this.clave = clave;
        this.tipoAlGuardar = tipoAlGuardar;
        this.tiposQueMuestra = EnumSet.of(tipoAlGuardar, otrosTiposQueMuestra);
    }

    /** La clave con la que la pantalla muestra un campo de ese tipo. */
    static String claveDe(TipoCampo tipo) {
        return Arrays.stream(values())
                .filter(tipoEnPantalla -> tipoEnPantalla.tiposQueMuestra.contains(tipo))
                .findFirst()
                .orElseThrow()
                .clave;
    }

    /** El tipo del dominio para la clave que mandó la pantalla; una clave que no se conoce es un texto. */
    static TipoCampo tipoAlGuardar(String clave) {
        return Arrays.stream(values())
                .filter(tipoEnPantalla -> tipoEnPantalla.clave.equals(clave))
                .findFirst()
                .map(tipoEnPantalla -> tipoEnPantalla.tipoAlGuardar)
                .orElse(TipoCampo.TEXTO);
    }
}
