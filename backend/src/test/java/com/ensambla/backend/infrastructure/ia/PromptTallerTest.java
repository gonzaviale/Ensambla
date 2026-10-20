package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.model.sami.taller.MensajeTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.service.sami.SamiPruebas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromptTallerTest {

    private static final KitsRubro KITS = SamiPruebas.kitsReales();
    private static final EstadoTaller VACIO = new EstadoTaller("", "mosaic", List.of());
    private static final EstadoTaller CON_PIEZAS = new EstadoTaller("Mi almacén", "mosaic", List.of(
            new PiezaTaller("e1", "inventory", "Inventario", null,
                    List.of(new CampoTaller("Producto", "text"), new CampoTaller("Cantidad", "number"))),
            new PiezaTaller("e2", "clients", "Clientes", "Aparte", List.of()),
            new PiezaTaller("e3", "sales", "Ventas", null,
                    List.of(new CampoTaller("Producto", "relation"), new CampoTaller("Cantidad", "number")))));

    private final PromptTaller prompt = new PromptTaller(
            new ClassPathResource("ia/prompt-taller.md"), SamiPruebas.catalogoReal(), KITS);

    @Test
    @DisplayName("El contexto lista las piezas con su id, tipo, sección y datos, y termina con el mensaje")
    void peticionShouldListPiecesAndEndWithTheMessage() {
        PeticionConHerramientas peticion = prompt.peticion(new ConsultaTaller("sumá gastos", CON_PIEZAS, List.of()));

        assertThat(peticion.contexto())
                .startsWith("Sistema actual: «Mi almacén» (distribución: mosaic)\n")
                .contains("- id e1, tipo inventory, «Inventario»: Producto, Cantidad\n")
                .contains("- id e2, tipo clients, «Clientes» sección «Aparte»: \n")
                .contains("- id e3, tipo sales, «Ventas»: Producto (vínculo), Cantidad\n")
                .endsWith("Mensaje de la persona: sumá gastos")
                .doesNotContain("Conversación reciente")
                .doesNotContain("Kit");
        assertThat(peticion.instrucciones()).contains("Sos Sami").contains("ofrecer_opciones").contains("usar_kit");
    }

    @Test
    @DisplayName("Las instrucciones terminan con los componentes del catálogo y sus datos de fábrica")
    void peticionShouldListCatalogComponentsInInstructions() {
        PeticionConHerramientas peticion = prompt.peticion(new ConsultaTaller("hola", VACIO, List.of()));

        assertThat(peticion.instrucciones())
                .contains("\nComponentes (tipo Nombre): inventory Inventario; orders Pedidos;")
                .contains("; summary Resumen; form Formulario: Nombre, Detalle, Fecha; note Nota; ")
                .endsWith("; exits Salidas.");
    }

    @Test
    @DisplayName("Un sistema sin piezas lo dice")
    void peticionShouldSayWhenThereAreNoPieces() {
        PeticionConHerramientas peticion = prompt.peticion(new ConsultaTaller("hola", VACIO, List.of()));

        assertThat(peticion.contexto()).contains(PromptTaller.SIN_PIEZAS);
    }

    @Test
    @DisplayName("Sin rubro reconocido, lista los kits disponibles con su id y su nombre")
    void peticionShouldListAvailableKitsWithoutKit() {
        PeticionConHerramientas peticion = prompt.peticion(new ConsultaTaller("hola", VACIO, List.of()));

        assertThat(peticion.contexto())
                .contains("Kits por rubro (usar_kit): almacen Almacén y kiosco, dietetica Dietética, velas Velas")
                .contains("libreria Librería y papelería.\n")
                .doesNotContain(PromptTaller.MARCA_KIT);
    }

    @Test
    @DisplayName("Con rubro reconocido, resume su kit: piezas, tipo, datos extra y conexiones")
    void peticionShouldSummarizeDetectedKit() {
        PeticionConHerramientas peticion = prompt.peticion(new ConsultaTaller(
                "vendo velas", VACIO, List.of(), KITS.buscar("velas").orElseThrow()));

        assertThat(peticion.contexto())
                .contains("Kit del rubro: velas (Velas y aromas). Piezas: Velas (inventory +Aroma, Cera, Tamaño); "
                        + "Materiales (form +Cantidad, Unidad, Costo); Ventas (sales +Canal); Quién me debe (clients +Debe)."
                        + " Conexiones: Velas→Materiales.\n")
                .doesNotContain("Kits por rubro");
    }

    @Test
    @DisplayName("Con piezas ya armadas no nombra el kit ni la lista de kits: un pedido puntual no arma el kit")
    void peticionShouldNotMentionKitsWhenSystemHasPieces() {
        PeticionConHerramientas conKit = prompt.peticion(new ConsultaTaller(
                "llamalo Velas de Ana", CON_PIEZAS, List.of(), KITS.buscar("velas").orElseThrow()));
        PeticionConHerramientas sinKit = prompt.peticion(new ConsultaTaller("sumá un dato", CON_PIEZAS, List.of()));

        assertThat(List.of(conKit.contexto(), sinKit.contexto())).allSatisfy(contexto -> assertThat(contexto)
                .doesNotContain(PromptTaller.MARCA_KIT)
                .doesNotContain("Kits por rubro"));
    }

    @Test
    @DisplayName("Un kit sin conexiones ni datos extra se resume sin esas partes")
    void peticionShouldSummarizeKitWithoutLinksOrExtras() {
        PeticionConHerramientas peticion = prompt.peticion(new ConsultaTaller(
                "dietética", VACIO, List.of(), KITS.buscar("dietetica").orElseThrow()));

        assertThat(peticion.contexto())
                .contains("Gastos (expenses).\n")
                .doesNotContain("Conexiones");
    }

    @Test
    @DisplayName("La charla reciente va con quién dijo cada cosa")
    void peticionShouldIncludeRecentConversation() {
        PeticionConHerramientas peticion = prompt.peticion(new ConsultaTaller(
                "dale", new EstadoTaller("S", "mosaic", List.of()),
                List.of(new MensajeTaller("sami", "¿Sumamos gastos?"), new MensajeTaller("you", "tal vez"))));

        assertThat(peticion.contexto()).contains("Conversación reciente:\nSami: ¿Sumamos gastos?\nPersona: tal vez\n");
    }

    @Test
    @DisplayName("Si falta el recurso del prompt la aplicación no levanta")
    void constructorShouldFailWhenResourceIsMissing() {
        assertThatThrownBy(() -> new PromptTaller(
                new ClassPathResource("ia/no-existe.md"), SamiPruebas.catalogoReal(), KITS))
                .isInstanceOf(IllegalStateException.class);
        assertThat(new PromptTaller(new ByteArrayResource("Sos Sami\n".getBytes()), SamiPruebas.catalogoReal(), KITS)
                .peticion(new ConsultaTaller("x", VACIO, List.of())).instrucciones())
                .startsWith("Sos Sami\nComponentes");
    }

    @Test
    @DisplayName("La versión del prompt es la del taller que ensambla")
    void versionShouldBeV2() {
        assertThat(PromptTaller.VERSION).isEqualTo("v2");
    }
}
