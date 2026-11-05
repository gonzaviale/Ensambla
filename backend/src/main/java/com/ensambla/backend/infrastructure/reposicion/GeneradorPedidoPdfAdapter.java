package com.ensambla.backend.infrastructure.reposicion;

import com.ensambla.backend.domain.model.reposicion.ItemReposicion;
import com.ensambla.backend.domain.model.reposicion.PedidoReposicion;
import com.ensambla.backend.domain.port.GeneradorPedidoPdfPort;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Escribe el pedido de reposición como una página A4 para mandársela al proveedor: quién pide, a quién, y la tabla de
 * productos con la cantidad de cada uno. No lleva precios ni costos: es un pedido, no una cuenta.
 */
@Component
public class GeneradorPedidoPdfAdapter implements GeneradorPedidoPdfPort {

    private static final Color PETROLEO = new Color(0x00, 0x50, 0x6a);
    private static final Color NARANJA = new Color(0xee, 0x80, 0x00);
    private static final Color GRIS = new Color(0x5c, 0x66, 0x6b);
    private static final Color RAYA = new Color(0xf4, 0xf7, 0xf9);
    private static final Color LINEA = new Color(0xd5, 0xdd, 0xe2);

    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, PETROLEO);
    private static final Font QUIEN = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, NARANJA);
    private static final Font TEXTO = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
    private static final Font NEGRITA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
    private static final Font ENCABEZADO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Font NOTA = FontFactory.getFont(FontFactory.HELVETICA, 9, GRIS);

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale ARGENTINA = Locale.of("es", "AR");
    private static final float[] ANCHOS_DE_COLUMNAS = {5, 1.6f};
    private static final float SEPARACION_DE_LA_TABLA = 14;

    @Override
    public byte[] generar(PedidoReposicion pedido) {
        return generar(pedido, new Document(PageSize.A4, 40, 40, 48, 48));
    }

    byte[] generar(PedidoReposicion pedido, Document documento) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();
            for (Element elemento : contenidoDe(pedido)) {
                documento.add(elemento);
            }
            documento.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo armar el PDF del pedido", e);
        }
        return salida.toByteArray();
    }

    private List<Element> contenidoDe(PedidoReposicion pedido) {
        List<Element> contenido = new ArrayList<>();
        contenido.add(new Paragraph("Pedido de reposición", TITULO));
        contenido.add(quienPide(pedido));
        destinatarioDe(pedido).ifPresent(contenido::add);
        contenido.add(saludo());
        contenido.add(tablaDeProductos(pedido.items()));
        contenido.add(despedida());
        return contenido;
    }

    private Paragraph quienPide(PedidoReposicion pedido) {
        Paragraph quien = new Paragraph(pedido.negocio() + "  ·  " + fecha(pedido.fecha()), QUIEN);
        quien.setSpacingBefore(2);
        return quien;
    }

    private Optional<Paragraph> destinatarioDe(PedidoReposicion pedido) {
        return Optional.ofNullable(pedido.proveedor()).map(proveedor -> new Paragraph("Para: " + proveedor, TEXTO));
    }

    private Paragraph saludo() {
        Paragraph saludo = new Paragraph("Hola, necesito reponer lo siguiente:", TEXTO);
        saludo.setSpacingBefore(SEPARACION_DE_LA_TABLA);
        return saludo;
    }

    private PdfPTable tablaDeProductos(List<ItemReposicion> items) {
        PdfPTable tabla = new PdfPTable(ANCHOS_DE_COLUMNAS);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(SEPARACION_DE_LA_TABLA);
        tabla.setSpacingAfter(SEPARACION_DE_LA_TABLA);
        encabezadosDe(tabla);
        items.forEach(item -> filaDe(tabla, item));
        return tabla;
    }

    private void encabezadosDe(PdfPTable tabla) {
        tabla.addCell(celdaDeEncabezado("Producto", Element.ALIGN_LEFT));
        tabla.addCell(celdaDeEncabezado("Cantidad", Element.ALIGN_RIGHT));
    }

    private void filaDe(PdfPTable tabla, ItemReposicion item) {
        boolean conRaya = tabla.getRows().size() % 2 == 0;
        tabla.addCell(celdaDeFila(item.nombre(), NEGRITA, Element.ALIGN_LEFT, conRaya));
        tabla.addCell(celdaDeFila(cantidad(item.cantidadAPedir()), TEXTO, Element.ALIGN_RIGHT, conRaya));
    }

    private Paragraph despedida() {
        return new Paragraph("Gracias. Pedido armado con Ensambla.", NOTA);
    }

    private PdfPCell celdaDeEncabezado(String texto, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, ENCABEZADO));
        celda.setBackgroundColor(PETROLEO);
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setPadding(6);
        celda.setHorizontalAlignment(alineacion);
        return celda;
    }

    private PdfPCell celdaDeFila(String texto, Font fuente, int alineacion, boolean conRaya) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setBorder(Rectangle.BOTTOM);
        celda.setBorderColor(LINEA);
        celda.setPadding(6);
        celda.setHorizontalAlignment(alineacion);
        celda.setBackgroundColor(conRaya ? RAYA : Color.WHITE);
        return celda;
    }

    private static String fecha(LocalDate fecha) {
        return fecha.format(FECHA);
    }

    private static String cantidad(BigDecimal valor) {
        return new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(ARGENTINA)).format(valor);
    }
}
