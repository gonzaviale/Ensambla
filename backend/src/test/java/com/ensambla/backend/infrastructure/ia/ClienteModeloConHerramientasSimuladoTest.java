package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.ImagenTaller;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.support.SamiPruebas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClienteModeloConHerramientasSimuladoTest {

    private static final KitsRubro KITS = SamiPruebas.kitsReales();
    private static final EstadoTaller VACIO = new EstadoTaller("Mi sistema", "mosaic", List.of());
    private static final EstadoTaller CON_PIEZAS = new EstadoTaller("Mi sistema", "mosaic", List.of(
            new PiezaTaller("e1", "inventory", "Velas", null, List.of())));

    private final ClienteModeloConHerramientasSimulado cliente = new ClienteModeloConHerramientasSimulado(KITS);
    private final PromptTaller prompt = new PromptTaller(
            new ClassPathResource("ia/prompt-taller.md"), SamiPruebas.catalogoReal(), KITS);

    private RespuestaConHerramientas respuesta;
    private RespuestaConHerramientas respuestaSinFoto;

    @Test
    @DisplayName("Si la persona nombra gastos, suma la pieza de gastos")
    void siLaPersonaNombraGastosSumaLaPiezaDeGastos() {
        cuandoSeCompletaElContexto("Mensaje de la persona: sumá los gastos");

        entoncesSeSumaLaPiezaDeGastosDelProveedorSimulado();
    }

    @Test
    @DisplayName("Ignora la foto adjunta: contesta lo mismo que sin ella")
    void ignoraLaFotoAdjuntaContestaLoMismoQueSinElla() {
        cuandoSeConsultaConYSinFotoSobreUnSistemaConPiezas();

        entoncesContestaLoMismoConYSinFoto();
    }

    @Test
    @DisplayName("Sin rubro reconocido pregunta de qué es el negocio ofreciendo los primeros cuatro kits")
    void sinRubroReconocidoPreguntaDeQueEsElNegocioOfreciendoLosPrimerosCuatroKits() {
        cuandoSeConsultaUnSistemaVacioSinKit();

        entoncesPreguntaElRubroConLosPrimerosCuatroKits();
    }

    @Test
    @DisplayName("Con el kit del rubro y el sistema vacío lo arma y hace la pregunta del kit")
    void conElKitDelRubroYElSistemaVacioLoArmaYHaceLaPreguntaDelKit() {
        cuandoSeConsultaUnSistemaVacioConElKitDeVelas();

        entoncesArmaElKitDeVelasYHaceSuPregunta();
    }

    @Test
    @DisplayName("Con un sistema con piezas no arma nada: pregunta qué ajustar")
    void conUnSistemaConPiezasNoArmaNadaPreguntaQueAjustar() {
        cuandoSeConsultaUnSistemaConPiezasConElKitDeVelas();

        entoncesSoloPreguntaQueAjustar();
    }

    @Test
    @DisplayName("Si el kit que nombra el contexto no existe, pregunta el rubro")
    void siElKitQueNombraElContextoNoExistePreguntaElRubro() {
        cuandoSeCompletaElContexto(
                "Todavía no tiene piezas.\n\nKit del rubro: astronautas (Naves).\n\nMensaje de la persona: hola");

        entoncesLaPrimeraLlamadaPreguntaElRubro();
    }

    @Test
    @DisplayName("Sólo mira el mensaje de la persona para los gastos, no el resto del contexto")
    void soloMiraElMensajeDeLaPersonaParaLosGastosNoElRestoDelContexto() {
        cuandoSeCompletaElContexto(
                "- id e3, tipo expenses, «Gastos»: Detalle (text)\n\nMensaje de la persona: hola");

        entoncesLaPrimeraLlamadaEs("ofrecer_opciones");
    }

    @Test
    @DisplayName("Sin la marca del mensaje mira todo el contexto")
    void sinLaMarcaDelMensajeMiraTodoElContexto() {
        cuandoSeCompletaElContexto("quiero gastos");

        entoncesLaPrimeraLlamadaEs("agregar_pieza");
    }

    // --- cuando ---

    private void cuandoSeCompletaElContexto(String contexto) {
        respuesta = cliente.completar(new PeticionConHerramientas("instrucciones", contexto));
    }

    private void cuandoSeConsultaConYSinFotoSobreUnSistemaConPiezas() {
        ImagenTaller foto = new ImagenTaller("image/png", new byte[]{1});
        respuesta = cliente.completar(prompt.peticion(new ConsultaTaller("hola", CON_PIEZAS, List.of(), null, foto)));
        respuestaSinFoto = cliente.completar(prompt.peticion(new ConsultaTaller("hola", CON_PIEZAS, List.of())));
    }

    private void cuandoSeConsultaUnSistemaVacioSinKit() {
        respuesta = cliente.completar(prompt.peticion(new ConsultaTaller("hola", VACIO, List.of())));
    }

    private void cuandoSeConsultaUnSistemaVacioConElKitDeVelas() {
        respuesta = cliente.completar(prompt.peticion(
                new ConsultaTaller("vendo velas", VACIO, List.of(), KITS.buscar("velas").orElseThrow())));
    }

    private void cuandoSeConsultaUnSistemaConPiezasConElKitDeVelas() {
        respuesta = cliente.completar(prompt.peticion(
                new ConsultaTaller("dale", CON_PIEZAS, List.of(), KITS.buscar("velas").orElseThrow())));
    }

    // --- entonces ---

    private void entoncesSeSumaLaPiezaDeGastosDelProveedorSimulado() {
        assertThat(respuesta.llamadas()).hasSize(1);
        assertThat(respuesta.llamadas().getFirst().nombre()).isEqualTo("agregar_pieza");
        assertThat(respuesta.llamadas().getFirst().argumentos()).containsEntry("tipo", "expenses");
        assertThat(respuesta.proveedor()).isEqualTo("simulado");
        assertThat(respuesta.tokensEntrada()).isZero();
    }

    private void entoncesContestaLoMismoConYSinFoto() {
        assertThat(respuesta).isEqualTo(respuestaSinFoto);
    }

    private void entoncesPreguntaElRubroConLosPrimerosCuatroKits() {
        assertThat(respuesta.llamadas()).hasSize(1);
        assertThat(respuesta.llamadas().getFirst().nombre()).isEqualTo("ofrecer_opciones");
        assertThat(respuesta.llamadas().getFirst().argumentos()).isEqualTo(Map.of(
                "pregunta", ClienteModeloConHerramientasSimulado.PREGUNTA_RUBRO,
                "opciones", List.of("Almacén y kiosco", "Dietética", "Velas y aromas", "Panadería")));
    }

    private void entoncesArmaElKitDeVelasYHaceSuPregunta() {
        assertThat(respuesta.llamadas()).extracting(llamada -> llamada.nombre())
                .containsExactly("usar_kit", "ofrecer_opciones");
        assertThat(respuesta.llamadas().getFirst().argumentos()).isEqualTo(Map.of("rubro", "velas"));
        assertThat(respuesta.llamadas().get(1).argumentos()).containsEntry(
                "pregunta", "¿Querés que te avise cuando te quedan pocos materiales?");
        assertThat(respuesta.texto()).isEqualTo(
                "Te armé una base para velas y aromas: Velas, Materiales, Ventas, Quién me debe. Mirá cómo queda.");
    }

    private void entoncesSoloPreguntaQueAjustar() {
        assertThat(respuesta.llamadas()).hasSize(1);
        assertThat(respuesta.llamadas().getFirst().argumentos()).isEqualTo(Map.of(
                "pregunta", ClienteModeloConHerramientasSimulado.PREGUNTA_AJUSTE,
                "opciones", ClienteModeloConHerramientasSimulado.OPCIONES_AJUSTE));
    }

    private void entoncesLaPrimeraLlamadaPreguntaElRubro() {
        assertThat(respuesta.llamadas().getFirst().argumentos())
                .containsEntry("pregunta", ClienteModeloConHerramientasSimulado.PREGUNTA_RUBRO);
    }

    private void entoncesLaPrimeraLlamadaEs(String herramienta) {
        assertThat(respuesta.llamadas().getFirst().nombre()).isEqualTo(herramienta);
    }
}
