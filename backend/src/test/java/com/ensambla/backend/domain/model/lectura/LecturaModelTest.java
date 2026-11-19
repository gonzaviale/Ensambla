package com.ensambla.backend.domain.model.lectura;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LecturaModelTest {

    private static final double UMBRAL = 0.8;
    private static final List<CampoPropuesto> CAMPOS =
            List.of(new CampoPropuesto("monto", "Monto", TipoCampo.NUMERO, List.of()));
    private static final BloqueId BLOQUE_EXISTENTE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");

    private FilaLeida fila;
    private BloqueConocido bloqueConocido;
    private CampoContexto campoContexto;

    @Test
    @DisplayName("Un dato es dudoso cuando la confianza no llega al umbral")
    void unDatoEsDudosoCuandoLaConfianzaNoLlegaAlUmbral() {
        entoncesElDatoConConfianzaEsDudoso(0.79, true);
    }

    @Test
    @DisplayName("Un dato no es dudoso cuando la confianza alcanza el umbral")
    void unDatoNoEsDudosoCuandoLaConfianzaAlcanzaElUmbral() {
        entoncesElDatoConConfianzaEsDudoso(0.8, false);
    }

    @Test
    @DisplayName("Una fila tiene dudas si algún dato está por debajo del umbral")
    void unaFilaTieneDudasSiAlgunDatoEstaPorDebajoDelUmbral() {
        dadoUnaFila(Map.of("cliente", new DatoLeido("Rosa", 0.6), "monto", new DatoLeido("8000", 0.95)));

        entoncesLaFilaTieneDudas(true);
    }

    @Test
    @DisplayName("Una fila no tiene dudas si todos sus datos son confiables")
    void unaFilaNoTieneDudasSiTodosSusDatosSonConfiables() {
        dadoUnaFila(Map.of("monto", new DatoLeido("8000", 0.95)));

        entoncesLaFilaTieneDudas(false);
    }

    @Test
    @DisplayName("La fila copia sus datos: cambiar el mapa original no la altera")
    void laFilaCopiaSusDatosCambiarElMapaOriginalNoLaAltera() {
        Map<String, DatoLeido> datos = new HashMap<>(Map.of("monto", new DatoLeido("8000", 0.95)));
        dadoUnaFila(datos);

        cuandoSeVaciaElMapaOriginal(datos);

        entoncesLaFilaConservaElMonto();
    }

    @Test
    @DisplayName("Expone el bloque existente cuando la IA sugiere sumar las filas a uno del negocio")
    void exponeElBloqueExistenteCuandoLaIaSugiereSumarLasFilasAUnoDelNegocio() {
        entoncesElBloquePropuestoExpone(BLOQUE_EXISTENTE_ID);
    }

    @Test
    @DisplayName("No expone bloque existente cuando la IA propone uno nuevo")
    void noExponeBloqueExistenteCuandoLaIaProponeUnoNuevo() {
        entoncesElBloquePropuestoNoExponeNinguno();
    }

    @Test
    @DisplayName("El bloque conocido copia la lista de campos")
    void elBloqueConocidoCopiaLaListaDeCampos() {
        CampoContexto fecha = new CampoContexto("Fecha", TipoCampo.FECHA, List.of());
        List<CampoContexto> campos = new ArrayList<>(List.of(fecha));
        dadoUnBloqueConocidoConLosCampos(campos);

        cuandoSeAgregaUnCampoALaListaOriginal(campos);

        entoncesElBloqueConocidoSoloTiene(fecha);
    }

    @Test
    @DisplayName("El campo de contexto copia la lista de opciones")
    void elCampoDeContextoCopiaLaListaDeOpciones() {
        List<String> opciones = new ArrayList<>(List.of("Pagado"));
        dadoUnCampoDeContextoConLasOpciones(opciones);

        cuandoSeAgregaUnaOpcionALaListaOriginal(opciones);

        entoncesElCampoDeContextoSoloTieneLaOpcion("Pagado");
    }

    // --- dado ---
    private void dadoUnaFila(Map<String, DatoLeido> datos) {
        fila = new FilaLeida(1, 1, datos);
    }

    private void dadoUnBloqueConocidoConLosCampos(List<CampoContexto> campos) {
        bloqueConocido = new BloqueConocido(BloqueId.nuevo(), "Ventas", campos);
    }

    private void dadoUnCampoDeContextoConLasOpciones(List<String> opciones) {
        campoContexto = new CampoContexto("Estado", TipoCampo.OPCION, opciones);
    }

    // --- cuando ---
    private void cuandoSeVaciaElMapaOriginal(Map<String, DatoLeido> datos) {
        datos.clear();
    }

    private void cuandoSeAgregaUnCampoALaListaOriginal(List<CampoContexto> campos) {
        campos.add(new CampoContexto("Monto", TipoCampo.NUMERO, List.of()));
    }

    private void cuandoSeAgregaUnaOpcionALaListaOriginal(List<String> opciones) {
        opciones.add("Debe");
    }

    // --- entonces ---
    private void entoncesElDatoConConfianzaEsDudoso(double confianza, boolean esperado) {
        assertThat(new DatoLeido("Marta", confianza).esDudoso(UMBRAL)).isEqualTo(esperado);
    }

    private void entoncesLaFilaTieneDudas(boolean esperado) {
        assertThat(fila.tieneDudas(UMBRAL)).isEqualTo(esperado);
    }

    private void entoncesLaFilaConservaElMonto() {
        assertThat(fila.datos()).containsKey("monto");
    }

    private void entoncesElBloquePropuestoExpone(BloqueId id) {
        assertThat(new BloquePropuesto("Ventas", id, CAMPOS).bloqueExistente()).contains(id);
    }

    private void entoncesElBloquePropuestoNoExponeNinguno() {
        assertThat(new BloquePropuesto("Ventas", null, CAMPOS).bloqueExistente()).isEmpty();
    }

    private void entoncesElBloqueConocidoSoloTiene(CampoContexto campo) {
        assertThat(bloqueConocido.campos()).containsExactly(campo);
    }

    private void entoncesElCampoDeContextoSoloTieneLaOpcion(String opcion) {
        assertThat(campoContexto.opciones()).containsExactly(opcion);
    }
}
