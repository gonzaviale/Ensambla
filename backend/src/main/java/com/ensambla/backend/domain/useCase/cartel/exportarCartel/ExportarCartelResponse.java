package com.ensambla.backend.domain.useCase.cartel.exportarCartel;

import com.ensambla.backend.domain.model.cartel.ComposicionDeCartel;
import com.ensambla.backend.domain.model.cartel.SalidaDeCartel;
import com.ensambla.backend.domain.model.cartel.TipoDeCartel;

import java.util.Locale;

public record ExportarCartelResponse(byte[] contenido, String tipoMime, String nombreDeArchivo) {

    public static ExportarCartelResponse de(byte[] contenido, TipoDeCartel tipo, ComposicionDeCartel composicion) {
        SalidaDeCartel salida = composicion.formato().salida();
        String nombre = "cartel-" + tipo.name().toLowerCase(Locale.ROOT).replace('_', '-') + "-"
                + composicion.formato().name().toLowerCase(Locale.ROOT) + "." + salida.extension();
        return new ExportarCartelResponse(contenido, salida.tipoMime(), nombre);
    }
}
