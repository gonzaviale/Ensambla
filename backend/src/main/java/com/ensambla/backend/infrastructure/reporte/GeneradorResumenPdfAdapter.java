package com.ensambla.backend.infrastructure.reporte;

import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.AvisosResumen;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.Clientes;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.Gastos;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.Stock;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.Ventas;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.ZonaResumen;
import com.ensambla.backend.domain.port.GeneradorResumenPdfPort;
import com.lowagie.text.Chunk;
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
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * Escribe el resumen del negocio como un PDF en A4 pensado para leerse de corrido: encabezado con el período, los
 * números principales en tarjetas y una sección por tema (productos, stock, clientes, gastos, zona y avisos). Las
 * secciones sin datos no se dibujan. Los textos son los del comerciante: pesos con punto de miles y fechas día/mes/año.
 */
@Component
public class GeneradorResumenPdfAdapter implements GeneradorResumenPdfPort {

    private static final Color PETROLEO = new Color(0x00, 0x50, 0x6a);
    private static final Color NARANJA = new Color(0xee, 0x80, 0x00);
    private static final Color GRIS = new Color(0x5c, 0x66, 0x6b);
    private static final Color FONDO = new Color(0xe0, 0xee, 0xf3);
    private static final Color RAYA = new Color(0xf4, 0xf7, 0xf9);
    private static final Color LINEA = new Color(0xd5, 0xdd, 0xe2);

    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, PETROLEO);
    private static final Font QUIEN = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, NARANJA);
    private static final Font SECCION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, PETROLEO);
    private static final Font TEXTO = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, Color.BLACK);
    private static final Font NEGRITA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, Color.BLACK);
    private static final Font ENCABEZADO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font NOTA = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, GRIS);
    private static final Font VALOR = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, PETROLEO);

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale ARGENTINA = Locale.of("es", "AR");
    private static final String SIN_DATO = "-";
    /** Filas que no se separan al comienzo de una sección: título, bajada, encabezados y la primera fila de datos. */
    private static final int COMIENZO_DE_SECCION = 4;

    private static final Map<TipoAviso, String> NOMBRE_DE_AVISO = Map.of(
            TipoAviso.STOCK, "Stock",
            TipoAviso.PRECIO_ZONA, "Precio frente a la zona",
            TipoAviso.ESTANCADO, "Productos sin movimiento",
            TipoAviso.MUY_VENDIDO, "Productos que se venden mucho",
            TipoAviso.COMBO, "Combos",
            TipoAviso.CONSUMO_ZONA, "Lo que pide la zona",
            TipoAviso.REVISAR_PRECIO, "Precios para revisar");

    @Override
    public byte[] generar(ResumenNegocio resumen) {
        return generar(resumen, new Document(PageSize.A4, 36, 36, 40, 54));
    }

    byte[] generar(ResumenNegocio resumen, Document documento) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            PdfWriter escritor = PdfWriter.getInstance(documento, salida);
            escritor.setPageEvent(new Pie(resumen.sistema()));
            Hoja hoja = new Hoja(documento, escritor);
            documento.open();
            encabezado(documento, resumen);
            tarjetas(documento, resumen);
            productos(hoja, resumen);
            stock(hoja, resumen.stock());
            clientes(hoja, resumen.clientes());
            gastos(hoja, resumen.gastos());
            zona(hoja, resumen.zona());
            avisos(hoja, resumen.avisos());
            documento.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo armar el PDF del resumen", e);
        }
        return salida.toByteArray();
    }

    // --- encabezado y tarjetas ---

    private void encabezado(Document documento, ResumenNegocio resumen) throws DocumentException {
        documento.add(new Paragraph("Resumen de tu negocio", TITULO));
        Paragraph quien = new Paragraph(resumen.negocio() + "  ·  " + resumen.sistema(), QUIEN);
        quien.setSpacingBefore(2);
        documento.add(quien);
        Paragraph periodo = new Paragraph("Del " + fecha(resumen.periodo().desde()) + " al "
                + fecha(resumen.periodo().hasta()) + " (" + resumen.periodo().dias() + " días)  ·  emitido el "
                + fecha(resumen.emitidoEl()), NOTA);
        periodo.setSpacingAfter(12);
        documento.add(periodo);
    }

    private void tarjetas(Document documento, ResumenNegocio resumen) throws DocumentException {
        Ventas ventas = resumen.ventas();
        Gastos gastos = resumen.gastos();
        PdfPTable tabla = new PdfPTable(4);
        tabla.setWidthPercentage(100);
        tabla.addCell(tarjeta("Ingresos", pesos(ventas.ingresos()), variacion(ventas)));
        tabla.addCell(tarjeta("Unidades vendidas", numero(ventas.unidades()), ventas.cantidad() + " ventas anotadas"));
        tabla.addCell(tarjeta("Ganancia estimada", opcional(ventas.gananciaEstimada()),
                "Ventas menos el costo de lo vendido"));
        tabla.addCell(tarjeta("Ingreso por día", pesos(ventas.ingresoPorDia()), "En promedio"));
        tabla.addCell(tarjeta("Gastos", gastos == null ? SIN_DATO : pesos(gastos.total()),
                gastos == null ? "Sin tabla de gastos" : gastos.cantidad() + " gastos anotados"));
        tabla.addCell(tarjeta("Resultado", gastos == null ? SIN_DATO : pesos(gastos.resultado()),
                "Ingresos menos gastos"));
        tabla.addCell(tarjeta("Vendido a fiado", pesos(resumen.clientes().fiado()),
                resumen.clientes().ventasFiadas() + " ventas"));
        tabla.addCell(tarjeta("Valor del stock", pesos(resumen.stock().valorACosto()), "A costo"));
        documento.add(tabla);
    }

    private static String variacion(Ventas ventas) {
        if (ventas.variacion() == null) {
            return "Sin ventas en el período anterior";
        }
        return porcentaje(ventas.variacion()) + " contra el período anterior";
    }

    private PdfPCell tarjeta(String etiqueta, String valor, String detalle) {
        Phrase contenido = new Phrase();
        contenido.add(new Chunk(etiqueta + "\n", NOTA));
        contenido.add(new Chunk(valor + "\n", VALOR));
        contenido.add(new Chunk(detalle, NOTA));
        PdfPCell celda = new PdfPCell(contenido);
        celda.setBackgroundColor(FONDO);
        celda.setBorderColor(Color.WHITE);
        celda.setBorderWidth(3);
        celda.setPadding(7);
        return celda;
    }

    // --- secciones ---

    private void productos(Hoja hoja, ResumenNegocio resumen) throws DocumentException {
        if (!resumen.masVendidos().isEmpty()) {
            PdfPTable tabla = seccion("Lo que más se vendió",
                    "Por unidades en el período. La ganancia usa el costo actual de cada producto.",
                    new float[] {4, 1.4f, 2, 2}, "Producto", "Unidades", "Ingresos", "Ganancia");
            resumen.masVendidos().forEach(producto -> fila(tabla, producto.nombre(), numero(producto.unidades()),
                    pesos(producto.ingresos()), opcional(producto.ganancia())));
            agregar(hoja, tabla);
        }
        if (!resumen.sinMovimiento().isEmpty()) {
            PdfPTable tabla = seccion("Productos parados",
                    "Tenés stock y no se vendieron en el período: es plata que está quieta.",
                    new float[] {4, 1.6f, 2.4f}, "Producto", "En stock", "Plata parada (a costo)");
            resumen.sinMovimiento().forEach(producto -> fila(tabla, producto.nombre(), numero(producto.cantidad()),
                    opcional(producto.capital())));
            agregar(hoja, tabla);
        }
    }

    private void stock(Hoja hoja, Stock stock) throws DocumentException {
        PdfPTable tabla = seccion("Stock",
                stock.productos() + " productos. Valen " + pesos(stock.valorACosto()) + " a costo y "
                        + pesos(stock.valorAPrecio()) + " a precio de venta.",
                new float[] {4, 1.4f, 1.4f, 2}, columnas(!stock.faltantes().isEmpty(), "Producto", "Quedan", "Mínimo",
                        "Para llegar al doble"));
        stock.faltantes().forEach(faltante -> fila(tabla, faltante.nombre(), numero(faltante.cantidad()),
                faltante.minimo() == null ? SIN_DATO : numero(faltante.minimo()),
                faltante.pedir() == null ? SIN_DATO : numero(faltante.pedir())));
        if (stock.faltantes().isEmpty()) {
            nota(tabla, "Ningún producto está agotado ni por debajo de su mínimo.");
        } else if (stock.totalFaltantes() > stock.faltantes().size()) {
            nota(tabla, "Y " + (stock.totalFaltantes() - stock.faltantes().size()) + " más.");
        }
        agregar(hoja, tabla);
    }

    private void clientes(Hoja hoja, Clientes clientes) throws DocumentException {
        if (clientes.vacio()) {
            return;
        }
        PdfPTable tabla = seccion("Clientes y fiado",
                "Vendiste " + pesos(clientes.fiado()) + " a fiado en el período (" + clientes.ventasFiadas() + " ventas).",
                new float[] {4, 1.6f, 2.4f}, "Cliente", "Compras", "Ingresos");
        clientes.top().forEach(cliente -> fila(tabla, cliente.nombre(), String.valueOf(cliente.compras()),
                pesos(cliente.ingresos())));
        agregar(hoja, tabla);
    }

    private void gastos(Hoja hoja, Gastos gastos) throws DocumentException {
        if (gastos == null) {
            return;
        }
        PdfPTable tabla = seccion("Gastos",
                "Gastaste " + pesos(gastos.total()) + " en el período. Ingresos menos gastos: "
                        + pesos(gastos.resultado()) + ".",
                new float[] {5, 2.4f}, columnas(!gastos.porCategoria().isEmpty(), "Categoría", "Total"));
        gastos.porCategoria().forEach(categoria -> fila(tabla, categoria.categoria(), pesos(categoria.total())));
        if (gastos.porCategoria().isEmpty()) {
            nota(tabla, "No anotaste gastos en el período.");
        }
        agregar(hoja, tabla);
    }

    private void zona(Hoja hoja, ZonaResumen zona) throws DocumentException {
        if (zona == null) {
            return;
        }
        PdfPTable tabla = seccion("Tus precios frente a la zona (" + zona.nombre() + ")",
                zona.debajo() + " por debajo del rango, " + zona.dentro() + " dentro y " + zona.encima()
                        + " por encima. Son datos agregados y anónimos de " + zona.comercios() + " comercios.",
                new float[] {3.6f, 1.6f, 2.4f, 1.6f}, columnas(!zona.desalineados().isEmpty(), "Producto", "Tu precio",
                        "Rango de la zona", "Diferencia"));
        zona.desalineados().forEach(producto -> fila(tabla, producto.nombre(), pesos(producto.precio()),
                pesos(producto.minimo()) + " a " + pesos(producto.maximo()), porcentaje(producto.porcentaje())));
        if (zona.desalineados().isEmpty()) {
            nota(tabla, "Todos tus precios comparables están dentro del rango de la zona.");
        }
        agregar(hoja, tabla);
    }

    private void avisos(Hoja hoja, AvisosResumen avisos) throws DocumentException {
        PdfPTable tabla = seccion("Avisos de Sami",
                avisos.totalPendientes() + " pendientes. En el período aplicaste " + avisos.aplicados()
                        + " y descartaste " + avisos.descartados() + ".",
                new float[] {5, 1.6f}, columnas(!avisos.pendientes().isEmpty(), "Qué conviene mirar", "Avisos"));
        NOMBRE_DE_AVISO.keySet().stream().sorted()
                .filter(avisos.pendientes()::containsKey)
                .forEach(tipo -> fila(tabla, NOMBRE_DE_AVISO.get(tipo), String.valueOf(avisos.pendientes().get(tipo))));
        if (avisos.pendientes().isEmpty()) {
            nota(tabla, "No tenés avisos pendientes.");
        }
        if (avisos.medidos() > 0) {
            nota(tabla, "De los avisos aplicados ya medimos " + avisos.medidos() + ": el ingreso semanal cambió "
                    + pesos(avisos.ingresoSemanalDelta()) + " entre antes y después. Es una comparación, no prueba "
                    + "que el aviso sea la causa: también influyen la época del año, los feriados o el faltante.");
        }
        agregar(hoja, tabla);
    }

    // --- piezas ---

    /**
     * Una sección es una sola tabla: el título y la bajada son sus dos primeras filas y después van los encabezados de
     * las columnas y las filas. Ninguna fila se repite al cambiar de página (el título no se vuelve a escribir): lo que
     * se cuida con {@link #agregar} es que el principio de la sección no quede solo al pie de una página.
     */
    private PdfPTable seccion(String titulo, String bajada, float[] anchos, String... encabezados)
            throws DocumentException {
        PdfPTable tabla = new PdfPTable(anchos.length);
        tabla.setWidthPercentage(100);
        tabla.setWidths(anchos);
        tabla.setSpacingBefore(14);
        tabla.addCell(textoDeSeccion(titulo, SECCION, anchos.length, 3));
        tabla.addCell(textoDeSeccion(bajada, NOTA, anchos.length, 5));
        for (int columna = 0; columna < encabezados.length; columna++) {
            PdfPCell celda = new PdfPCell(new Phrase(encabezados[columna], ENCABEZADO));
            celda.setBackgroundColor(PETROLEO);
            celda.setBorder(Rectangle.NO_BORDER);
            celda.setPadding(5);
            celda.setHorizontalAlignment(columna == 0 ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            tabla.addCell(celda);
        }
        return tabla;
    }

    /**
     * Agrega la sección al documento cuidando su comienzo: el título, la bajada, los encabezados de las columnas y la
     * primera fila de datos van siempre juntos. Si no entran al pie de la página, la sección empieza en la siguiente;
     * si entran, el resto de las filas puede seguir en la otra página (sin repetir el título).
     */
    private void agregar(Hoja hoja, PdfPTable tabla) throws DocumentException {
        tabla.setTotalWidth(hoja.documento().right() - hoja.documento().left());
        tabla.calculateHeights(true);
        float comienzo = tabla.spacingBefore();
        for (int fila = 0; fila < Math.min(COMIENZO_DE_SECCION, tabla.size()); fila++) {
            comienzo += tabla.getRowHeight(fila);
        }
        if (comienzo > hoja.escritor().getVerticalPosition(true) - hoja.documento().bottom()) {
            hoja.documento().newPage();
        }
        hoja.documento().add(tabla);
    }

    /** Los encabezados de las columnas, sólo si la tabla va a tener filas: sobre una nota sola no tienen sentido. */
    private static String[] columnas(boolean hayFilas, String... nombres) {
        return hayFilas ? nombres : new String[0];
    }

    private PdfPCell textoDeSeccion(String texto, Font fuente, int columnas, float separacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setColspan(columnas);
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setPadding(0);
        celda.setPaddingBottom(separacion);
        return celda;
    }

    private void nota(PdfPTable tabla, String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, NOTA));
        celda.setColspan(tabla.getNumberOfColumns());
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setPaddingTop(6);
        celda.setPaddingLeft(0);
        tabla.addCell(celda);
    }

    private void fila(PdfPTable tabla, String... valores) {
        boolean conRaya = (tabla.getRows().size() % 2) == 0;
        for (int columna = 0; columna < valores.length; columna++) {
            PdfPCell celda = new PdfPCell(new Phrase(valores[columna], columna == 0 ? NEGRITA : TEXTO));
            celda.setBorder(Rectangle.BOTTOM);
            celda.setBorderColor(LINEA);
            celda.setPadding(5);
            celda.setHorizontalAlignment(columna == 0 ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            if (conRaya) {
                celda.setBackgroundColor(RAYA);
            }
            tabla.addCell(celda);
        }
    }

    // --- formatos ---

    private static String fecha(LocalDate fecha) {
        return fecha.format(FECHA);
    }

    /** Pesos con punto de miles y coma decimal, sin decimales si no hacen falta: $ 1.150 y $ 1.150,50. */
    static String pesos(BigDecimal importe) {
        String texto = formato().format(importe.abs());
        return (importe.signum() < 0 ? "-$ " : "$ ") + texto;
    }

    static String numero(BigDecimal valor) {
        return formato().format(valor);
    }

    private static String opcional(BigDecimal valor) {
        return valor == null ? SIN_DATO : pesos(valor);
    }

    private static String porcentaje(BigDecimal valor) {
        return (valor.signum() > 0 ? "+" : "") + valor.toPlainString() + " %";
    }

    private static DecimalFormat formato() {
        return new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(ARGENTINA));
    }

    /** El documento y su escritor: el escritor dice cuánto lugar queda en la página. */
    private record Hoja(Document documento, PdfWriter escritor) {
    }

    /** El pie de cada página: el sistema, la aclaración de los datos y el número de página. */
    private static final class Pie extends PdfPageEventHelper {

        private final String sistema;

        private Pie(String sistema) {
            this.sistema = sistema;
        }

        @Override
        public void onEndPage(PdfWriter escritor, Document documento) {
            PdfPTable pie = new PdfPTable(new float[] {5, 1});
            pie.setTotalWidth(documento.right() - documento.left());
            pie.addCell(celdaDePie("Ensambla  ·  " + sistema + "  ·  Datos cargados por tu negocio; la zona, sólo "
                    + "agregados anónimos", Element.ALIGN_LEFT));
            pie.addCell(celdaDePie("Página " + escritor.getPageNumber(), Element.ALIGN_RIGHT));
            pie.writeSelectedRows(0, -1, documento.left(), documento.bottom() - 10, escritor.getDirectContent());
        }

        private static PdfPCell celdaDePie(String texto, int alineacion) {
            PdfPCell celda = new PdfPCell(new Phrase(texto, NOTA));
            celda.setBorder(Rectangle.TOP);
            celda.setBorderColor(LINEA);
            celda.setHorizontalAlignment(alineacion);
            celda.setPaddingTop(4);
            return celda;
        }
    }
}
