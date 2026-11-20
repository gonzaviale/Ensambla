package com.ensambla.backend.domain.useCase.sistema;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.BloqueItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.CampoItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.SeccionItem;
import com.ensambla.backend.domain.useCase.sistema.guardarSistema.GuardarSistema;
import com.ensambla.backend.domain.useCase.sistema.guardarSistema.GuardarSistemaRequest;
import com.ensambla.backend.domain.useCase.sistema.listarSistemas.ListarSistemas;
import com.ensambla.backend.domain.useCase.sistema.listarSistemas.ListarSistemasRequest;
import com.ensambla.backend.infrastructure.config.useCase.SistemaUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static com.ensambla.backend.support.SistemasDePrueba.unSistemaCompleto;
import static com.ensambla.backend.support.SistemasDePrueba.unSistemaVacio;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Que todo negocio se abra y se guarde con la estructura fija completa y que no se la puedan quitar. */
class EstructuraFijaEnSistemasTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneOffset.UTC);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final SistemaRepository sistemaRepository = mock(SistemaRepository.class);
    private final SistemaUseCaseConfig config = new SistemaUseCaseConfig();
    private final ListarSistemas listarSistemas = config.listarSistemas(negocioRepository, sistemaRepository,
            new TransaccionDirecta(), CLOCK);
    private final GuardarSistema guardarSistema = config.guardarSistema(negocioRepository, sistemaRepository,
            mock(UsoPlanRepository.class), mock(ControlPlanService.class), new TransaccionDirecta(), CLOCK);

    private List<SistemaItem> listados;
    private SistemaItem propuesta;
    private Throwable rechazo;

    @BeforeEach
    void dadoUnNegocioDelUsuario() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Al abrir un negocio al que le falta la estructura fija se completa y se guarda")
    void alAbrirUnNegocioAlQueLeFaltaLaEstructuraFijaSeCompletaYSeGuarda() {
        dadoUnNegocioGuardadoSinNingunaSeccion();

        cuandoSeListanLosNegocios();

        entoncesSeVenLasCuatroSeccionesObligatorias();
        entoncesSeGuardaElNegocioCompleto();
    }

    @Test
    @DisplayName("Al abrir un negocio completo no se guarda nada")
    void alAbrirUnNegocioCompletoNoSeGuardaNada() {
        dadoUnNegocioGuardadoCompleto();

        cuandoSeListanLosNegocios();

        entoncesNoSeGuardaNada();
    }

    @Test
    @DisplayName("Al guardar un negocio sin secciones se le suma la estructura fija")
    void alGuardarUnNegocioSinSeccionesSeLeSumaLaEstructuraFija() {
        dadoUnaPropuestaSinSecciones();

        cuandoSeGuarda();

        entoncesSeGuardaElNegocioCompleto();
    }

    @Test
    @DisplayName("Al guardar sin la sección de Ventas de un negocio que la tenía se repone")
    void alGuardarSinLaSeccionDeVentasDeUnNegocioQueLaTeniaSeRepone() {
        dadoUnNegocioGuardadoCompletoYUnaPropuestaSinVentas();

        cuandoSeGuarda();

        entoncesSeGuardaElNegocioCompleto();
    }

    @Test
    @DisplayName("Al guardar con el precio en otro tipo de dato se rechaza")
    void alGuardarConElPrecioEnOtroTipoDeDatoSeRechaza() {
        dadoUnNegocioGuardadoCompletoYUnaPropuestaConElPrecioEnTexto();

        cuandoSeIntentaGuardar();

        entoncesSeRechazaPorque("No se puede cambiar «Precio»: Ensambla la necesita para cobrar en Caja y calcular "
                + "la ganancia. Podés cambiarle el nombre.");
        entoncesNoSeGuardaNada();
    }

    private void dadoUnNegocioGuardadoSinNingunaSeccion() {
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(unSistemaVacio()));
    }

    private void dadoUnNegocioGuardadoCompleto() {
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(unSistemaCompleto()));
    }

    private void dadoUnaPropuestaSinSecciones() {
        propuesta = SistemaItem.de(unSistemaVacio());
    }

    private void dadoUnNegocioGuardadoCompletoYUnaPropuestaSinVentas() {
        Sistema completo = unSistemaCompleto();
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(completo));
        propuesta = sinLaSeccionDeVentas(SistemaItem.de(completo));
    }

    private void dadoUnNegocioGuardadoCompletoYUnaPropuestaConElPrecioEnTexto() {
        Sistema completo = unSistemaCompleto();
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(completo));
        propuesta = conElPrecioEnTexto(SistemaItem.de(completo));
    }

    private static SistemaItem conElPrecioEnTexto(SistemaItem completo) {
        List<BloqueItem> bloques = completo.sections().get(0).blocks().stream().map(bloque -> "inventory"
                .equals(bloque.type()) ? conElPrecioEnTexto(bloque) : bloque).toList();
        SeccionItem seccion = new SeccionItem(completo.sections().get(0).id(), completo.sections().get(0).name(),
                bloques);
        return new SistemaItem(completo.id(), completo.name(), completo.createdAt(), completo.updatedAt(),
                List.of(seccion), completo.template(), completo.options(), completo.layout(), completo.demoAlmacen());
    }

    private static BloqueItem conElPrecioEnTexto(BloqueItem stock) {
        List<CampoItem> campos = stock.fields().stream().map(campo -> "price".equals(campo.key())
                ? new CampoItem("price", campo.label(), "text", false, null, null, null) : campo).toList();
        return new BloqueItem(stock.id(), stock.type(), stock.name(), stock.description(), stock.color(),
                stock.width(), campos, stock.rows(), stock.content(), stock.startRow(), stock.stockDirection());
    }

    private static SistemaItem sinLaSeccionDeVentas(SistemaItem completo) {
        List<BloqueItem> sinVentas = completo.sections().get(0).blocks().stream()
                .filter(bloque -> !"sales".equals(bloque.type())).toList();
        SeccionItem seccion = new SeccionItem(completo.sections().get(0).id(), completo.sections().get(0).name(),
                sinVentas);
        return new SistemaItem(completo.id(), completo.name(), completo.createdAt(), completo.updatedAt(),
                List.of(seccion), completo.template(), completo.options(), completo.layout(), completo.demoAlmacen());
    }

    private void cuandoSeListanLosNegocios() {
        listados = listarSistemas.execute(new ListarSistemasRequest(USUARIO_ID));
    }

    private void cuandoSeGuarda() {
        guardarSistema.execute(new GuardarSistemaRequest(USUARIO_ID, propuesta.id(), propuesta));
    }

    private void cuandoSeIntentaGuardar() {
        rechazo = catchThrowable(this::cuandoSeGuarda);
    }

    private Sistema elSistemaGuardado() {
        ArgumentCaptor<Sistema> captor = ArgumentCaptor.forClass(Sistema.class);
        verify(sistemaRepository).guardar(captor.capture(), any());
        return captor.getValue();
    }

    private void entoncesSeVenLasCuatroSeccionesObligatorias() {
        assertThat(listados.get(0).sections().get(0).blocks()).extracting(BloqueItem::type)
                .containsExactly("inventory", "clients", "sales", "expenses");
    }

    private void entoncesSeGuardaElNegocioCompleto() {
        assertThat(elSistemaGuardado().proyecto().getBloques()).extracting(Bloque::getTipo)
                .containsExactlyInAnyOrder(TipoBloque.INVENTARIO, TipoBloque.CLIENTES, TipoBloque.VENTAS,
                        TipoBloque.GASTOS);
    }

    private void entoncesNoSeGuardaNada() {
        verify(sistemaRepository, never()).guardar(any(), any());
    }

    private void entoncesSeRechazaPorque(String mensaje) {
        assertThat(rechazo).isInstanceOf(DuplicateResourceException.class).hasMessage(mensaje);
    }
}
