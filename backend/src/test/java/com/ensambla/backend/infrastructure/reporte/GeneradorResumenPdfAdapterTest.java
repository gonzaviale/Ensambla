package com.ensambla.backend.infrastructure.reporte;

import com.ensambla.backend.domain.model.reporte.ResumenNegocio;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.Gastos;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.Stock;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.ZonaResumen;
import com.ensambla.backend.support.ResumenesDePrueba;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.ResumenesDePrueba.faltante;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeneradorResumenPdfAdapterTest {

    private static final String NARANJA = "0.93333 0.50196 0 rg";
    private static final String AMARILLO = "0.92157 0.80784 0.19608 rg";
    private static final String CELESTE = "0.30588 0.69804 0.83137 rg";
    private static final String PETROLEO = "0 0.31373 0.41569 rg";
    private static final String CORAL = "0.97255 0.37255 0.35294 rg";

    private final GeneradorResumenPdfAdapter adapter = new GeneradorResumenPdfAdapter();

    private ResumenNegocio resumen;
    private byte[] pdf;

    @Test
    @DisplayName("Arma un PDF con el encabezado, los números principales y todas las secciones cuando hay datos")
    void elPdfTraeElEncabezadoLosNumerosYTodasLasSeccionesCuandoHayDatos() throws IOException {
        dadoUnNegocioConDatosEnTodasLasSecciones();

        cuandoSeGeneraElPdf();

        entoncesElTextoTrae("Resumen de tu negocio", "Almacén de Ana", "Del 20/09/2026 al 19/10/2026 (30 días)",
                "emitido el 19/10/2026", "Ingresos", "$ 1.250.000", "+12 % contra el período anterior",
                "Ganancia estimada", "$ 310.000", "Resultado", "-$ 5.000",
                "Lo que más se vendió", "Yerba Nobleza Gaucha 1 kg", "Productos parados", "Mermelada BC",
                "Stock", "Aceite Cocinero 900 ml", "Clientes y fiado", "Doña Rosa",
                "Gastos", "Mercadería", "Tus precios frente a la zona (San Justo)", "+13 %", "-3 %",
                "Avisos de Sami", "Qué conviene mirar", "Precio frente a la zona", "Es una comparación, no prueba");
    }

    @Test
    @DisplayName("Sin datos arma igual el PDF y deja afuera las secciones vacías")
    void sinDatosArmaElPdfYDejaAfueraLasSeccionesVacias() throws IOException {
        dadoUnNegocioSinMovimiento();

        cuandoSeGeneraElPdf();

        entoncesElTextoTrae("Resumen de tu negocio", "Sin ventas en el período anterior", "Sin tabla de gastos",
                "Ningún producto está agotado", "0 pendientes", "No tenés avisos pendientes");
        entoncesElTextoNoTrae("Lo que más se vendió", "Productos parados", "Clientes y fiado",
                "Tus precios frente a la zona", "Qué conviene mirar", "Es una comparación");
    }

    @Test
    @DisplayName("Dice cuántos faltantes quedan sin listar, y cuándo no hay gastos ni desalineados")
    void diceCuantosFaltantesQuedanSinListarYCuandoNoHayGastosNiDesalineados() throws IOException {
        dadoUnNegocioConFaltantesSinListarSinGastosYConLaZonaAlineada();

        cuandoSeGeneraElPdf();

        entoncesElTextoTrae("Y 19 más.", "No anotaste gastos en el período.", "Todos tus precios comparables");
    }

    @Test
    @DisplayName("Una sección cuyo comienzo no entra al pie de la página empieza en la siguiente, entera")
    void unaSeccionCuyoComienzoNoEntraAlPieEmpiezaEnLaSiguienteEntera() throws IOException {
        dadoUnNegocioCuyoStockNoEntraAlPieDeLaPrimeraPagina();

        cuandoSeGeneraElPdf();

        entoncesElStockEmpiezaEnLaSegundaPaginaSinCortarse();
    }

    @Test
    @DisplayName("Si el comienzo de la sección entra, sigue en la otra página sin repetir el título")
    void siElComienzoDeLaSeccionEntraSigueEnLaOtraPaginaSinRepetirElTitulo() throws IOException {
        dadoUnNegocioCuyoStockEmpiezaAlPieDeLaPrimeraPagina();

        cuandoSeGeneraElPdf();

        entoncesElStockSigueEnLaSegundaPaginaSinRepetirElTitulo();
    }

    @Test
    @DisplayName("Cada página lleva al tope la banda de los cinco colores de la marca")
    void cadaPaginaLlevaAlTopeLaBandaDeLosCincoColoresDeLaMarca() throws IOException {
        dadoUnNegocioCuyoStockEmpiezaAlPieDeLaPrimeraPagina();

        cuandoSeGeneraElPdf();

        entoncesCadaPaginaTrae(NARANJA, AMARILLO, CELESTE, PETROLEO, CORAL);
    }

    @Test
    @DisplayName("El encabezado lleva a Sami guiñando y el título de los avisos, a Sami pensando")
    void elEncabezadoLlevaASamiGuinandoYElTituloDeLosAvisosASamiPensando() throws IOException {
        dadoUnNegocioConDatosEnTodasLasSecciones();

        cuandoSeGeneraElPdf();

        entoncesLaPaginaTraeImagenes(1);
        entoncesLaPaginaTraeImagenes(2);
    }

    @Test
    @DisplayName("El texto va en Plus Jakarta Sans, la tipografía de Ensambla")
    void elTextoVaEnPlusJakartaSansLaTipografiaDeEnsambla() throws IOException {
        dadoUnNegocioConDatosEnTodasLasSecciones();

        cuandoSeGeneraElPdf();

        entoncesElPdfIncrustaLaFuente("PlusJakartaSans");
    }

    @Test
    @DisplayName("Si el documento falla al escribirse avisa con un error claro")
    void siElDocumentoFallaAlEscribirseAvisaConUnErrorClaro() {
        dadoUnNegocioSinMovimiento();

        assertThatThrownBy(this::cuandoElDocumentoFallaAlEscribirse)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No se pudo armar el PDF del resumen");
    }

    @Test
    @DisplayName("Los pesos llevan punto de miles y coma decimal, y los negativos van con signo")
    void losPesosLlevanPuntoDeMilesYComaDecimalYLosNegativosVanConSigno() {
        assertThat(GeneradorResumenPdfAdapter.pesos(new BigDecimal("1150"))).isEqualTo("$ 1.150");
        assertThat(GeneradorResumenPdfAdapter.pesos(new BigDecimal("1150.5"))).isEqualTo("$ 1.150,5");
        assertThat(GeneradorResumenPdfAdapter.pesos(new BigDecimal("-5000"))).isEqualTo("-$ 5.000");
        assertThat(GeneradorResumenPdfAdapter.numero(new BigDecimal("12.5"))).isEqualTo("12,5");
    }

    // --- dado ---

    private void dadoUnNegocioConDatosEnTodasLasSecciones() {
        resumen = ResumenesDePrueba.completo();
    }

    private void dadoUnNegocioSinMovimiento() {
        resumen = ResumenesDePrueba.vacio();
    }

    private void dadoUnNegocioConFaltantesSinListarSinGastosYConLaZonaAlineada() {
        Stock stock = new Stock(40, 20, List.of(faltante("Fideos", "2", "8", null)), BigDecimal.ZERO, BigDecimal.ZERO);
        Gastos sinGastos = new Gastos(BigDecimal.ZERO, 0, List.of(), new BigDecimal("1000"));
        ZonaResumen zonaAlineada = new ZonaResumen("San Justo", 8, 0, 5, 0, List.of());
        resumen = ResumenesDePrueba.conAjustes(stock, sinGastos, zonaAlineada);
    }

    private void dadoUnNegocioCuyoStockNoEntraAlPieDeLaPrimeraPagina() {
        resumen = ResumenesDePrueba.largo(10);
    }

    private void dadoUnNegocioCuyoStockEmpiezaAlPieDeLaPrimeraPagina() {
        resumen = ResumenesDePrueba.largo(5);
    }

    // --- cuando ---

    private void cuandoSeGeneraElPdf() {
        pdf = adapter.generar(resumen);
    }

    private void cuandoElDocumentoFallaAlEscribirse() {
        adapter.generar(resumen, new Document() {
            @Override
            public boolean add(Element elemento) throws DocumentException {
                throw new DocumentException("falla");
            }
        });
    }

    // --- entonces ---

    private void entoncesElTextoTrae(String... textos) throws IOException {
        assertThat(String.join(" ", paginas())).contains(textos);
    }

    private void entoncesElTextoNoTrae(String... textos) throws IOException {
        assertThat(String.join(" ", paginas())).doesNotContain(textos);
    }

    private void entoncesElStockEmpiezaEnLaSegundaPaginaSinCortarse() throws IOException {
        List<String> paginas = paginas();
        assertThat(paginas).hasSize(2);
        assertThat(paginas.get(0)).doesNotContain("15 productos. Valen").doesNotContain("Faltante 0");
        assertThat(paginas.get(1)).contains("Stock 15 productos. Valen").contains("Faltante 0", "Faltante 11");
    }

    private void entoncesElStockSigueEnLaSegundaPaginaSinRepetirElTitulo() throws IOException {
        List<String> paginas = paginas();
        assertThat(paginas).hasSize(2);
        assertThat(paginas.get(0)).contains("15 productos. Valen").contains("Faltante 0");
        assertThat(paginas.get(1)).doesNotContain("15 productos. Valen").contains("Faltante 11");
    }

    private void entoncesCadaPaginaTrae(String... colores) throws IOException {
        PdfReader lector = new PdfReader(pdf);
        for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
            assertThat(new String(lector.getPageContent(pagina), StandardCharsets.ISO_8859_1)).contains(colores);
        }
    }

    private void entoncesLaPaginaTraeImagenes(int pagina) throws IOException {
        PdfDictionary recursos = new PdfReader(pdf).getPageN(pagina).getAsDict(PdfName.RESOURCES);
        PdfDictionary objetos = recursos.getAsDict(PdfName.XOBJECT);
        assertThat(imagenesDe(objetos)).isPositive();
    }

    private void entoncesElPdfIncrustaLaFuente(String nombre) {
        assertThat(new String(pdf, StandardCharsets.ISO_8859_1)).contains(nombre);
    }

    // --- apoyo ---

    private static long imagenesDe(PdfDictionary objetos) {
        return objetos.getKeys().stream()
                .map(clave -> (PdfDictionary) PdfReader.getPdfObject(objetos.get(clave)))
                .filter(objeto -> PdfName.IMAGE.equals(objeto.get(PdfName.SUBTYPE)))
                .count();
    }

    private List<String> paginas() throws IOException {
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        PdfReader lector = new PdfReader(pdf);
        PdfTextExtractor extractor = new PdfTextExtractor(lector);
        List<String> paginas = new ArrayList<>();
        for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
            paginas.add(extractor.getTextFromPage(pagina).replaceAll("\\s+", " ").strip());
        }
        return paginas;
    }
}
