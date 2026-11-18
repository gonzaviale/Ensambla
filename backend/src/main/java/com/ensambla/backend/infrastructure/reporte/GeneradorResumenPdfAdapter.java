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
import com.lowagie.text.Image;
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

import static com.ensambla.backend.infrastructure.reporte.MarcaEnsambla.CREMA;
import static com.ensambla.backend.infrastructure.reporte.MarcaEnsambla.LINEA;
import static com.ensambla.backend.infrastructure.reporte.MarcaEnsambla.NARANJA;
import static com.ensambla.backend.infrastructure.reporte.MarcaEnsambla.PETROLEO;

/**
 * Escribe el resumen del negocio como un PDF en A4 pensado para leerse de corrido: encabezado con la marca y el período,
 * los números principales en tarjetas y una sección por tema (productos, stock, clientes, gastos, zona y avisos). Las
 * secciones sin datos no se dibujan. Los textos son los del comerciante: pesos con punto de miles y fechas día/mes/año.
 * La identidad es la del sistema: Plus Jakarta Sans, la banda de cinco colores, el naranja como acento y Sami.
 */
@Component
public class GeneradorResumenPdfAdapter implements GeneradorResumenPdfPort {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale ARGENTINA = Locale.of("es", "AR");
    private static final String SIN_DATO = "-";
    /** Filas que no se separan al comienzo de una sección: título, bajada, encabezados y la primera fila de datos. */
    private static final int COMIENZO_DE_SECCION = 4;
    private static final float ALTO_DEL_LOGO = 22;
    private static final float LADO_DE_SAMI_EN_TITULO = 34;

    private static final Map<TipoAviso, String> NOMBRE_DE_AVISO = Map.of(
            TipoAviso.STOCK, "Stock",
            TipoAviso.PRECIO_ZONA, "Precio frente a la zona",
            TipoAviso.ESTANCADO, "Productos sin movimiento",
            TipoAviso.MUY_VENDIDO, "Productos que se venden mucho",
            TipoAviso.COMBO, "Combos",
            TipoAviso.CONSUMO_ZONA, "Lo que pide la zona",
            TipoAviso.REVISAR_PRECIO, "Precios para revisar");

    private final RecursosDeMarca recursos;
    private final Fuentes fuentes;
    private final EstiloDeTarjeta tarjetaNormal;
    private final EstiloDeTarjeta tarjetaDestacada;

    public GeneradorResumenPdfAdapter() {
        this(new RecursosDeMarca());
    }

    GeneradorResumenPdfAdapter(RecursosDeMarca recursos) {
        this.recursos = recursos;
        this.fuentes = Fuentes.de(recursos);
        this.tarjetaNormal = new EstiloDeTarjeta(CREMA, fuentes.nota(), fuentes.valor());
        this.tarjetaDestacada = new EstiloDeTarjeta(PETROLEO, fuentes.notaClara(), fuentes.valorClaro());
    }

    @Override
    public byte[] generar(ResumenNegocio resumen) {
        return generar(resumen, new Document(PageSize.A4, 36, 36, 40, 54));
    }

