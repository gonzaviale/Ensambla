package com.ensambla.backend.infrastructure.reporte;

import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.reporte.PeriodoResumen;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.AvisosResumen;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.ClienteTop;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.Clientes;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.Faltante;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.GastoPorCategoria;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.Gastos;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.ProductoDesalineado;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.ProductoParado;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.ProductoVendido;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.Stock;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.Ventas;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.ZonaResumen;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeneradorResumenPdfAdapterTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final PeriodoResumen PERIODO = new PeriodoResumen(LocalDate.of(2026, 9, 20), HOY);

    private final GeneradorResumenPdfAdapter adapter = new GeneradorResumenPdfAdapter();

    @Test
    @DisplayName("Arma un PDF con el encabezado, los números principales y todas las secciones cuando hay datos")
    void generarShouldWriteEverySectionWhenThereIsData() throws IOException {
        String texto = textoDe(adapter.generar(resumenCompleto()));

        assertThat(texto).contains("Resumen de tu negocio", "Almacén de Ana", "Del 20/09/2026 al 19/10/2026 (30 días)",
                "emitido el 19/10/2026", "Ingresos", "$ 1.250.000", "+12 % contra el período anterior",
                "Ganancia estimada", "$ 310.000", "Resultado", "-$ 5.000",
                "Lo que más se vendió", "Yerba Nobleza Gaucha 1 kg", "Productos parados", "Mermelada BC",
                "Stock", "Aceite Cocinero 900 ml", "Clientes y fiado", "Doña Rosa",
                "Gastos", "Mercadería", "Tus precios frente a la zona (San Justo)", "+13 %", "-3 %",
                "Avisos de Sami", "Qué conviene mirar", "Precio frente a la zona", "Es una comparación, no prueba");
    }

    @Test
    @DisplayName("Sin datos arma igual el PDF y deja afuera las secciones vacías")
    void generarShouldSkipSectionsWithoutData() throws IOException {
        String texto = textoDe(adapter.generar(resumenVacio()));

        assertThat(texto).contains("Resumen de tu negocio", "Sin ventas en el período anterior",
                "Sin tabla de gastos", "Ningún producto está agotado", "0 pendientes", "No tenés avisos pendientes");
        assertThat(texto).doesNotContain("Lo que más se vendió", "Productos parados", "Clientes y fiado",
                "Tus precios frente a la zona", "Qué conviene mirar", "Es una comparación");
    }

    @Test
    @DisplayName("Dice cuántos faltantes quedan sin listar, y cuándo no hay gastos ni desalineados")
    void generarShouldMentionTruncatedListsAndEmptySections() throws IOException {
        Stock stock = new Stock(40, 20, List.of(faltante("Fideos", "2", "8", null)), BigDecimal.ZERO, BigDecimal.ZERO);
        ResumenNegocio resumen = conAjustes(stock, new Gastos(BigDecimal.ZERO, 0, List.of(), new BigDecimal("1000")),
                new ZonaResumen("San Justo", 8, 0, 5, 0, List.of()));

        String texto = textoDe(adapter.generar(resumen));

        assertThat(texto).contains("Y 19 más.", "No anotaste gastos en el período.", "Todos tus precios comparables");
    }

    @Test
    @DisplayName("Una sección cuyo comienzo no entra al pie de la página empieza en la siguiente, entera")
    void generarShouldStartASectionOnTheNextPageWhenItsStartDoesNotFit() throws IOException {
        List<String> paginas = paginasDe(adapter.generar(resumenLargo(10)));

        assertThat(paginas).hasSize(2);
        assertThat(paginas.get(0)).doesNotContain("15 productos. Valen").doesNotContain("Faltante 0");
        assertThat(paginas.get(1)).contains("Stock 15 productos. Valen").contains("Faltante 0", "Faltante 11");
    }

    @Test
    @DisplayName("Si el comienzo de la sección entra, sigue en la otra página sin repetir el título")
    void generarShouldContinueASectionOnTheNextPageWithoutRepeatingItsTitle() throws IOException {
        List<String> paginas = paginasDe(adapter.generar(resumenLargo(8)));

        assertThat(paginas).hasSize(2);
        assertThat(paginas.get(0)).contains("15 productos. Valen").contains("Faltante 0");
        assertThat(paginas.get(1)).doesNotContain("15 productos. Valen").contains("Faltante 11");
    }

    @Test
    @DisplayName("Si el documento falla al escribirse avisa con un error claro")
    void generarShouldFailClearlyWhenTheDocumentCannotBeWritten() {
        Document roto = new Document() {
            @Override
            public boolean add(Element elemento) throws DocumentException {
                throw new DocumentException("falla");
            }
        };

        assertThatThrownBy(() -> adapter.generar(resumenVacio(), roto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No se pudo armar el PDF del resumen");
    }

    @Test
    @DisplayName("Los pesos llevan punto de miles y coma decimal, y los negativos van con signo")
    void pesosShouldUseArgentinaFormat() {
        assertThat(GeneradorResumenPdfAdapter.pesos(new BigDecimal("1150"))).isEqualTo("$ 1.150");
        assertThat(GeneradorResumenPdfAdapter.pesos(new BigDecimal("1150.5"))).isEqualTo("$ 1.150,5");
        assertThat(GeneradorResumenPdfAdapter.pesos(new BigDecimal("-5000"))).isEqualTo("-$ 5.000");
        assertThat(GeneradorResumenPdfAdapter.numero(new BigDecimal("12.5"))).isEqualTo("12,5");
    }

    // --- helpers ---
    private static String textoDe(byte[] pdf) throws IOException {
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        PdfReader lector = new PdfReader(pdf);
        PdfTextExtractor extractor = new PdfTextExtractor(lector);
        StringBuilder texto = new StringBuilder();
        for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
            texto.append(extractor.getTextFromPage(pagina)).append('\n');
        }
        return texto.toString().replaceAll("\\s+", " ");
    }

    private static List<String> paginasDe(byte[] pdf) throws IOException {
        PdfReader lector = new PdfReader(pdf);
        PdfTextExtractor extractor = new PdfTextExtractor(lector);
        List<String> paginas = new ArrayList<>();
        for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
            paginas.add(extractor.getTextFromPage(pagina).replaceAll("\\s+", " ").strip());
        }
        return paginas;
    }

    /** Diez productos vendidos, {@code parados} parados y 12 faltantes: el stock cae justo en el corte de página. */
    private static ResumenNegocio resumenLargo(int parados) {
        List<ProductoVendido> vendidos = new ArrayList<>();
        List<ProductoParado> sinMovimiento = new ArrayList<>();
        List<Faltante> faltantes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            vendidos.add(new ProductoVendido("Producto vendido " + i, new BigDecimal(50 - i),
                    new BigDecimal(100000 - i * 1000), new BigDecimal(20000)));
        }
        for (int i = 0; i < parados; i++) {
            sinMovimiento.add(new ProductoParado("Producto parado " + i, new BigDecimal(10 + i),
                    new BigDecimal(5000 * (i + 1))));
        }
        for (int i = 0; i < 12; i++) {
            faltantes.add(faltante("Faltante " + i, "0", "6", "12"));
        }
        return new ResumenNegocio("Almacén de Ana", "Almacén de prueba", PERIODO, HOY,
                new Ventas(new BigDecimal("1250000"), new BigDecimal("640"), 310, new BigDecimal("41666.67"),
                        new BigDecimal("1116000"), new BigDecimal("12"), new BigDecimal("310000")),
                vendidos, sinMovimiento, new Stock(15, 12, faltantes, new BigDecimal("480000"), new BigDecimal("690000")),
                new Clientes(List.of(), BigDecimal.ZERO, 0), null, null,
                new AvisosResumen(new EnumMap<>(TipoAviso.class), 0, 0, 0, BigDecimal.ZERO));
    }

    private static Faltante faltante(String nombre, String cantidad, String minimo, String pedir) {
        return new Faltante(nombre, new BigDecimal(cantidad), minimo == null ? null : new BigDecimal(minimo),
                pedir == null ? null : new BigDecimal(pedir));
    }

    private static ResumenNegocio conAjustes(Stock stock, Gastos gastos, ZonaResumen zona) {
        ResumenNegocio base = resumenCompleto();
        return new ResumenNegocio(base.negocio(), base.sistema(), base.periodo(), base.emitidoEl(), base.ventas(),
                base.masVendidos(), base.sinMovimiento(), stock, base.clientes(), gastos, zona, base.avisos());
    }

    private static ResumenNegocio resumenCompleto() {
        List<Faltante> faltantes = new ArrayList<>();
        faltantes.add(faltante("Aceite Cocinero 900 ml", "0", "6", "12"));
        faltantes.add(faltante("Fideos Luchetti 500 g", "8", null, null));
        Map<TipoAviso, Long> pendientes = new EnumMap<>(TipoAviso.class);
        pendientes.put(TipoAviso.STOCK, 3L);
        pendientes.put(TipoAviso.PRECIO_ZONA, 2L);
        return new ResumenNegocio(
                "Almacén de Ana", "Almacén de prueba", PERIODO, HOY,
                new Ventas(new BigDecimal("1250000"), new BigDecimal("640"), 310, new BigDecimal("41666.67"),
                        new BigDecimal("1116000"), new BigDecimal("12"), new BigDecimal("310000")),
                List.of(new ProductoVendido("Yerba Nobleza Gaucha 1 kg", new BigDecimal("60"), new BigDecimal("204000"),
                        new BigDecimal("42000")),
                        new ProductoVendido("Producto sin costo", new BigDecimal("5"), new BigDecimal("5000"), null)),
                List.of(new ProductoParado("Mermelada BC La Campagnola 454 g", new BigDecimal("20"),
                        new BigDecimal("36000")), new ProductoParado("Sin costo", new BigDecimal("3"), null)),
                new Stock(15, 2, faltantes, new BigDecimal("480000"), new BigDecimal("690000")),
                new Clientes(List.of(new ClienteTop("Doña Rosa", 4, new BigDecimal("8000"))), new BigDecimal("12000"), 3),
                new Gastos(new BigDecimal("255000"), 6,
                        List.of(new GastoPorCategoria("Mercadería", new BigDecimal("200000"))), new BigDecimal("-5000")),
                new ZonaResumen("San Justo", 8, 1, 3, 1, List.of(
                        new ProductoDesalineado("Fideos Luchetti 500 g", new BigDecimal("1700"), new BigDecimal("1100"),
                                new BigDecimal("1500"), new BigDecimal("13")),
                        new ProductoDesalineado("Yerba Nobleza Gaucha 1 kg", new BigDecimal("3400"),
                                new BigDecimal("3500"), new BigDecimal("4800"), new BigDecimal("-3")))),
                new AvisosResumen(pendientes, 4, 1, 2, new BigDecimal("15000")));
    }

    private static ResumenNegocio resumenVacio() {
        return new ResumenNegocio(
                "Almacén de Ana", "Almacén vacío", PERIODO, HOY,
                new Ventas(BigDecimal.ZERO, BigDecimal.ZERO, 0, BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                List.of(), List.of(),
                new Stock(0, 0, List.of(), BigDecimal.ZERO, BigDecimal.ZERO),
                new Clientes(List.of(), BigDecimal.ZERO, 0), null, null,
                new AvisosResumen(new EnumMap<>(TipoAviso.class), 0, 0, 0, BigDecimal.ZERO));
    }
}
