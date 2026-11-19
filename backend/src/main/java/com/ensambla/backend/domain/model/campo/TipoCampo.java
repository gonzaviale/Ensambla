package com.ensambla.backend.domain.model.campo;

import java.util.function.Function;

public enum TipoCampo {
    TEXTO(CampoTexto::new),
    NUMERO(CampoNumero::new),
    FECHA(CampoFecha::new),
    OPCION(CampoOpcion::new),
    SI_NO(CampoSiNo::new),
    RELACION(CampoRelacion::new),
    FORMULA(CampoFormula::new),
    TEXTO_LARGO(CampoTextoLargo::new),
    MONEDA(CampoMoneda::new),
    EMAIL(CampoEmail::new),
    TELEFONO(CampoTelefono::new),
    HORA(CampoHora::new);

    private final Function<CampoBuilder, Campo> fabrica;

    TipoCampo(Function<CampoBuilder, Campo> fabrica) {
        this.fabrica = fabrica;
    }

    Campo crear(CampoBuilder datos) {
        return fabrica.apply(datos);
    }
}
