package com.ensambla.backend.infrastructure.reporte;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.BaseFont;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.function.Function;

/** Lee de {@code classpath:reporte/} las fuentes y las imágenes de la marca; si alguna falta, falla con un error claro. */
final class RecursosDeMarca {

    private final Function<String, Resource> recursos;

    RecursosDeMarca() {
        this(archivo -> new ClassPathResource("reporte/" + archivo));
    }

    RecursosDeMarca(Function<String, Resource> recursos) {
        this.recursos = recursos;
    }

    BaseFont fuente(String archivo) {
        try {
            return BaseFont.createFont(archivo, BaseFont.CP1252, BaseFont.EMBEDDED, true, bytesDe(archivo), null);
        } catch (DocumentException | IOException e) {
            throw noSePudoLeer(archivo, e);
        }
    }

    Image imagen(String archivo) {
        try {
            return Image.getInstance(bytesDe(archivo));
        } catch (DocumentException | IOException e) {
            throw noSePudoLeer(archivo, e);
        }
    }

    private byte[] bytesDe(String archivo) throws IOException {
        return recursos.apply(archivo).getContentAsByteArray();
    }

    private static IllegalStateException noSePudoLeer(String archivo, Exception causa) {
        return new IllegalStateException("No se pudo leer el recurso de marca " + archivo, causa);
    }
}
