package com.ensambla.backend.domain.useCase.aviso.ejecutarMotorAvisos;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.ContextoNegocio;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.aviso.ReglaAviso;
import com.ensambla.backend.domain.model.aviso.ResultadoRegla;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.precio.CambioPrecio;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.precio.HistorialPrecioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.support.AvisoTestData;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EjecutarMotorAvisosTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final NegocioId OTRO_NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000002");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");
    private static final OffsetDateTime ANTES = OffsetDateTime.parse("2026-10-19T11:00:00Z");
    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final ZonaId ZONA_ID = ZonaId.de("e0000000-0000-4000-8000-000000000001");

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private RegistroRepository registroRepository;

    @Mock
    private ZonaRepository zonaRepository;

    @Mock
    private AgregadoZonaRepository agregadoZonaRepository;

    @Mock
    private HistorialPrecioRepository historialPrecioRepository;

    @Mock
    private AvisoRepository avisoRepository;

    @Mock
    private ReglaAviso regla;

    private EjecutarMotorAvisos ejecutarMotorAvisos;

    private final ContextoNegocio contexto = contextoVacio(NEGOCIO_ID);

    private ResultadoRegla resultado;
    private ResultadoRegla resultadoNuevo;
    private Aviso avisoExistente;
    private Aviso otroAviso;
    private ContextoNegocio contextoArmado;
    private Throwable fallo;
    private Registro producto;
    private Registro venta;
    private ReglaAviso otraRegla;

    @BeforeEach
    void setUp() {
        ejecutarMotorAvisos = motor(List.of(regla));
        lenient().when(negocioRepository.findAllIds()).thenReturn(List.of(NEGOCIO_ID));
        lenient().when(proyectoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of());
        lenient().when(regla.tipo()).thenReturn(TipoAviso.STOCK);
        lenient().when(avisoRepository.save(any(Aviso.class))).thenAnswer(invocacion -> {
            Aviso aviso = invocacion.getArgument(0);
            if (aviso.getId() == null) {
                aviso.setId(AvisoId.nuevo());
            }
            return aviso;
        });
    }

    @Test
    @DisplayName("Crea el aviso cuando la regla detecta algo nuevo")
    void creaElAvisoCuandoLaReglaDetectaAlgoNuevo() {
        dadoQueLaReglaDetectaUnaEntidadNueva();

        cuandoSeEjecutaElMotor();

        entoncesSeCreaUnAvisoPendienteConLaHuellaDelResultado();
    }

    @Test
    @DisplayName("No duplica un aviso pendiente que la regla vuelve a detectar con los mismos datos")
    void noDuplicaUnAvisoPendienteQueLaReglaVuelveADetectarConLosMismosDatos() {
        dadoUnAvisoPendienteQueLaReglaVuelveADetectar();

        cuandoSeEjecutaElMotor();

        entoncesSeGuardaElMismoAvisoPendiente();
    }

    @Test
    @DisplayName("No vuelve a crear un aviso descartado mientras sus datos de origen no cambien")
    void noVuelveACrearUnAvisoDescartadoMientrasSusDatosDeOrigenNoCambien() {
        dadoUnAvisoYaResueltoConLosMismosDatos(EstadoAviso.DESCARTADO);

        cuandoSeEjecutaElMotor();

        entoncesNoSeGuardaNingunAviso();
    }

    @Test
    @DisplayName("No vuelve a crear un aviso aplicado con los mismos datos")
    void noVuelveACrearUnAvisoAplicadoConLosMismosDatos() {
        dadoUnAvisoYaResueltoConLosMismosDatos(EstadoAviso.APLICADO);

        cuandoSeEjecutaElMotor();

        entoncesNoSeGuardaNingunAviso();
    }

    @Test
    @DisplayName("Un aviso descartado reaparece cuando cambian los datos que lo originaron")
    void unAvisoDescartadoReapareceCuandoCambianLosDatosQueLoOriginaron() {
        dadoUnAvisoDescartadoYUnaReglaQueAhoraDetectaOtrosDatos();

        cuandoSeEjecutaElMotor();

        entoncesSeCreaUnAvisoPendienteConLosDatosNuevos();
    }

    @Test
    @DisplayName("Si cambian los datos de un aviso pendiente, lo actualiza en lugar de crear otro")
    void siCambianLosDatosDeUnAvisoPendienteLoActualizaEnLugarDeCrearOtro() {
        dadoUnAvisoPendienteCuyosDatosCambiaronEnLaRegla();

        cuandoSeEjecutaElMotor();

        entoncesSeActualizaElAvisoPendienteConLosDatosNuevos();
    }

    @Test
    @DisplayName("Reactiva un aviso vencido cuando la regla vuelve a detectarlo y vence el pendiente de la entidad")
    void reactivaUnAvisoVencidoCuandoLaReglaVuelveADetectarloYVenceElPendienteDeLaEntidad() {
        dadoUnAvisoVencidoQueLaReglaVuelveADetectarYOtroPendienteDeLaMismaEntidad();

        cuandoSeEjecutaElMotor();

        entoncesSeVenceElPendienteYSeReactivaElVencido();
    }

    @Test
    @DisplayName("Vence el aviso pendiente de una entidad que la regla ya no detecta")
    void venceElAvisoPendienteDeUnaEntidadQueLaReglaYaNoDetecta() {
        dadoUnAvisoPendienteQueLaReglaYaNoDetecta();

        cuandoSeEjecutaElMotor();

        entoncesSeGuardaElAvisoPendienteComoVencido();
    }

    @Test
    @DisplayName("Vence el pendiente de una entidad cuyo aviso ya fue descartado con los datos actuales")
    void venceElPendienteDeUnaEntidadCuyoAvisoYaFueDescartadoConLosDatosActuales() {
        dadoUnPendienteViejoYUnDescartadoConLosDatosActuales();

        cuandoSeEjecutaElMotor();

        entoncesSeGuardaElPendienteViejoComoVencido();
    }

    @Test
    @DisplayName("Ejecuta todas las reglas registradas")
    void ejecutaTodasLasReglasRegistradas() {
        dadoUnMotorConDosReglas();

        cuandoSeEjecutaElMotor();

        entoncesSeEvaluoTambienLaSegundaRegla();
    }

    @Test
    @DisplayName("Corre las reglas sobre cada negocio con el instante del reloj")
    void correLasReglasSobreCadaNegocioConElInstanteDelReloj() {
        dadoDosNegociosSinAvisos();

        cuandoSeEjecutaElMotor();

        entoncesSeEvaluoLaReglaEnCadaNegocio();
    }

    @Test
    @DisplayName("Un negocio con error no frena a los demás")
    void unNegocioConErrorNoFrenaALosDemas() {
        dadoUnNegocioQueFallaAlCargarYOtroSano();

        cuandoSeIntentaEjecutarElMotor();

        entoncesNoSePropagaElErrorYSeEvaluaElOtroNegocio();
    }

    @Test
    @DisplayName("Arma las tablas de inventario con sus campos por clave y sus registros")
    void armaLasTablasDeInventarioConSusCamposPorClaveYSusRegistros() {
        Registro registro = dadoUnInventarioConUnProducto();

        cuandoSeEjecutaElMotorYSeCapturaElContexto();

        entoncesElContextoTieneElInventarioConSusCamposYRegistros(registro);
    }

    @Test
    @DisplayName("Ubica la tabla de entradas del proyecto y deja afuera las tablas que no son de inventario")
    void ubicaLaTablaDeEntradasDelProyectoYDejaAfueraLasTablasQueNoSonDeInventario() {
        dadoUnProyectoConInventarioYEntradas();

        cuandoSeEjecutaElMotorYSeCapturaElContexto();

        entoncesElInventarioConoceLaTablaDeEntradas();
    }

    @Test
    @DisplayName("Ignora los campos sin clave semántica")
    void ignoraLosCamposSinClaveSemantica() {
        dadoUnInventarioConUnCampoSinClave();

        cuandoSeEjecutaElMotorYSeCapturaElContexto();

        entoncesElInventarioSoloTieneElCampoConClave();
    }

    @Test
    @DisplayName("Salta el proyecto que desapareció entre la lista y la carga")
    void saltaElProyectoQueDesaparecioEntreLaListaYLaCarga() {
        dadoUnProyectoQueDesaparecioEntreLaListaYLaCarga();

        cuandoSeEjecutaElMotorYSeCapturaElContexto();

        entoncesElContextoNoTieneInventarios();
    }

    @Test
    @DisplayName("Arma las tablas de ventas con sus campos por clave y sus registros, y deja afuera las demás")
    void armaLasTablasDeVentasConSusCamposPorClaveYSusRegistrosYDejaAfueraLasDemas() {
        Registro venta = dadoUnInventarioYUnaTablaDeVentasConUnaVenta();

        cuandoSeEjecutaElMotorYSeCapturaElContexto();

        entoncesElContextoTieneElInventarioYLasVentasConSusCamposYRegistros(venta);
    }

    @Test
    @DisplayName("Las filas de ejemplo de la plantilla no llegan a las reglas, ni en el inventario ni en las ventas")
    void lasFilasDeEjemploDeLaPlantillaNoLleganALasReglasNiEnElInventarioNiEnLasVentas() {
        dadoFilasRealesYDeEjemploEnInventarioYVentas();

        cuandoSeEjecutaElMotorYSeCapturaElContexto();

        entoncesLasReglasSoloRecibenLasFilasReales();
    }

    @Test
    @DisplayName("Le pasa a las reglas los agregados de la zona del negocio")
    void lePasaALasReglasLosAgregadosDeLaZonaDelNegocio() {
        AnalisisZona analisis = dadoUnNegocioConZona();

        cuandoSeEjecutaElMotorYSeCapturaElContexto();

        entoncesLasReglasRecibenElAnalisisDeLaZona(analisis);
    }

    @Test
    @DisplayName("Sin zona asignada, las reglas reciben una zona vacía")
    void sinZonaAsignadaLasReglasRecibenUnaZonaVacia() {
        dadoUnNegocioSinZona();

        cuandoSeEjecutaElMotorYSeCapturaElContexto();

        entoncesLasReglasRecibenUnaZonaVacia();
    }

    @Test
    @DisplayName("Le pasa a las reglas los cambios de precio del último período, desde la medianoche argentina")
    void lePasaALasReglasLosCambiosDePrecioDelUltimoPeriodoDesdeLaMedianocheArgentina() {
        CambioPrecio cambio = dadoUnCambioDePrecioEnElUltimoPeriodo();

        cuandoSeEjecutaElMotorYSeCapturaElContexto();

        entoncesLasReglasRecibenElCambioDePrecio(cambio);
    }

    @Test
    @DisplayName("El día de hoy es el de la hora argentina, aunque en UTC ya sea el día siguiente")
    void elDiaDeHoyEsElDeLaHoraArgentinaAunqueEnUtcYaSeaElDiaSiguiente() {
        dadoUnRelojQueEnUtcYaMarcaElDiaSiguiente();

        cuandoSeEjecutaElMotorYSeCapturaElContexto();

        entoncesHoyEsElDiaArgentino();
    }

    // --- dado ---

    private void dadoQueLaReglaDetecta(ResultadoRegla... resultados) {
        when(regla.evaluar(contexto)).thenReturn(List.of(resultados));
    }

    private void dadoQueNoHayPendientes() {
        when(avisoRepository.findByNegocioIdAndTipoAndEstado(NEGOCIO_ID, TipoAviso.STOCK, EstadoAviso.PENDIENTE))
                .thenReturn(List.of());
    }

    private void dadoQueNoHayAvisosPrevios() {
        dadoQueNoHayPendientes();
        lenient().when(avisoRepository.findByNegocioIdAndHuella(any(), any())).thenReturn(Optional.empty());
    }

    private void dadoQueHayPendientes(Aviso... pendientes) {
        when(avisoRepository.findByNegocioIdAndTipoAndEstado(NEGOCIO_ID, TipoAviso.STOCK, EstadoAviso.PENDIENTE))
                .thenReturn(new ArrayList<>(List.of(pendientes)));
    }

    private void dadoQueLaHuellaDeEsteResultadoCorrespondeA(ResultadoRegla datos, Aviso aviso) {
        when(avisoRepository.findByNegocioIdAndHuella(NEGOCIO_ID, datos.huellaPara(TipoAviso.STOCK)))
                .thenReturn(Optional.of(aviso));
    }

    private void dadoQueLaHuellaDeEsteResultadoNoExiste(ResultadoRegla datos) {
        when(avisoRepository.findByNegocioIdAndHuella(NEGOCIO_ID, datos.huellaPara(TipoAviso.STOCK)))
                .thenReturn(Optional.empty());
    }

    private void dadoQueLaReglaDetectaUnaEntidadNueva() {
        resultado = unResultado("registro:1", 2);
        dadoQueLaReglaDetecta(resultado);
        dadoQueNoHayAvisosPrevios();
    }

    private void dadoUnAvisoPendienteQueLaReglaVuelveADetectar() {
        resultado = unResultado("registro:1", 2);
        avisoExistente = unAvisoDe(resultado, EstadoAviso.PENDIENTE);
        dadoQueLaReglaDetecta(resultado);
        dadoQueHayPendientes(avisoExistente);
        dadoQueLaHuellaDeEsteResultadoCorrespondeA(resultado, avisoExistente);
    }

    private void dadoUnAvisoYaResueltoConLosMismosDatos(EstadoAviso estado) {
        resultado = unResultado("registro:1", 2);
        avisoExistente = unAvisoDe(resultado, estado);
        dadoQueLaReglaDetecta(resultado);
        dadoQueNoHayPendientes();
        dadoQueLaHuellaDeEsteResultadoCorrespondeA(resultado, avisoExistente);
    }

    private void dadoUnAvisoDescartadoYUnaReglaQueAhoraDetectaOtrosDatos() {
        unAvisoDe(unResultado("registro:1", 2, 4), EstadoAviso.DESCARTADO);
        resultadoNuevo = unResultado("registro:1", 3, 0);
        dadoQueLaReglaDetecta(resultadoNuevo);
        dadoQueNoHayAvisosPrevios();
    }

    private void dadoUnAvisoPendienteCuyosDatosCambiaronEnLaRegla() {
        avisoExistente = unAvisoDe(unResultado("registro:1", 2, 4), EstadoAviso.PENDIENTE);
        resultadoNuevo = unResultado("registro:1", 3, 0);
        dadoQueLaReglaDetecta(resultadoNuevo);
        dadoQueHayPendientes(avisoExistente);
        dadoQueLaHuellaDeEsteResultadoNoExiste(resultadoNuevo);
    }

    private void dadoUnAvisoVencidoQueLaReglaVuelveADetectarYOtroPendienteDeLaMismaEntidad() {
        resultado = unResultado("registro:1", 2, 4);
        avisoExistente = unAvisoDe(resultado, EstadoAviso.VENCIDO);
        otroAviso = unAvisoDe(unResultado("registro:1", 2, 3), EstadoAviso.PENDIENTE);
        dadoQueLaReglaDetecta(resultado);
        dadoQueHayPendientes(otroAviso);
        dadoQueLaHuellaDeEsteResultadoCorrespondeA(resultado, avisoExistente);
    }

    private void dadoUnAvisoPendienteQueLaReglaYaNoDetecta() {
        avisoExistente = unAvisoDe(unResultado("registro:1", 2), EstadoAviso.PENDIENTE);
        dadoQueLaReglaDetecta();
        dadoQueHayPendientes(avisoExistente);
    }

    private void dadoUnPendienteViejoYUnDescartadoConLosDatosActuales() {
        resultado = unResultado("registro:1", 2, 4);
        avisoExistente = unAvisoDe(resultado, EstadoAviso.DESCARTADO);
        otroAviso = unAvisoDe(unResultado("registro:1", 2, 9), EstadoAviso.PENDIENTE);
        dadoQueLaReglaDetecta(resultado);
        dadoQueHayPendientes(otroAviso);
        dadoQueLaHuellaDeEsteResultadoCorrespondeA(resultado, avisoExistente);
    }

    private void dadoUnMotorConDosReglas() {
        ReglaAviso otra = mock(ReglaAviso.class);
        when(otra.tipo()).thenReturn(TipoAviso.PRECIO_ZONA);
        when(otra.evaluar(contexto)).thenReturn(List.of());
        ejecutarMotorAvisos = motor(List.of(regla, otra));
        otraRegla = otra;
        dadoQueLaReglaDetecta();
        dadoQueNoHayAvisosPrevios();
    }

    private void dadoDosNegociosSinAvisos() {
        when(negocioRepository.findAllIds()).thenReturn(List.of(NEGOCIO_ID, OTRO_NEGOCIO_ID));
        when(proyectoRepository.findAllByNegocioId(OTRO_NEGOCIO_ID)).thenReturn(List.of());
        dadoQueNoHayPendientes();
        dadoQueNoHayPendientesEn(OTRO_NEGOCIO_ID);
        when(regla.evaluar(any())).thenReturn(List.of());
    }

    private void dadoUnNegocioQueFallaAlCargarYOtroSano() {
        when(negocioRepository.findAllIds()).thenReturn(List.of(NEGOCIO_ID, OTRO_NEGOCIO_ID));
        when(proyectoRepository.findAllByNegocioId(NEGOCIO_ID)).thenThrow(new IllegalStateException("Base caída"));
        when(proyectoRepository.findAllByNegocioId(OTRO_NEGOCIO_ID)).thenReturn(List.of());
        when(regla.evaluar(any())).thenReturn(List.of());
        dadoQueNoHayPendientesEn(OTRO_NEGOCIO_ID);
    }

    private void dadoQueNoHayPendientesEn(NegocioId negocioId) {
        when(avisoRepository.findByNegocioIdAndTipoAndEstado(negocioId, TipoAviso.STOCK, EstadoAviso.PENDIENTE))
                .thenReturn(List.of());
    }

    private Registro dadoUnInventarioConUnProducto() {
        Registro registro = AvisoTestData.registro(UUID.randomUUID(), "Yerba", 2, 5);
        dadoUnProyectoConBloques(AvisoTestData.bloqueInventario());
        dadoRegistrosDeInventario(registro);
        return registro;
    }

    private void dadoUnProyectoConInventarioYEntradas() {
        Bloque entradas = Bloque.builder().id(AvisoTestData.ENTRADAS_ID).negocioId(NEGOCIO_ID)
                .proyectoId(AvisoTestData.PROYECTO_ID).tipo(TipoBloque.ENTRADAS).campos(List.of()).build();
        dadoUnProyectoConBloques(AvisoTestData.bloqueInventario(), entradas);
        dadoRegistrosDeInventario();
    }

    private void dadoUnInventarioConUnCampoSinClave() {
        Bloque bloque = AvisoTestData.bloqueInventario();
        bloque.setCampos(List.of(AvisoTestData.campo(AvisoTestData.CAMPO_NOMBRE_ID.valor(), "name"),
                Campo.builder().id(CampoId.nuevo()).clave(null).build()));
        dadoUnProyectoConBloques(bloque);
        dadoRegistrosDeInventario();
    }

    private void dadoUnProyectoQueDesaparecioEntreLaListaYLaCarga() {
        when(proyectoRepository.findAllByNegocioId(NEGOCIO_ID))
                .thenReturn(List.of(Proyecto.builder().id(AvisoTestData.PROYECTO_ID).negocioId(NEGOCIO_ID).build()));
        when(proyectoRepository.findByIdAndNegocioId(AvisoTestData.PROYECTO_ID, NEGOCIO_ID))
                .thenReturn(Optional.empty());
    }

    private Registro dadoUnInventarioYUnaTablaDeVentasConUnaVenta() {
        Registro venta = AvisoTestData.venta("producto", 1, 100, "2026-10-18", null);
        dadoUnProyectoConBloques(AvisoTestData.bloqueInventario(), AvisoTestData.bloqueVentas());
        dadoRegistrosDeInventario();
        dadoRegistrosDeVentas(venta);
        return venta;
    }

    private void dadoFilasRealesYDeEjemploEnInventarioYVentas() {
        producto = AvisoTestData.registro(UUID.randomUUID(), "Yerba", 2, 5);
        Registro productoDeEjemplo = AvisoTestData.registro(UUID.randomUUID(), "Fideos", 0, 5);
        productoDeEjemplo.setEsEjemplo(true);
        venta = AvisoTestData.venta("producto", 1, 100, "2026-10-18", null);
        Registro ventaDeEjemplo = AvisoTestData.venta("producto", 9, 100, "2026-10-18", null);
        ventaDeEjemplo.setEsEjemplo(true);
        dadoUnProyectoConBloques(AvisoTestData.bloqueInventario(), AvisoTestData.bloqueVentas());
        dadoRegistrosDeInventario(producto, productoDeEjemplo);
        dadoRegistrosDeVentas(venta, ventaDeEjemplo);
    }

    private AnalisisZona dadoUnNegocioConZona() {
        Zona zona = Zona.builder().id(ZONA_ID).nombre("San Justo").build();
        AnalisisZona analisis = new AnalisisZona("San Justo", 8, List.of());
        when(negocioRepository.findZonaIdById(NEGOCIO_ID)).thenReturn(Optional.of(ZONA_ID));
        when(zonaRepository.findById(ZONA_ID)).thenReturn(Optional.of(zona));
        when(agregadoZonaRepository.analizar(zona)).thenReturn(analisis);
        return analisis;
    }

    private void dadoUnNegocioSinZona() {
        when(negocioRepository.findZonaIdById(NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private CambioPrecio dadoUnCambioDePrecioEnElUltimoPeriodo() {
        CambioPrecio cambio = new CambioPrecio(NEGOCIO_ID, RegistroId.nuevo(), new BigDecimal("1000"),
                new BigDecimal("1200"), ANTES);
        OffsetDateTime desde = OffsetDateTime.parse("2026-09-19T03:00:00Z");
        when(historialPrecioRepository.findByNegocioIdDesde(NEGOCIO_ID, desde)).thenReturn(List.of(cambio));
        return cambio;
    }

    private void dadoUnRelojQueEnUtcYaMarcaElDiaSiguiente() {
        ejecutarMotorAvisos = motor(List.of(regla), OffsetDateTime.parse("2026-10-20T01:00:00Z"));
    }

    private void dadoUnProyectoConBloques(Bloque... bloques) {
        Proyecto resumen = Proyecto.builder().id(AvisoTestData.PROYECTO_ID).negocioId(NEGOCIO_ID).build();
        Proyecto completo = Proyecto.builder().id(AvisoTestData.PROYECTO_ID).negocioId(NEGOCIO_ID)
                .bloques(List.of(bloques)).build();
        when(proyectoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(resumen));
        when(proyectoRepository.findByIdAndNegocioId(AvisoTestData.PROYECTO_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(completo));
    }

    private void dadoRegistrosDeInventario(Registro... registros) {
        when(registroRepository.findAllByBloqueIdAndNegocioId(AvisoTestData.INVENTARIO_ID, NEGOCIO_ID))
                .thenReturn(List.of(registros));
    }

    private void dadoRegistrosDeVentas(Registro... registros) {
        when(registroRepository.findAllByBloqueIdAndNegocioId(AvisoTestData.VENTAS_ID, NEGOCIO_ID))
                .thenReturn(List.of(registros));
    }

    // --- cuando ---

    private void cuandoSeEjecutaElMotor() {
        ejecutarMotorAvisos.execute();
    }

    private void cuandoSeIntentaEjecutarElMotor() {
        fallo = catchThrowable(ejecutarMotorAvisos::execute);
    }

    private void cuandoSeEjecutaElMotorYSeCapturaElContexto() {
        when(regla.evaluar(any())).thenReturn(List.of());
        dadoQueNoHayPendientes();
        ejecutarMotorAvisos.execute();
        ArgumentCaptor<ContextoNegocio> captor = ArgumentCaptor.forClass(ContextoNegocio.class);
        verify(regla).evaluar(captor.capture());
        contextoArmado = captor.getValue();
    }

    // --- entonces: avisos ---

    private void entoncesSeCreaUnAvisoPendienteConLaHuellaDelResultado() {
        Aviso creado = elAvisoGuardado();
        assertThat(creado.getEstado()).isEqualTo(EstadoAviso.PENDIENTE);
        assertThat(creado.getEntidad()).isEqualTo("registro:1");
        assertThat(creado.getHuella()).isEqualTo(resultado.huellaPara(TipoAviso.STOCK));
    }

    private void entoncesSeGuardaElMismoAvisoPendiente() {
        Aviso guardado = elAvisoGuardado();
        assertThat(guardado.getId()).isEqualTo(avisoExistente.getId());
        assertThat(guardado.getEstado()).isEqualTo(EstadoAviso.PENDIENTE);
        verify(avisoRepository).save(any(Aviso.class));
    }

    private void entoncesNoSeGuardaNingunAviso() {
        verify(avisoRepository, never()).save(any(Aviso.class));
    }

    private void entoncesSeCreaUnAvisoPendienteConLosDatosNuevos() {
        Aviso creado = elAvisoGuardado();
        assertThat(creado.getEstado()).isEqualTo(EstadoAviso.PENDIENTE);
        assertThat(creado.getHuella()).isEqualTo(resultadoNuevo.huellaPara(TipoAviso.STOCK));
    }

    private void entoncesSeActualizaElAvisoPendienteConLosDatosNuevos() {
        Aviso guardado = elAvisoGuardado();
        assertThat(guardado.getId()).isEqualTo(avisoExistente.getId());
        assertThat(guardado.getPrioridad()).isEqualTo((short) 3);
        assertThat(guardado.getHuella()).isEqualTo(resultadoNuevo.huellaPara(TipoAviso.STOCK));
    }

    private void entoncesSeVenceElPendienteYSeReactivaElVencido() {
        ArgumentCaptor<Aviso> guardados = ArgumentCaptor.forClass(Aviso.class);
        verify(avisoRepository, times(2)).save(guardados.capture());
        assertThat(guardados.getAllValues()).extracting(Aviso::getId, Aviso::getEstado)
                .containsExactly(
                        tuple(otroAviso.getId(), EstadoAviso.VENCIDO),
                        tuple(avisoExistente.getId(), EstadoAviso.PENDIENTE));
    }

    private void entoncesSeGuardaElAvisoPendienteComoVencido() {
        Aviso guardado = elAvisoGuardado();
        assertThat(guardado.getId()).isEqualTo(avisoExistente.getId());
        assertThat(guardado.getEstado()).isEqualTo(EstadoAviso.VENCIDO);
    }

    private void entoncesSeGuardaElPendienteViejoComoVencido() {
        Aviso guardado = elAvisoGuardado();
        assertThat(guardado.getId()).isEqualTo(otroAviso.getId());
        assertThat(guardado.getEstado()).isEqualTo(EstadoAviso.VENCIDO);
    }

    // --- entonces: reglas y negocios ---

    private void entoncesSeEvaluoTambienLaSegundaRegla() {
        verify(otraRegla).evaluar(contexto);
        verify(avisoRepository).findByNegocioIdAndTipoAndEstado(NEGOCIO_ID, TipoAviso.PRECIO_ZONA,
                EstadoAviso.PENDIENTE);
    }

    private void entoncesSeEvaluoLaReglaEnCadaNegocio() {
        verify(regla).evaluar(contexto);
        verify(regla).evaluar(contextoVacio(OTRO_NEGOCIO_ID));
    }

    private void entoncesNoSePropagaElErrorYSeEvaluaElOtroNegocio() {
        assertThat(fallo).isNull();
        verify(regla).evaluar(contextoVacio(OTRO_NEGOCIO_ID));
    }

    // --- entonces: contexto de las reglas ---

    private void entoncesElContextoTieneElInventarioConSusCamposYRegistros(Registro registro) {
        assertThat(contextoArmado.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(contextoArmado.inventarios()).singleElement().satisfies(inventario -> {
            assertThat(inventario.bloque().getId()).isEqualTo(AvisoTestData.INVENTARIO_ID);
            assertThat(inventario.camposPorClave()).containsOnlyKeys("name", "quantity", "minimum");
            assertThat(inventario.registros()).containsExactly(registro);
            assertThat(inventario.entradasId()).isNull();
        });
    }

    private void entoncesElInventarioConoceLaTablaDeEntradas() {
        assertThat(contextoArmado.inventarios()).extracting(BloqueInventario::entradasId)
                .containsExactly(AvisoTestData.ENTRADAS_ID);
    }

    private void entoncesElInventarioSoloTieneElCampoConClave() {
        assertThat(contextoArmado.inventarios().getFirst().camposPorClave()).containsOnlyKeys("name");
    }

    private void entoncesElContextoNoTieneInventarios() {
        assertThat(contextoArmado.inventarios()).isEmpty();
    }

    private void entoncesElContextoTieneElInventarioYLasVentasConSusCamposYRegistros(Registro venta) {
        assertThat(contextoArmado.inventarios()).singleElement().satisfies(
                inventario -> assertThat(inventario.bloque().getId()).isEqualTo(AvisoTestData.INVENTARIO_ID));
        assertThat(contextoArmado.ventas()).singleElement().satisfies(ventas -> {
            assertThat(ventas.bloque().getId()).isEqualTo(AvisoTestData.VENTAS_ID);
            assertThat(ventas.camposPorClave()).containsOnlyKeys("product", "quantity", "price", "date", "customer");
            assertThat(ventas.registros()).containsExactly(venta);
        });
    }

    private void entoncesLasReglasSoloRecibenLasFilasReales() {
        assertThat(contextoArmado.inventarios().getFirst().registros()).containsExactly(producto);
        assertThat(contextoArmado.ventas().getFirst().registros()).containsExactly(venta);
    }

    private void entoncesLasReglasRecibenElAnalisisDeLaZona(AnalisisZona analisis) {
        assertThat(contextoArmado.zona()).isEqualTo(analisis);
    }

    private void entoncesLasReglasRecibenUnaZonaVacia() {
        assertThat(contextoArmado.zona()).isEqualTo(AnalisisZona.sinZona());
    }

    private void entoncesLasReglasRecibenElCambioDePrecio(CambioPrecio cambio) {
        assertThat(contextoArmado.cambiosDePrecio()).containsExactly(cambio);
    }

    private void entoncesHoyEsElDiaArgentino() {
        assertThat(contextoArmado.hoy()).isEqualTo(HOY);
    }

    // --- datos ---

    private EjecutarMotorAvisos motor(List<ReglaAviso> reglas) {
        return motor(reglas, AHORA);
    }

    private EjecutarMotorAvisos motor(List<ReglaAviso> reglas, OffsetDateTime ahora) {
        return new EjecutarMotorAvisos(negocioRepository, reglas, proyectoRepository, registroRepository,
                zonaRepository, agregadoZonaRepository, historialPrecioRepository,
                new MetricasVentas(30, new BigDecimal("0.5")), avisoRepository, new TransaccionDirecta(),
                Clock.fixed(ahora.toInstant(), ZoneOffset.UTC));
    }

    private static ContextoNegocio contextoVacio(NegocioId negocioId) {
        return new ContextoNegocio(negocioId, HOY, List.of(), List.of(), AnalisisZona.sinZona());
    }

    private Aviso elAvisoGuardado() {
        ArgumentCaptor<Aviso> captor = ArgumentCaptor.forClass(Aviso.class);
        verify(avisoRepository).save(captor.capture());
        return captor.getValue();
    }

    private static ResultadoRegla unResultado(String entidad, int prioridad) {
        return unResultado(entidad, prioridad, 4);
    }

    private static ResultadoRegla unResultado(String entidad, int prioridad, int cantidad) {
        return new ResultadoRegla(null, entidad, (short) prioridad, "titulo", "detalle",
                Map.of("cantidad", cantidad), List.of(), List.of());
    }

    private static Aviso unAvisoDe(ResultadoRegla datos, EstadoAviso estado) {
        Aviso aviso = Aviso.nuevo(NEGOCIO_ID, TipoAviso.STOCK, datos, ANTES);
        aviso.setId(AvisoId.nuevo());
        aviso.setEstado(estado);
        if (estado == EstadoAviso.DESCARTADO || estado == EstadoAviso.APLICADO) {
            aviso.setResueltoEn(ANTES);
        }
        return aviso;
    }
}
