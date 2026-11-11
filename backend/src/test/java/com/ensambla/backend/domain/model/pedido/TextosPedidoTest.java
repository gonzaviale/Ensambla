package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.ensambla.backend.support.PedidoTestData.producto;
import static org.assertj.core.api.Assertions.assertThat;

class TextosPedidoTest {

    private static final Comercio PEPE = new Comercio(NegocioId.de("a0000000-0000-4000-8000-000000000001"),
            "Almacén Don Pepe");
    private static final Comercio ROSA = new Comercio(NegocioId.de("a0000000-0000-4000-8000-000000000002"),
            "Almacén Doña Rosa");

    @Test
    @DisplayName("La oferta de seguir nombra al último comercio y explica cómo cambiarlo")
    void bienvenidaConUltimoShouldNameTheBusiness() {
        assertThat(TextosPedido.bienvenidaConUltimo("Almacén Don Pepe"))
                .contains("*Almacén Don Pepe*")
                .contains("*SÍ*");
    }

    @Test
    @DisplayName("El aviso de que no encontró el comercio repite lo que se escribió sin los espacios de los bordes")
    void comercioNoEncontradoShouldEchoTheText() {
        assertThat(TextosPedido.comercioNoEncontrado("  panadería luna ")).contains("«panadería luna»");
    }

    @Test
    @DisplayName("La lista de comercios parecidos los numera desde 1 y avisa cómo descartarla")
    void variosComerciosShouldNumberTheOptions() {
        assertThat(TextosPedido.variosComercios(List.of(PEPE, ROSA))).isEqualTo("""
                Encontré varios comercios parecidos. Respondé con el número:
                1) Almacén Don Pepe
                2) Almacén Doña Rosa
                O respondé NO para escribir otro nombre.""");
    }

    @Test
    @DisplayName("Una opción inválida dice entre qué números elegir")
    void opcionInvalidaShouldTellTheRange() {
        assertThat(TextosPedido.opcionInvalida(3)).contains("entre 1 y 3");
    }

    @Test
    @DisplayName("La lista muestra cada renglón con su precio, las alternativas, el límite de stock, el total y lo que falta")
    void listaShouldShowEveryLineWithPricesAndMissingProducts() {
        ProductoEnStock yerba = producto("Yerba Playadito 1 kg", "3500", "10");
        ProductoEnStock aceite = producto("Aceite Cocinero 900 ml", "2000.50", "3");
        ProductoEnStock natura = producto("Aceite Natura 900 ml", "2400", "8");
        ProductoEnStock fideos = producto("Fideos Matarazzo 500 g", "1200", "20");
        ArmadoDeLista armado = new ArmadoDeLista(List.of(
                LineaDeCompra.de(yerba, BigDecimal.valueOf(2), List.of()),
                LineaDeCompra.de(aceite, BigDecimal.TEN, List.of(natura, yerba))),
                List.of(new ProductoFaltante("fideos tallarin", fideos), new ProductoFaltante("pañales", null)));

        assertThat(TextosPedido.lista("Almacén Don Pepe", armado)).isEqualTo("""
                Tu pedido en *Almacén Don Pepe*:
                1) Yerba Playadito 1 kg x2 — $7.000
                2) Aceite Cocinero 900 ml x3 — $6.001,50 (solo tengo 3)
                   También tengo: Aceite Natura 900 ml, Yerba Playadito 1 kg
                Total: $13.001,50
                No tengo «fideos tallarin», pero tengo Fideos Matarazzo 500 g ($1.200).
                No tengo «pañales».
                Respondé *SÍ* para confirmar tu pedido, *NO* para descartarlo o *CAMBIAR 1* (el número del renglón) para elegir otra marca.
                Para pedir otra cosa, escribímelo de nuevo. Para elegir otro comercio, escribí *OTRO COMERCIO*.""");
    }

    @Test
    @DisplayName("Si nada de lo pedido está en el stock dice qué falta y cómo seguir")
    void sinCoincidenciasShouldListWhatIsMissing() {
        assertThat(TextosPedido.sinCoincidencias(List.of(new ProductoFaltante("pañales", null)))).isEqualTo("""
                No tengo «pañales».
                Probá con otra cosa, o escribí *OTRO COMERCIO* para elegir otro comercio.""");
    }

    @Test
    @DisplayName("Un comercio sin productos pedibles lo dice y ofrece cambiar de comercio")
    void sinProductosShouldNameTheBusiness() {
        assertThat(TextosPedido.sinProductos("Kiosco Sol")).contains("*Kiosco Sol*").contains("*OTRO COMERCIO*");
    }

    @Test
    @DisplayName("Los importes salen en pesos argentinos y sin centavos cuando son cero")
    void dineroShouldUseArgentineFormat() {
        assertThat(TextosPedido.dinero(new BigDecimal("3500"))).isEqualTo("$3.500");
        assertThat(TextosPedido.dinero(new BigDecimal("3500.5"))).isEqualTo("$3.500,50");
        assertThat(TextosPedido.dinero(new BigDecimal("1234567.00"))).isEqualTo("$1.234.567");
        assertThat(TextosPedido.dinero(BigDecimal.ZERO)).isEqualTo("$0");
    }

