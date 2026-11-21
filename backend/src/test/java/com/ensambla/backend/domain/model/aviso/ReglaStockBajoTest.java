package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.support.AccionesDePrueba;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReglaStockBajoTest {

    private static final RegistroId YERBA = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final BloqueId SIN_ENTRADAS = null;

    private final ReglaStockBajo regla = new ReglaStockBajo();
    private ContextoNegocio contexto;
    private List<ResultadoRegla> avisos;

    @Test
    @DisplayName("Es la regla de los avisos de stock")
    void esLaReglaDeLosAvisosDeStock() {
        entoncesLaReglaEsDeTipo(TipoAviso.STOCK);
    }

    @Test
    @DisplayName("Un producto agotado genera un aviso urgente")
    void unProductoAgotadoGeneraUnAvisoUrgente() {
        dadoUnInventarioCon(unProducto("Yerba", 0, 5));

        cuandoSeEvaluaLaRegla();

        entoncesHayUnAvisoConPrioridad((short) 3);
        entoncesElAvisoSeTitula("Te quedaste sin Yerba");
        entoncesElAvisoDetalla("No te quedan unidades y tu mínimo es 5. Te conviene pedir 10 para no quedarte sin stock.");
        entoncesElAvisoEsSobreElRegistro(YERBA);
    }

    @Test
    @DisplayName("Un producto por debajo del mínimo genera un aviso para hoy")
    void unProductoPorDebajoDelMinimoGeneraUnAvisoParaHoy() {
        dadoUnInventarioCon(unProducto("Yerba", 3, 5));

        cuandoSeEvaluaLaRegla();

        entoncesHayUnAvisoConPrioridad((short) 2);
        entoncesElAvisoSeTitula("Yerba está por debajo del mínimo");
        entoncesElAvisoDetalla("Te quedan 3 y tu mínimo es 5. Te conviene pedir 7 para no quedarte sin stock.");
        entoncesLosMotivosSon("Definiste un mínimo de 5 y quedan 3.", "Te conviene pedir 7 para no quedarte sin stock.");
        entoncesElAvisoGuardaLaCantidadYElMinimo(new BigDecimal("3"), new BigDecimal("5"));
    }

    @Test
    @DisplayName("Un producto agotado sin mínimo avisa que se agotó y sugiere pedir diez")
    void unProductoAgotadoSinMinimoAvisaQueSeAgotoYSugierePedirDiez() {
        dadoUnInventarioCon(unProducto("Azúcar", 0, null));

        cuandoSeEvaluaLaRegla();

        entoncesElAvisoDetalla("No te quedan unidades. Te conviene pedir 10 para no quedarte sin stock.");
        entoncesElPrimerMotivoEs("No tiene un mínimo definido: te avisamos porque se agotó.");
        entoncesElAvisoGuardaElMinimo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Pide al menos una unidad y escribe los decimales con coma")
    void pideAlMenosUnaUnidadYEscribeLosDecimalesConComa() {
        dadoUnInventarioCon(unProducto("Harina", 1.5, 1.5));

        cuandoSeEvaluaLaRegla();

        entoncesElAvisoDetalla("Te quedan 1,5 y tu mínimo es 1,5. Te conviene pedir 1,5 para no quedarte sin stock.");
    }

    @Test
    @DisplayName("Un producto con stock por encima del mínimo no genera aviso")
    void unProductoConStockPorEncimaDelMinimoNoGeneraAviso() {
        dadoUnInventarioCon(unProducto("Yerba", 6, 5));

        cuandoSeEvaluaLaRegla();

        entoncesNoHayAvisos();
    }

    @Test
    @DisplayName("Un producto con stock y sin mínimo no genera aviso")
    void unProductoConStockYSinMinimoNoGeneraAviso() {
        dadoUnInventarioCon(unProducto("Yerba", 1, null));

        cuandoSeEvaluaLaRegla();

        entoncesNoHayAvisos();
    }

    @Test
    @DisplayName("Un mínimo en cero es lo mismo que no tener mínimo")
    void unMinimoEnCeroEsLoMismoQueNoTenerMinimo() {
        dadoUnInventarioCon(unProducto("Yerba", 3, 0));

        cuandoSeEvaluaLaRegla();

        entoncesNoHayAvisos();
    }

    @Test
    @DisplayName("Un producto que no cargó la cantidad no genera aviso")
    void unProductoQueNoCargoLaCantidadNoGeneraAviso() {
        dadoUnInventarioCon(unProducto("Yerba", null, 5));

        cuandoSeEvaluaLaRegla();

        entoncesNoHayAvisos();
    }

    @Test
    @DisplayName("Un producto sin nombre se nombra como un producto sin nombre")
    void unProductoSinNombreSeNombraComoUnProductoSinNombre() {
        dadoUnInventarioCon(unProducto("  ", 0, null));

        cuandoSeEvaluaLaRegla();

        entoncesElAvisoSeTitula("Te quedaste sin Un producto sin nombre");
    }

    @Test
    @DisplayName("Un producto que no cargó el nombre se nombra como un producto sin nombre")
    void unProductoQueNoCargoElNombreSeNombraComoUnProductoSinNombre() {
        dadoUnInventarioCon(unProducto(null, 0, null));

        cuandoSeEvaluaLaRegla();

        entoncesElAvisoSeTitula("Te quedaste sin Un producto sin nombre");
    }

    @Test
    @DisplayName("Con tabla de entradas la acción es registrar una entrada")
    void conTablaDeEntradasLaAccionEsRegistrarUnaEntrada() {
        dadoUnInventarioConEntradasCon(unProducto("Yerba", 0, null));

        cuandoSeEvaluaLaRegla();

        entoncesLaAccionEs(AccionesDePrueba.registrarEntrada());
    }

    @Test
    @DisplayName("Sin tabla de entradas la acción es abrir el producto")
    void sinTablaDeEntradasLaAccionEsAbrirElProducto() {
        dadoUnInventarioCon(unProducto("Yerba", 0, null));

        cuandoSeEvaluaLaRegla();

        entoncesLaAccionEs(AccionesDePrueba.abrirProducto(YERBA));
    }

    @Test
    @DisplayName("Sin tablas de inventario no hay avisos")
    void sinTablasDeInventarioNoHayAvisos() {
        dadoUnNegocioSinInventarios();

        cuandoSeEvaluaLaRegla();

        entoncesNoHayAvisos();
    }

    private static Registro unProducto(Object nombre, Object cantidad, Object minimo) {
        return AvisoTestData.registro(YERBA.valor(), nombre, cantidad, minimo);
    }

    private void dadoUnInventarioCon(Registro producto) {
        contexto = new ContextoNegocio(AvisoTestData.NEGOCIO_ID, List.of(AvisoTestData.inventario(SIN_ENTRADAS, producto)));
    }

    private void dadoUnInventarioConEntradasCon(Registro producto) {
        contexto = new ContextoNegocio(AvisoTestData.NEGOCIO_ID,
                List.of(AvisoTestData.inventario(AvisoTestData.ENTRADAS_ID, producto)));
    }

    private void dadoUnNegocioSinInventarios() {
        contexto = new ContextoNegocio(AvisoTestData.NEGOCIO_ID, List.of());
    }

    private void cuandoSeEvaluaLaRegla() {
        avisos = regla.evaluar(contexto);
    }

    private void entoncesLaReglaEsDeTipo(TipoAviso tipo) {
        assertThat(regla.tipo()).isEqualTo(tipo);
    }

    private void entoncesNoHayAvisos() {
        assertThat(avisos).isEmpty();
    }

    private ResultadoRegla elAviso() {
        assertThat(avisos).hasSize(1);
        return avisos.getFirst();
    }

    private void entoncesHayUnAvisoConPrioridad(short prioridad) {
        assertThat(elAviso().prioridad()).isEqualTo(prioridad);
    }

    private void entoncesElAvisoSeTitula(String titulo) {
        assertThat(elAviso().titulo()).isEqualTo(titulo);
    }

    private void entoncesElAvisoDetalla(String detalle) {
        assertThat(elAviso().detalle()).isEqualTo(detalle);
    }

    private void entoncesElAvisoEsSobreElRegistro(RegistroId registroId) {
        assertThat(elAviso().entidad()).isEqualTo("registro:" + registroId);
    }

    private void entoncesLosMotivosSon(String... motivos) {
        assertThat(elAviso().motivos()).containsExactly(motivos);
    }

    private void entoncesElPrimerMotivoEs(String motivo) {
        assertThat(elAviso().motivos().getFirst()).isEqualTo(motivo);
    }

    private void entoncesElAvisoGuardaLaCantidadYElMinimo(BigDecimal cantidad, BigDecimal minimo) {
        assertThat(elAviso().datos()).containsEntry("cantidad", cantidad).containsEntry("minimo", minimo);
    }

    private void entoncesElAvisoGuardaElMinimo(BigDecimal minimo) {
        assertThat(elAviso().datos()).containsEntry("minimo", minimo);
    }

    private void entoncesLaAccionEs(AccionDeAviso accion) {
        assertThat(elAviso().acciones()).containsExactly(accion);
    }
}
