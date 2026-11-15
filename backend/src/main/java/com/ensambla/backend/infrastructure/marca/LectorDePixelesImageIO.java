package com.ensambla.backend.infrastructure.marca;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.marca.LogoDeMarca;
import com.ensambla.backend.domain.port.LectorDePixelesPort;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Lee los píxeles del logo con el decodificador de imágenes del JDK, muestreando para no recorrerlos todos. */
@Component
public class LectorDePixelesImageIO implements LectorDePixelesPort {

    private static final int MUESTRA_MAXIMA = 40_000;

    @Override
    public List<Integer> leer(LogoDeMarca logo) {
        BufferedImage imagen = decodificar(logo);
        return muestrear(imagen);
    }

    private BufferedImage decodificar(LogoDeMarca logo) {
        try {
            return exigirImagen(ImageIO.read(new ByteArrayInputStream(logo.contenido())));
        } catch (IOException error) {
            throw logoIlegible();
        }
    }

    private BufferedImage exigirImagen(BufferedImage imagen) {
        if (imagen == null) {
            throw logoIlegible();
        }
        return imagen;
    }

    private BusinessRuleException logoIlegible() {
        return new BusinessRuleException("No pudimos leer el logo: probá con otra imagen PNG o JPG");
    }

    private List<Integer> muestrear(BufferedImage imagen) {
        int paso = pasoDeMuestreo(imagen);
        List<Integer> pixeles = new ArrayList<>();
        for (int y = 0; y < imagen.getHeight(); y += paso) {
            agregarFila(imagen, y, paso, pixeles);
        }
        return pixeles;
    }

    private void agregarFila(BufferedImage imagen, int y, int paso, List<Integer> pixeles) {
        for (int x = 0; x < imagen.getWidth(); x += paso) {
            if (esOpaco(imagen.getRGB(x, y))) {
                pixeles.add(imagen.getRGB(x, y) & 0xFFFFFF);
            }
        }
    }

    private boolean esOpaco(int argb) {
        return (argb >>> 24) >= 128;
    }

    private int pasoDeMuestreo(BufferedImage imagen) {
        long total = (long) imagen.getWidth() * imagen.getHeight();
        return (int) Math.max(1, Math.round(Math.sqrt((double) total / MUESTRA_MAXIMA)));
    }
}