    @Test
    @DisplayName("Las cantidades salen sin ceros de más")
    void cantidadShouldDropTrailingZeros() {
        assertThat(TextosPedido.cantidad(new BigDecimal("2.0"))).isEqualTo("2");
        assertThat(TextosPedido.cantidad(new BigDecimal("0.50"))).isEqualTo("0.5");
        assertThat(TextosPedido.cantidad(BigDecimal.valueOf(10))).isEqualTo("10");
    }

    @Test
    @DisplayName("Sami avisa al cliente cuando la orden se acepta, se rechaza o queda lista, y no cuando se prepara")
    void avisoAlClienteShouldCoverTheStepsTheClientCaresAbout() {
        OrdenCompra base = OrdenCompra.builder().total(new BigDecimal("10500")).build();

        assertThat(TextosPedido.avisoAlCliente(base.toBuilder().estado(EstadoOrdenCompra.ACEPTADA).build(), "Kiosco Sol"))
                .hasValueSatisfying(texto -> assertThat(texto).contains("*Kiosco Sol* aceptó tu pedido"));
        assertThat(TextosPedido.avisoAlCliente(base.toBuilder().estado(EstadoOrdenCompra.LISTA).build(), "Kiosco Sol"))
                .contains("¡Tu pedido en *Kiosco Sol* está listo! Ya podés pasar a retirarlo. Total: $10.500.");
        assertThat(TextosPedido.avisoAlCliente(base.toBuilder().estado(EstadoOrdenCompra.EN_PREPARACION).build(),
                "Kiosco Sol")).isEmpty();
        assertThat(TextosPedido.avisoAlCliente(base.toBuilder().estado(EstadoOrdenCompra.PENDIENTE).build(),
                "Kiosco Sol")).isEmpty();
    }

    @Test
    @DisplayName("El aviso de rechazo incluye el motivo sólo si lo hay")
    void avisoAlClienteShouldIncludeTheReasonOnlyWhenThereIsOne() {
        OrdenCompra rechazada = OrdenCompra.builder().estado(EstadoOrdenCompra.RECHAZADA).build();

        assertThat(TextosPedido.avisoAlCliente(rechazada.toBuilder().motivoRechazo("Sin stock").build(), "Kiosco Sol"))
                .hasValueSatisfying(texto -> assertThat(texto)
                        .contains("*Kiosco Sol* no pudo aceptar tu pedido. Motivo: Sin stock. Si querés"));
        assertThat(TextosPedido.avisoAlCliente(rechazada, "Kiosco Sol"))
                .hasValueSatisfying(texto -> assertThat(texto)
                        .contains("no pudo aceptar tu pedido. Si querés").doesNotContain("Motivo"));
    }

    @Test
    @DisplayName("Al elegir el comercio le dice quién lo atiende y cómo cambiar")
    void comercioElegidoShouldNameTheBusiness() {
        assertThat(TextosPedido.comercioElegido("Kiosco Sol")).contains("*Kiosco Sol*").contains("*OTRO COMERCIO*");
    }

    @Test
    @DisplayName("Sin otras marcas en ningún renglón la lista sólo ofrece confirmar o descartar")
    void listaShouldNotOfferBrandChangeWithoutAlternatives() {
        ArmadoDeLista armado = new ArmadoDeLista(List.of(
                LineaDeCompra.de(producto("Yerba Playadito 1 kg", "3500", "10"), BigDecimal.ONE, List.of())),
                List.of());

        assertThat(TextosPedido.lista("Kiosco Sol", armado))
                .contains("Respondé *SÍ* para confirmar tu pedido o *NO* para descartarlo.")
                .doesNotContain("CAMBIAR 1");
    }

    @Test
    @DisplayName("Las otras marcas del renglón salen numeradas con su precio y se puede dejar como está")
    void alternativasShouldNumberTheOtherBrands() {
        LineaDeCompra linea = LineaDeCompra.de(producto("Aceite Cocinero 900 ml", "2000", "3"), BigDecimal.ONE,
                List.of(producto("Aceite Natura 900 ml", "2400", "8"), producto("Aceite Lira 900 ml", "2200.50", "5")));

        assertThat(TextosPedido.alternativas(linea)).isEqualTo("""
                Para *Aceite Cocinero 900 ml* tengo estas otras opciones. Respondé con el número:
                1) Aceite Natura 900 ml — $2.400
                2) Aceite Lira 900 ml — $2.200,50
                O respondé NO para dejarlo como está.""");
    }

    @Test
    @DisplayName("Los avisos del cambio de marca y de la confirmación dicen lo que pasó")
    void confirmationAndBrandTextsShouldSayWhatHappened() {
        assertThat(TextosPedido.sinAlternativas("Fideos")).contains("*Fideos*").contains("otras marcas");
        assertThat(TextosPedido.opcionDeMarcaInvalida(2)).contains("entre 1 y 2").contains("como está");
        assertThat(TextosPedido.cambioDeStock(List.of("Yerba", "Aceite"))).contains("Yerba, Aceite");
        assertThat(TextosPedido.pedidoConfirmado("Kiosco Sol", new BigDecimal("9000")))
                .contains("*Kiosco Sol*").contains("$9.000").contains("listo para retirar");
    }
}
