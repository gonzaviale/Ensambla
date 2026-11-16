package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.marca.ColorHex;

import java.util.ArrayList;
import java.util.List;

/**
 * Una forma de componer el cartel. La base reparte los textos en la zona de texto; cada variante decide dónde está esa
 * zona, qué adornos lleva y sobre qué color cae cada texto.
 */
public abstract class DisenoDeVariante {

    private static final double INTERLINEADO_MINIMO = 1.2;
    private static final int LINEAS_DE_REFERENCIA = 3;

    public final List<ElementoDeCartel> componer(ContextoDeDiseno contexto) {
        List<ElementoDeCartel> elementos = new ArrayList<>(adornos(contexto));
        elementos.addAll(textosDeLaZona(contexto));
        elementos.addAll(textoDeLaMarca(contexto));
        return elementos;
    }

    public abstract ColorHex fondo(ContextoDeDiseno contexto);

    protected abstract List<ElementoDeCartel> adornos(ContextoDeDiseno contexto);

    protected abstract Zona zonaDeTexto(ContextoDeDiseno contexto);

    protected abstract Zona zonaDeMarca(ContextoDeDiseno contexto);

    protected abstract ColorHex superficieDeTexto(ContextoDeDiseno contexto);

    protected abstract ColorHex superficieDeMarca(ContextoDeDiseno contexto);

    protected abstract AlineacionDeTexto alineacion();

    protected ColorHex superficieDeLoDestacado(ContextoDeDiseno contexto) {
        return superficieDeTexto(contexto);
    }

    private ColorHex superficieDe(RolDeTexto rol, ContextoDeDiseno contexto) {
        return rol.esDestacado() ? superficieDeLoDestacado(contexto) : superficieDeTexto(contexto);
    }

    private List<ElementoDeCartel> textosDeLaZona(ContextoDeDiseno contexto) {
        ContenidoDeCartel contenido = contexto.contenido();
        List<ElementoDeCartel> textos = new ArrayList<>();
        textos.addAll(textoSimple(RolDeTexto.TITULAR, contenido.titular(), contexto));
        textos.addAll(textoSimple(RolDeTexto.PROTAGONISTA, contenido.protagonista(), contexto));
        textos.addAll(textoSimple(RolDeTexto.BAJADA, contenido.bajada(), contexto));
        textos.addAll(textoSimple(RolDeTexto.DETALLE, contenido.detalle(), contexto));
        textos.addAll(lineas(contenido.lineas(), contexto));
        return textos;
    }

    private List<ElementoDeCartel> textoSimple(RolDeTexto rol, String texto, ContextoDeDiseno contexto) {
        if (texto.isEmpty()) {
            return List.of();
        }
        return List.of(escribir(texto, rol, rol.lugarEn(zonaDeTexto(contexto)), contexto));
    }

    private List<ElementoDeCartel> lineas(List<String> lineas, ContextoDeDiseno contexto) {
        Zona lugar = RolDeTexto.LINEAS.lugarEn(zonaDeTexto(contexto));
        int reparto = Math.max(lineas.size(), LINEAS_DE_REFERENCIA);
        exigirAlturaLegible(lugar.alto() / reparto, contexto);
        List<ElementoDeCartel> textos = new ArrayList<>();
        for (int i = 0; i < lineas.size(); i++) {
            textos.add(escribir(lineas.get(i), RolDeTexto.LINEAS, lugar.franja((double) i / reparto, 1.0 / reparto),
                    contexto));
        }
        return textos;
    }

    private void exigirAlturaLegible(double alturaDeLinea, ContextoDeDiseno contexto) {
        if (alturaDeLinea < contexto.formato().tamanioMinimoDeTexto() * INTERLINEADO_MINIMO) {
            throw new BusinessRuleException("Hay demasiadas líneas para este formato: elegí otro o poné menos");
        }
    }

    private TextoDeCartel escribir(String texto, RolDeTexto rol, Zona franja, ContextoDeDiseno contexto) {
        double tamanio = rol.tamanioEn(franja);
        ColorHex color = rol.colorSobre(superficieDe(rol, contexto), contexto.paleta());
        TextoDeCartel suelto = new TextoDeCartel(texto, alineacion().puntoDe(zonaDeTexto(contexto)),
                baseDe(franja, tamanio), tamanio, rol.peso(), color, alineacion());
        return suelto.ajustadoA(zonaDeTexto(contexto).ancho(), contexto.formato().tamanioMinimoDeTexto());
    }

    private List<ElementoDeCartel> textoDeLaMarca(ContextoDeDiseno contexto) {
        String negocio = contexto.contenido().negocio();
        if (negocio.isEmpty()) {
            return List.of();
        }
        Zona zona = zonaDeMarca(contexto);
        double tamanio = zona.alto() * 0.45;
        TextoDeCartel suelto = new TextoDeCartel(negocio, alineacion().puntoDe(zona), baseDe(zona, tamanio), tamanio,
                700, contexto.paleta().tintaSobre(superficieDeMarca(contexto)), alineacion());
        return List.of(suelto.ajustadoA(zona.ancho(), contexto.formato().tamanioMinimoDeTexto()));
    }

    private double baseDe(Zona franja, double tamanio) {
        return franja.y() + franja.alto() / 2 + tamanio * 0.35;
    }
}
