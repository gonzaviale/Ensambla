package com.ensambla.backend.infrastructure.reposicion;

import com.ensambla.backend.domain.model.reposicion.ItemReposicion;
import com.ensambla.backend.domain.model.reposicion.PedidoReposicion;
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
import java.util.List;

import static com.ensambla.backend.support.ReposicionDePrueba.ACEITE_ID;
import static com.ensambla.backend.support.ReposicionDePrueba.YERBA_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeneradorPedidoPdfAdapterTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 19);

    private final GeneradorPedidoPdfAdapter adapter = new GeneradorPedidoPdfAdapter();

    @Test
    @DisplayName("Arma un PDF con quién pide, a quién, y cada producto con su cantidad")
    void generarShouldWriteTheOrderWithItsProducts() throws IOException {
        PedidoReposicion pedido = pedidoCon("Molinos", yerba("17"), aceite("2.5"));

        String texto = textoDe(adapter.generar(pedido));

        assertThat(texto).contains("Pedido de reposición", "Almacén de Ana", "19/10/2026", "Para: Molinos",
                "Hola, necesito reponer lo siguiente:", "Producto", "Cantidad", "Yerba Playadito 1 kg", "17",
                "Aceite Cocinero 900 ml", "2,5", "Pedido armado con Ensambla");
    }

    @Test
    @DisplayName("Sin proveedor no escribe el destinatario")
    void generarShouldSkipTheRecipientWhenThereIsNoSupplier() throws IOException {
        PedidoReposicion pedido = pedidoCon(null, yerba("17"));

        String texto = textoDe(adapter.generar(pedido));

        assertThat(texto).doesNotContain("Para:");
    }

    @Test
    @DisplayName("No muestra precios ni costos")
    void generarShouldNotShowPricesOrCosts() throws IOException {
        PedidoReposicion pedido = pedidoCon("Molinos", yerba("17"));

        String texto = textoDe(adapter.generar(pedido));

        assertThat(texto).doesNotContain("$", "Precio", "Costo");
    }

    @Test
    @DisplayName("Falla con un mensaje claro cuando el documento no se puede escribir")
    void generarShouldFailClearlyWhenTheDocumentCannotBeWritten() {
        Document roto = documentoQueFalla();
        PedidoReposicion pedido = pedidoCon("Molinos", yerba("17"));

        assertThatThrownBy(() -> adapter.generar(pedido, roto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No se pudo armar el PDF del pedido");
    }

    // --- arrange ---
    private PedidoReposicion pedidoCon(String proveedor, ItemReposicion... items) {
        return PedidoReposicion.armar("Almacén de Ana", proveedor, FECHA, List.of(items));
    }

    private ItemReposicion yerba(String cantidadAPedir) {
        return new ItemReposicion(YERBA_ID, "Yerba Playadito 1 kg", "Molinos", BigDecimal.ONE,
                new BigDecimal(cantidadAPedir), "Quedan 1 (mínimo 10)", 1);
    }

    private ItemReposicion aceite(String cantidadAPedir) {
        return new ItemReposicion(ACEITE_ID, "Aceite Cocinero 900 ml", null, BigDecimal.ZERO,
                new BigDecimal(cantidadAPedir), "Agotado", 2);
    }

    private Document documentoQueFalla() {
        return new Document() {
            @Override
            public boolean add(Element elemento) throws DocumentException {
                throw new DocumentException("falla");
            }
        };
    }

    // --- act ---
    private static String textoDe(byte[] pdf) throws IOException {
        PdfReader lector = new PdfReader(pdf);
        PdfTextExtractor extractor = new PdfTextExtractor(lector);
        StringBuilder texto = new StringBuilder();
        for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
            texto.append(extractor.getTextFromPage(pagina)).append('\n');
        }
        lector.close();
        return texto.toString();
    }
}