    byte[] generar(ResumenNegocio resumen, Document documento) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            PdfWriter escritor = PdfWriter.getInstance(documento, salida);
            escritor.setPageEvent(new Pie(resumen.sistema(), fuentes));
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
        PdfPTable tabla = new PdfPTable(new float[] {4, 1});
        tabla.setWidthPercentage(100);
        tabla.addCell(celdaDelNegocio(resumen));
        tabla.addCell(celdaDeSami());
        tabla.setSpacingAfter(12);
        documento.add(tabla);
    }

    private PdfPCell celdaDelNegocio(ResumenNegocio resumen) {
        PdfPCell celda = celdaSinBorde(new PdfPCell());
        celda.addElement(logoConPalabra());
        celda.addElement(titulo());
        celda.addElement(quien(resumen));
        celda.addElement(periodo(resumen));
        return celda;
    }

    private PdfPTable logoConPalabra() {
        PdfPTable tabla = new PdfPTable(new float[] {ALTO_DEL_LOGO * 1.2f, 150});
        tabla.setTotalWidth(ALTO_DEL_LOGO * 1.2f + 150);
        tabla.setLockedWidth(true);
        tabla.setHorizontalAlignment(Element.ALIGN_LEFT);
        tabla.addCell(celdaDelLogo());
        tabla.addCell(celdaDeLaPalabra());
        return tabla;
    }

    private PdfPCell celdaDelLogo() {
        PdfPCell celda = celdaSinBorde(new PdfPCell());
        celda.setFixedHeight(ALTO_DEL_LOGO);
        celda.setCellEvent(new LogoDeMarca());
        return celda;
    }

    private PdfPCell celdaDeLaPalabra() {
        PdfPCell celda = celdaSinBorde(new PdfPCell(new Phrase("Ensambla", fuentes.palabra())));
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return celda;
    }

    private Paragraph titulo() {
        Paragraph titulo = new Paragraph("Resumen de tu negocio", fuentes.titulo());
        titulo.setSpacingBefore(10);
        return titulo;
    }

    private Paragraph quien(ResumenNegocio resumen) {
        Paragraph quien = new Paragraph(resumen.negocio() + "  ·  " + resumen.sistema(), fuentes.quien());
        quien.setSpacingBefore(2);
        return quien;
    }

    private Paragraph periodo(ResumenNegocio resumen) {
        return new Paragraph("Del " + fecha(resumen.periodo().desde()) + " al " + fecha(resumen.periodo().hasta())
                + " (" + resumen.periodo().dias() + " días)  ·  emitido el " + fecha(resumen.emitidoEl()),
                fuentes.nota());
    }

    private PdfPCell celdaDeSami() {
        PdfPCell celda = celdaSinBorde(new PdfPCell(recursos.imagen("sami-guino.png"), true));
        celda.setHorizontalAlignment(Element.ALIGN_RIGHT);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return celda;
    }

    private void tarjetas(Document documento, ResumenNegocio resumen) throws DocumentException {
        PdfPTable tabla = new PdfPTable(4);
        tabla.setWidthPercentage(100);
        tarjetasDeVentas(tabla, resumen.ventas());
        tarjetasDeCuentas(tabla, resumen);
        documento.add(tabla);
    }

    private void tarjetasDeVentas(PdfPTable tabla, Ventas ventas) {
        tabla.addCell(tarjeta(tarjetaDestacada, "Ingresos", pesos(ventas.ingresos()), variacion(ventas)));
        tabla.addCell(tarjeta(tarjetaNormal, "Unidades vendidas", numero(ventas.unidades()),
                ventas.cantidad() + " ventas anotadas"));
        tabla.addCell(tarjeta(tarjetaNormal, "Ganancia estimada", opcional(ventas.gananciaEstimada()),
                "Ventas menos el costo de lo vendido"));
        tabla.addCell(tarjeta(tarjetaNormal, "Ingreso por día", pesos(ventas.ingresoPorDia()), "En promedio"));
    }

    private void tarjetasDeCuentas(PdfPTable tabla, ResumenNegocio resumen) {
        Gastos gastos = resumen.gastos();
        tabla.addCell(tarjeta(tarjetaNormal, "Gastos", gastos == null ? SIN_DATO : pesos(gastos.total()),
                gastos == null ? "Sin tabla de gastos" : gastos.cantidad() + " gastos anotados"));
        tabla.addCell(tarjeta(tarjetaNormal, "Resultado", gastos == null ? SIN_DATO : pesos(gastos.resultado()),
                "Ingresos menos gastos"));
        tabla.addCell(tarjeta(tarjetaNormal, "Vendido a fiado", pesos(resumen.clientes().fiado()),
                resumen.clientes().ventasFiadas() + " ventas"));
        tabla.addCell(tarjeta(tarjetaNormal, "Valor del stock", pesos(resumen.stock().valorACosto()), "A costo"));
    }

    private static String variacion(Ventas ventas) {
        if (ventas.variacion() == null) {
            return "Sin ventas en el período anterior";
        }
        return porcentaje(ventas.variacion()) + " contra el período anterior";
    }

    /** Fondo crema con el borde izquierdo naranja; la tarjeta destacada va en petróleo, como en el sistema. */
    private PdfPCell tarjeta(EstiloDeTarjeta estilo, String etiqueta, String valor, String detalle) {
        Phrase contenido = new Phrase();
        contenido.add(new Chunk(etiqueta + "\n", estilo.etiqueta()));
        contenido.add(new Chunk(valor + "\n", estilo.valor()));
        contenido.add(new Chunk(detalle, estilo.etiqueta()));
        PdfPCell celda = new PdfPCell(contenido);
        celda.setBackgroundColor(estilo.fondo());
        celda.setBorderColor(Color.WHITE);
        celda.setBorderWidth(3);
        celda.setCellEvent(new FiloDeTarjeta());
        celda.setPadding(7);
        celda.setPaddingLeft(10);
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
        PdfPTable tabla = seccion(tituloConSami("Avisos de Sami"),
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

    private Phrase tituloConSami(String titulo) {
        Image sami = recursos.imagen("sami-pensando.png");
        sami.scaleToFit(LADO_DE_SAMI_EN_TITULO, LADO_DE_SAMI_EN_TITULO);
        Phrase frase = new Phrase();
        frase.add(new Chunk(sami, 0, -3, true));
        frase.add(new Chunk("  " + titulo, fuentes.seccion()));
        return frase;
    }

    // --- piezas ---

    private PdfPTable seccion(String titulo, String bajada, float[] anchos, String... encabezados)
            throws DocumentException {
        return seccion(new Phrase(titulo, fuentes.seccion()), bajada, anchos, encabezados);
    }

    /**
     * Una sección es una sola tabla: el título (con su filete naranja) y la bajada son sus dos primeras filas y después
     * van los encabezados de las columnas y las filas. Ninguna fila se repite al cambiar de página (el título no se
     * vuelve a escribir): lo que se cuida con {@link #agregar} es que el principio de la sección no quede solo al pie
     * de una página.
     */
    private PdfPTable seccion(Phrase titulo, String bajada, float[] anchos, String... encabezados)
            throws DocumentException {
        PdfPTable tabla = new PdfPTable(anchos.length);
        tabla.setWidthPercentage(100);
        tabla.setWidths(anchos);
        tabla.setSpacingBefore(14);
        tabla.addCell(celdaDelTitulo(titulo, anchos.length));
        tabla.addCell(textoDeSeccion(bajada, anchos.length));
        agregarEncabezados(tabla, encabezados);
        return tabla;
    }

    private PdfPCell celdaDelTitulo(Phrase titulo, int columnas) {
        PdfPCell celda = new PdfPCell(titulo);
        celda.setColspan(columnas);
        celda.setBorder(Rectangle.BOTTOM);
        celda.setBorderColor(NARANJA);
        celda.setBorderWidth(1.5f);
        celda.setPadding(0);
        celda.setPaddingBottom(4);
        return celda;
    }

    private PdfPCell textoDeSeccion(String texto, int columnas) {
        PdfPCell celda = celdaSinBorde(new PdfPCell(new Phrase(texto, fuentes.nota())));
        celda.setColspan(columnas);
        celda.setPadding(0);
        celda.setPaddingTop(5);
        celda.setPaddingBottom(5);
        return celda;
    }

    private void agregarEncabezados(PdfPTable tabla, String[] encabezados) {
        for (int columna = 0; columna < encabezados.length; columna++) {
            PdfPCell celda = celdaSinBorde(new PdfPCell(new Phrase(encabezados[columna], fuentes.encabezado())));
            celda.setBackgroundColor(PETROLEO);
            celda.setPadding(5);
            celda.setHorizontalAlignment(columna == 0 ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            tabla.addCell(celda);
        }
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

    private static PdfPCell celdaSinBorde(PdfPCell celda) {
        celda.setBorder(Rectangle.NO_BORDER);
        return celda;
    }

    private void nota(PdfPTable tabla, String texto) {
        PdfPCell celda = celdaSinBorde(new PdfPCell(new Phrase(texto, fuentes.nota())));
        celda.setColspan(tabla.getNumberOfColumns());
        celda.setPaddingTop(6);
        celda.setPaddingLeft(0);
        tabla.addCell(celda);
    }

    private void fila(PdfPTable tabla, String... valores) {
        boolean conRaya = (tabla.getRows().size() % 2) == 0;
        for (int columna = 0; columna < valores.length; columna++) {
            Font fuente = columna == 0 ? fuentes.negrita() : fuentes.texto();
            PdfPCell celda = new PdfPCell(new Phrase(valores[columna], fuente));
            celda.setBorder(Rectangle.BOTTOM);
            celda.setBorderColor(LINEA);
            celda.setPadding(5);
            celda.setHorizontalAlignment(columna == 0 ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            if (conRaya) {
                celda.setBackgroundColor(CREMA);
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

    /** Cómo se ve una tarjeta de número: su fondo y los textos de la etiqueta y del valor. */
    private record EstiloDeTarjeta(Color fondo, Font etiqueta, Font valor) {
    }

    /** En cada página: la banda de cinco colores al tope y, al pie, el sistema, la aclaración y el número de página. */
    private static final class Pie extends PdfPageEventHelper {

        private final String sistema;
        private final Fuentes fuentes;

        private Pie(String sistema, Fuentes fuentes) {
            this.sistema = sistema;
            this.fuentes = fuentes;
        }

        @Override
        public void onEndPage(PdfWriter escritor, Document documento) {
            MarcaEnsambla.banda(escritor.getDirectContent(), documento.getPageSize());
            pie(escritor, documento).writeSelectedRows(0, -1, documento.left(), documento.bottom() - 10,
                    escritor.getDirectContent());
        }

        private PdfPTable pie(PdfWriter escritor, Document documento) {
            PdfPTable pie = new PdfPTable(new float[] {5, 1});
            pie.setTotalWidth(documento.right() - documento.left());
            pie.addCell(celdaDelSistema());
            pie.addCell(celdaDePie(new Phrase("Página " + escritor.getPageNumber(), fuentes.nota()),
                    Element.ALIGN_RIGHT));
            return pie;
        }

        private PdfPCell celdaDelSistema() {
            Phrase texto = new Phrase();
            texto.add(new Chunk("Ensambla", fuentes.pie()));
            texto.add(new Chunk("  ·  " + sistema + "  ·  Datos cargados por tu negocio; la zona, sólo agregados "
                    + "anónimos", fuentes.nota()));
            return celdaDePie(texto, Element.ALIGN_LEFT);
        }

        private static PdfPCell celdaDePie(Phrase texto, int alineacion) {
            PdfPCell celda = new PdfPCell(texto);
            celda.setBorder(Rectangle.TOP);
            celda.setBorderColor(NARANJA);
            celda.setBorderWidth(1);
            celda.setHorizontalAlignment(alineacion);
            celda.setPaddingTop(4);
            return celda;
        }
    }
}
