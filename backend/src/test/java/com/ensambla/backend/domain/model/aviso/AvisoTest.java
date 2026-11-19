package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class AvisoTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final ProyectoId PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000001");
    private static final RegistroId PRODUCTO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final String ENTIDAD_DE_PRODUCTO = "registro:" + PRODUCTO_ID;
    private static final String ENTIDAD_DE_ZONA = "zona:yerba";
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");
    private static final OffsetDateTime DESPUES = OffsetDateTime.parse("2026-10-19T13:00:00Z");

    private Aviso aviso;
    private ResultadoRegla resultado;
    private String huellaOriginal;
    private Throwable rechazo;

    @Test
    @DisplayName("Un aviso nuevo queda pendiente con el grupo de su tipo y los datos de la regla")
    void unAvisoNuevoQuedaPendienteConElGrupoDeSuTipoYLosDatosDeLaRegla() {
        dadoUnResultadoDeLaRegla((short) 3, "Te quedaste sin yerba");

        cuandoSeCreaUnAvisoDeStock();

        entoncesQuedaPendienteEnElGrupoDeStock();
        entoncesCopiaLosDatosDeLaRegla();
    }

    @Test
    @DisplayName("Es urgente desde la prioridad 2")
    void esUrgenteDesdeLaPrioridadDos() {
        dadoUnAvisoPendienteDePrioridad((short) 1);

        entoncesSoloSonUrgentesLasPrioridadesDosYTres();
    }

    @Test
    @DisplayName("Aplicar un aviso pendiente guarda cuándo se resolvió")
    void aplicarUnAvisoPendienteGuardaCuandoSeResolvio() {
        dadoUnAvisoPendienteDePrioridad((short) 2);

        cuandoSeAplica();

        entoncesQuedaResueltoComo(EstadoAviso.APLICADO);
        entoncesSeActualizoEnElMomentoDeResolver();
    }

    @Test
    @DisplayName("Descartar un aviso pendiente guarda cuándo se resolvió")
    void descartarUnAvisoPendienteGuardaCuandoSeResolvio() {
        dadoUnAvisoPendienteDePrioridad((short) 2);

        cuandoSeDescarta();

        entoncesQuedaResueltoComo(EstadoAviso.DESCARTADO);
    }

    @Test
    @DisplayName("No se puede aplicar un aviso que ya fue resuelto")
    void noSePuedeAplicarUnAvisoQueYaFueResuelto() {
        dadoUnAvisoDescartado();

        cuandoSeIntentaAplicar();

        entoncesSeRechazaPorqueElAvisoYaFueResuelto();
    }

    @Test
    @DisplayName("No se puede descartar un aviso que ya fue resuelto")
    void noSePuedeDescartarUnAvisoQueYaFueResuelto() {
        dadoUnAvisoDescartado();

        cuandoSeIntentaDescartar();

        entoncesSeRechazaPorqueElAvisoYaFueResuelto();
    }

    @Test
    @DisplayName("Un aviso que el motor ya no detecta vence sin fecha de resolución")
    void unAvisoQueElMotorYaNoDetectaVenceSinFechaDeResolucion() {
        dadoUnAvisoPendienteDePrioridad((short) 2);

        cuandoVence();

        entoncesQuedaVencidoSinFechaDeResolucion();
    }

    @Test
    @DisplayName("Un aviso vencido no vuelve a vencer")
    void unAvisoVencidoNoVuelveAVencer() {
        dadoUnAvisoVencido();

        cuandoSeIntentaVencer();

        entoncesSeRechazaConUnaReglaDeNegocio();
    }

    @Test
    @DisplayName("Actualizar un aviso pendiente cambia sus datos y mantiene el estado")
    void actualizarUnAvisoPendienteCambiaSusDatosYMantieneElEstado() {
        dadoUnAvisoPendienteDePrioridad((short) 2);

        cuandoSeActualizaConUnResultadoDePrioridad((short) 3, "Te quedaste sin yerba");

        entoncesTieneLosDatosNuevos();
        entoncesSigueEnEstado(EstadoAviso.PENDIENTE);
    }

    @Test
    @DisplayName("Actualizar un aviso vencido lo deja pendiente otra vez")
    void actualizarUnAvisoVencidoLoDejaPendienteOtraVez() {
        dadoUnAvisoVencido();

        cuandoSeActualizaConUnResultadoDePrioridad((short) 2, "Yerba por debajo del mínimo");

        entoncesSigueEnEstado(EstadoAviso.PENDIENTE);
        entoncesNoTieneFechaDeResolucion();
    }

    @Test
    @DisplayName("No se puede actualizar un aviso aplicado")
    void noSePuedeActualizarUnAvisoAplicado() {
        dadoUnAvisoAplicadoDePrioridad((short) 2);

        cuandoSeIntentaActualizar();

        entoncesSeRechazaConUnaReglaDeNegocio();
    }

    @Test
    @DisplayName("No se puede actualizar un aviso descartado")
    void noSePuedeActualizarUnAvisoDescartado() {
        dadoUnAvisoDescartado();

        cuandoSeIntentaActualizar();

        entoncesSeRechazaConUnaReglaDeNegocio();
    }

    @Test
    @DisplayName("Un aviso aplicado de estancado sobre un producto es medible")
    void unAvisoAplicadoDeEstancadoSobreUnProductoEsMedible() {
        dadoUnAvisoAplicadoDe(TipoAviso.ESTANCADO, ENTIDAD_DE_PRODUCTO);

        entoncesEsMedible();
    }

    @Test
    @DisplayName("Un aviso aplicado de stock sobre un producto es medible")
    void unAvisoAplicadoDeStockSobreUnProductoEsMedible() {
        dadoUnAvisoAplicadoDe(TipoAviso.STOCK, ENTIDAD_DE_PRODUCTO);

        entoncesEsMedible();
    }

    @Test
    @DisplayName("Un aviso deja de ser medible cuando ya se midió")
    void unAvisoDejaDeSerMedibleCuandoYaSeMidio() {
        dadoUnAvisoAplicadoDe(TipoAviso.ESTANCADO, ENTIDAD_DE_PRODUCTO);

        cuandoSeRegistraElImpacto();

        entoncesNoEsMedible();
    }

    @Test
    @DisplayName("Un aviso pendiente no es medible")
    void unAvisoPendienteNoEsMedible() {
        dadoUnAvisoPendienteDePrioridad((short) 2);

        entoncesNoEsMedible();
    }

    @Test
    @DisplayName("Un aviso sin fecha de resolución no es medible")
    void unAvisoSinFechaDeResolucionNoEsMedible() {
        dadoUnAvisoAplicadoSinFechaDeResolucion();

        entoncesNoEsMedible();
    }

    @Test
    @DisplayName("Un aviso de combo no es medible")
    void unAvisoDeComboNoEsMedible() {
        dadoUnAvisoAplicadoDe(TipoAviso.COMBO, ENTIDAD_DE_PRODUCTO);

        entoncesNoEsMedible();
    }

    @Test
    @DisplayName("Un aviso que no apunta a un producto no es medible")
    void unAvisoQueNoApuntaAUnProductoNoEsMedible() {
        dadoUnAvisoAplicadoDe(TipoAviso.STOCK, ENTIDAD_DE_ZONA);

        entoncesNoEsMedible();
    }

    @Test
    @DisplayName("El producto del aviso sale de su entidad")
    void elProductoDelAvisoSaleDeSuEntidad() {
        dadoUnAvisoAplicadoDe(TipoAviso.STOCK, ENTIDAD_DE_PRODUCTO);

        entoncesSuProductoEsElDeLaEntidad();
    }

    @Test
    @DisplayName("Un aviso sobre una zona no tiene producto")
    void unAvisoSobreUnaZonaNoTieneProducto() {
        dadoUnAvisoAplicadoDe(TipoAviso.STOCK, ENTIDAD_DE_ZONA);

        entoncesNoTieneProducto();
    }

    @Test
    @DisplayName("Registrar el impacto lo suma a los datos sin perder los de origen ni cambiar la huella")
    void registrarElImpactoLoSumaALosDatosSinPerderLosDeOrigenNiCambiarLaHuella() {
        dadoUnAvisoAplicadoDe(TipoAviso.ESTANCADO, ENTIDAD_DE_PRODUCTO);

        cuandoSeRegistraElImpacto();

        entoncesConservaLosDatosDeOrigenYSumaElImpacto();
        entoncesConservaLaHuellaYElEstadoAplicado();
    }

    @Test
    @DisplayName("Registrar el impacto en un aviso sin datos crea los datos")
    void registrarElImpactoEnUnAvisoSinDatosCreaLosDatos() {
        dadoUnAvisoSinDatos();

        cuandoSeRegistraElImpacto();

        entoncesElImpactoQuedaRegistrado();
    }

    // --- dado ---

    private void dadoUnResultadoDeLaRegla(short prioridad, String titulo) {
        resultado = unResultado(prioridad, titulo);
    }

    private void dadoUnAvisoPendienteDePrioridad(short prioridad) {
        aviso = unAvisoDePrioridad(prioridad);
    }

    private void dadoUnAvisoDescartado() {
        dadoUnAvisoPendienteDePrioridad((short) 2);
        aviso.descartar(DESPUES);
    }

    private void dadoUnAvisoVencido() {
        dadoUnAvisoPendienteDePrioridad((short) 2);
        aviso.vencer(AHORA);
    }

    private void dadoUnAvisoAplicadoDePrioridad(short prioridad) {
        dadoUnAvisoPendienteDePrioridad(prioridad);
        aviso.aplicar(AHORA);
    }

    private void dadoUnAvisoAplicadoDe(TipoAviso tipo, String entidad) {
        aviso = unAvisoAplicado(tipo, entidad);
        huellaOriginal = aviso.getHuella();
    }

    private void dadoUnAvisoAplicadoSinFechaDeResolucion() {
        dadoUnAvisoAplicadoDe(TipoAviso.STOCK, ENTIDAD_DE_PRODUCTO);
        aviso.setResueltoEn(null);
    }

    private void dadoUnAvisoSinDatos() {
        aviso = Aviso.builder().build();
    }

    // --- cuando ---

    private void cuandoSeCreaUnAvisoDeStock() {
        aviso = Aviso.nuevo(NEGOCIO_ID, TipoAviso.STOCK, resultado, AHORA);
    }

    private void cuandoSeAplica() {
        aviso.aplicar(DESPUES);
    }

    private void cuandoSeDescarta() {
        aviso.descartar(DESPUES);
    }

    private void cuandoSeIntentaAplicar() {
        rechazo = catchThrowable(() -> aviso.aplicar(DESPUES));
    }

    private void cuandoSeIntentaDescartar() {
        rechazo = catchThrowable(() -> aviso.descartar(DESPUES));
    }

    private void cuandoVence() {
        aviso.vencer(DESPUES);
    }

    private void cuandoSeIntentaVencer() {
        rechazo = catchThrowable(() -> aviso.vencer(DESPUES));
    }

    private void cuandoSeActualizaConUnResultadoDePrioridad(short prioridad, String titulo) {
        aviso.actualizarCon(unResultado(prioridad, titulo), DESPUES);
    }

    private void cuandoSeIntentaActualizar() {
        ResultadoRegla nuevo = unResultado((short) 2, "Yerba");
        rechazo = catchThrowable(() -> aviso.actualizarCon(nuevo, DESPUES));
    }

    private void cuandoSeRegistraElImpacto() {
        aviso.registrarImpacto(unImpacto(), DESPUES);
    }

    // --- entonces ---

    private void entoncesQuedaPendienteEnElGrupoDeStock() {
        assertThat(aviso.getEstado()).isEqualTo(EstadoAviso.PENDIENTE);
        assertThat(aviso.getGrupo()).isEqualTo(GrupoAviso.STOCK);
        assertThat(aviso.estaPendiente()).isTrue();
    }

    private void entoncesCopiaLosDatosDeLaRegla() {
        assertThat(aviso.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(aviso.getProyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(aviso.getTitulo()).isEqualTo("Te quedaste sin yerba");
        assertThat(aviso.getHuella()).isEqualTo(resultado.huellaPara(TipoAviso.STOCK));
        assertThat(aviso.getCreadoEn()).isEqualTo(AHORA);
        assertThat(aviso.getActualizadoEn()).isEqualTo(AHORA);
    }

    private void entoncesSoloSonUrgentesLasPrioridadesDosYTres() {
        assertThat(unAvisoDePrioridad((short) 3).esUrgente()).isTrue();
        assertThat(unAvisoDePrioridad((short) 2).esUrgente()).isTrue();
        assertThat(aviso.esUrgente()).isFalse();
    }

    private void entoncesQuedaResueltoComo(EstadoAviso estado) {
        assertThat(aviso.getEstado()).isEqualTo(estado);
        assertThat(aviso.getResueltoEn()).isEqualTo(DESPUES);
    }

    private void entoncesSeActualizoEnElMomentoDeResolver() {
        assertThat(aviso.getActualizadoEn()).isEqualTo(DESPUES);
    }

    private void entoncesSeRechazaPorqueElAvisoYaFueResuelto() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessage("El aviso ya fue resuelto");
    }

    private void entoncesSeRechazaConUnaReglaDeNegocio() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class);
    }

    private void entoncesQuedaVencidoSinFechaDeResolucion() {
        assertThat(aviso.getEstado()).isEqualTo(EstadoAviso.VENCIDO);
        assertThat(aviso.getResueltoEn()).isNull();
        assertThat(aviso.getActualizadoEn()).isEqualTo(DESPUES);
    }

    private void entoncesTieneLosDatosNuevos() {
        assertThat(aviso.getPrioridad()).isEqualTo((short) 3);
        assertThat(aviso.getTitulo()).isEqualTo("Te quedaste sin yerba");
        assertThat(aviso.getActualizadoEn()).isEqualTo(DESPUES);
    }

    private void entoncesSigueEnEstado(EstadoAviso estado) {
        assertThat(aviso.getEstado()).isEqualTo(estado);
    }

    private void entoncesNoTieneFechaDeResolucion() {
        assertThat(aviso.getResueltoEn()).isNull();
    }

    private void entoncesEsMedible() {
        assertThat(aviso.esMedible()).isTrue();
    }

    private void entoncesNoEsMedible() {
        assertThat(aviso.esMedible()).isFalse();
    }

    private void entoncesSuProductoEsElDeLaEntidad() {
        assertThat(aviso.productoId()).contains(PRODUCTO_ID);
    }

    private void entoncesNoTieneProducto() {
        assertThat(aviso.productoId()).isEmpty();
    }

    private void entoncesConservaLosDatosDeOrigenYSumaElImpacto() {
        assertThat(aviso.getDatos()).containsEntry("cantidad", 2).containsKey("impacto");
        assertThat(aviso.impacto()).contains(unImpacto());
    }

    private void entoncesConservaLaHuellaYElEstadoAplicado() {
        assertThat(aviso.getHuella()).isEqualTo(huellaOriginal);
        assertThat(aviso.getEstado()).isEqualTo(EstadoAviso.APLICADO);
        assertThat(aviso.getActualizadoEn()).isEqualTo(DESPUES);
    }

    private void entoncesElImpactoQuedaRegistrado() {
        assertThat(aviso.impacto()).contains(unImpacto());
    }

    // --- datos ---

    private static Aviso unAvisoAplicado(TipoAviso tipo, String entidad) {
        Aviso aviso = Aviso.nuevo(NEGOCIO_ID, tipo,
                new ResultadoRegla(PROYECTO_ID, entidad, (short) 2, "Titulo", "Detalle", Map.of("cantidad", 2),
                        List.of(), List.of()), AHORA);
        aviso.aplicar(DESPUES);
        return aviso;
    }

    private static Aviso unAvisoDePrioridad(short prioridad) {
        return Aviso.nuevo(NEGOCIO_ID, TipoAviso.STOCK, unResultado(prioridad, "Yerba por debajo del mínimo"), AHORA);
    }

    private static ResultadoRegla unResultado(short prioridad, String titulo) {
        return new ResultadoRegla(PROYECTO_ID, "registro:1", prioridad, titulo, "Te quedan 2",
                Map.of("cantidad", 2), List.of("motivo"), List.of());
    }

    private static ImpactoAviso unImpacto() {
        return new ImpactoAviso(14, new BigDecimal("1.00"), new BigDecimal("2.00"),
                new BigDecimal("100.00"), new BigDecimal("250.00"));
    }
}
