package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.sistema.IdSistema;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.plan.AccesoContenidoItem;
import com.ensambla.backend.domain.useCase.plan.desbloquearContenidos.DesbloquearContenidos;
import com.ensambla.backend.domain.useCase.plan.desbloquearContenidos.DesbloquearContenidosRequest;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.BloqueItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.SeccionItem;
import com.ensambla.backend.domain.useCase.sistema.guardarSistema.GuardarSistema;
import com.ensambla.backend.domain.useCase.sistema.guardarSistema.GuardarSistemaRequest;
import com.ensambla.backend.domain.useCase.sistema.listarSistemas.ListarSistemas;
import com.ensambla.backend.domain.useCase.sistema.listarSistemas.ListarSistemasRequest;
import com.ensambla.backend.domain.useCase.zona.ZonaItem;
import com.ensambla.backend.support.LecturasInmediatasDePrueba;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.ensambla.backend.support.PruebasPremiumDePrueba;
import com.ensambla.backend.support.SuscripcionesDePrueba;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CuposContenidoPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-22T15:00:00Z");
    private static final List<String> SIETE_CLAVES = List.of("a", "b", "c", "d", "e", "f", "g");

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ControlPlanService acceso;
    @Autowired
    private SuscripcionesDePrueba suscripcion;
    @Autowired
    private PruebasPremiumDePrueba prueba;
    @Autowired
    private LecturasInmediatasDePrueba consumo;
    @Autowired
    private GuardarSistema guardarSistema;
    @Autowired
    private ListarSistemas listarSistemas;
    @Autowired
    private UsoPlanRepository uso;
    @Autowired
    private DesbloquearContenidos desbloquear;

    private UUID negocio;
    private UUID usuario;
    private AccesoContenidoItem autorizacion;
    private ZonaItem primeraZona;
    private ZonaItem segundaZona;
    private ZonaItem zonaCerca;
    private ZonaItem otraZona;
    private ZonaItem zonaSinCupo;
    private int admitidos;
    private Throwable rechazo;
    private List<?> sistemasListados;

    @BeforeEach
    void dadoUnNegocioGratuito() {
        usuario = UUID.randomUUID();
        negocio = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario(id,email,nombre) VALUES (?,?,'Cupos locales')", usuario,
                usuario + "@example.test");
        jdbc.update("INSERT INTO negocio(id,nombre,duenio_id) VALUES (?,'Cupos locales',?)", negocio, usuario);
        suscripcion.asignarGratuito(NegocioId.de(negocio), AHORA.minusDays(1));
    }

    @AfterEach
    void borrarElNegocioDePrueba() {
        jdbc.update("DELETE FROM negocio WHERE id=?", negocio);
        jdbc.update("DELETE FROM usuario WHERE id=?", usuario);
    }

    @Test
    @DisplayName("El cupo persiste: las claves ya desbloqueadas no vuelven a autorizarse")
    void elCupoPersisteLasClavesYaDesbloqueadasNoVuelvenAAutorizarse() {
        dadoQueSeDesbloquearonSieteAvisos();

        cuandoSePideDeNuevoElAvisoG(AHORA);

        entoncesNoSeAutorizaNada();
    }

    @Test
    @DisplayName("El cupo es separado por función")
    void elCupoEsSeparadoPorFuncion() {
        dadoQueSeDesbloquearonSieteAvisos();

        cuandoSePideLaClaveAEnLaZona();

        entoncesSeAutorizaSoloLaClave("a");
    }

    @Test
    @DisplayName("El cupo se renueva al cambiar el mes UTC")
    void elCupoSeRenuevaAlCambiarElMesUtc() {
        dadoQueSeDesbloquearonSieteAvisos();

        cuandoSePideDeNuevoElAvisoG(AHORA.plusMonths(1));

        entoncesSeAutorizaSoloLaClave("g");
    }

    @Test
    @DisplayName("Dos consultas del mismo cupo de zona comparten seis productos; el séptimo y el octavo no salen")
    void dosConsultasDelMismoCupoDeZonaComparten6ProductosElSeptimoYElOctavoNoSalen() {
        cuandoSeConsultanDosVecesLosProductosDeLaMismaZona();

        entoncesSeComparteElCupoDeSeisSinFiltrarProductos();
    }

    @Test
    @DisplayName("Precios, Qué se vende cerca y Otra zona gastan cada uno sus seis: una pestaña no deja sin cupo a otra")
    void preciosQueSeVendeCercaYOtraZonaGastanCadaUnoSusSeisUnaPestanaNoDejaSinCupoAOtra() {
        cuandoSeConsultanLasTresPestanasDeZona();

        entoncesCadaPestanaTieneSuPropioCupo();
    }

    @Test
    @DisplayName("La base acepta los cupos de cada pestaña de zona")
    void laBaseAceptaLosCuposDeCadaPestanaDeZona() {
        cuandoSeGuardaUnCupoDeCadaTipoDeContenido();

        entoncesLaBaseGuardoUnCupoPorTipo();
    }

    @Test
    @DisplayName("La base rechaza cualquier otro tipo de contenido")
    void laBaseRechazaCualquierOtroTipoDeContenido() {
        cuandoSeIntentaGuardarUnCupoDeTipoDesconocido();

        entoncesLaBaseRechazaElTipo();
    }

    @Test
    @DisplayName("Dos consultas concurrentes no admiten más de seis contenidos en total")
    void dosConsultasConcurrentesNoAdmitenMasDeSeisContenidosEnTotal() throws Exception {
        cuandoSeDesbloqueanAvisosConDosConsultasConcurrentes();

        entoncesSeAdmitenSeisEnTotal();
    }

    @Test
    @DisplayName("La prueba vigente permite todos los avisos")
    void laPruebaVigentePermiteTodosLosAvisos() {
        dadoUnaPruebaPremiumVigente();

        cuandoSePidenLosSieteAvisos(AHORA);

        entoncesSeAutorizanLosSiete();
    }

    @Test
    @DisplayName("Al vencer la prueba aplica el cupo gratuito")
    void alVencerLaPruebaAplicaElCupoGratuito() {
        dadoUnaPruebaPremiumVigente();

        cuandoSePidenLosSieteAvisos(AHORA.plusDays(7));

        entoncesSeAutorizanSeisYSeBloqueaUno();
    }

    @Test
    @DisplayName("El guardado completo bloquea el segundo sistema")
    void elGuardadoCompletoBloqueaElSegundoSistema() {
        dadoUnSistemaGuardado("primero", "Sistema local");

        cuandoSeIntentaGuardarOtroSistema("segundo", "Sistema local");

        entoncesSeRechazaPor("LIMITE_SISTEMAS");
    }

    @Test
    @DisplayName("El guardado completo permite editar el primero")
    void elGuardadoCompletoPermiteEditarElPrimero() {
        dadoUnSistemaGuardado("primero", "Sistema local");

        cuandoSeEditaElPrimerSistema();

        entoncesElPrimeroQuedaConSuNuevoNombreSinRegistros();
    }

    @Test
    @DisplayName("La undécima página real del gratuito se rechaza antes de llamar al proveedor")
    void laUndecimaPaginaRealDelGratuitoSeRechazaAntesDeLlamarAlProveedor() {
        dadoQueSeConsumieronLasDiezPaginasDelGratuito();

        cuandoSeIntentaConsumirLaPaginaOnce();

        entoncesSeRechazaPor("LIMITE_IA_MENSUAL");
    }

    @Test
    @DisplayName("El desbloqueo identifica el negocio exclusivamente desde el usuario de la sesión")
    void elDesbloqueoIdentificaElNegocioExclusivamenteDesdeElUsuarioDeLaSesion() {
        cuandoSeDesbloqueaDesdeLaSesionDelUsuario();

        entoncesSeAutorizanSeisYSeBloqueaUno();
    }

    @Test
    @DisplayName("El guardado completo rechaza 1.001 filas y conserva el sistema previo sin cambios parciales")
    void elGuardadoCompletoRechaza1001FilasYConservaElSistemaPrevioSinCambiosParciales() {
        dadoUnSistemaGuardado("original", "Sistema local");

        cuandoSeIntentaReemplazarPorUnSistemaDeMilUnaFilas();

        entoncesSeRechazaPorSinCambiosParciales("LIMITE_REGISTROS");
    }

    // --- dado ---

    private void dadoQueSeDesbloquearonSieteAvisos() {
        acceso.seleccionar(NegocioId.de(negocio), TipoContenidoPlan.AVISOS, SIETE_CLAVES, AHORA);
    }

    private void dadoUnaPruebaPremiumVigente() {
        prueba.iniciar(NegocioId.de(negocio), AHORA);
    }

    private void dadoUnSistemaGuardado(String id, String nombre) {
        guardar(unSistema(id, nombre, 0));
    }

    private void dadoQueSeConsumieronLasDiezPaginasDelGratuito() {
        for (int i = 0; i < 10; i++) {
            consumo.registrar(NegocioId.de(negocio), AHORA);
        }
    }

    // --- cuando ---

    private void cuandoSePideDeNuevoElAvisoG(OffsetDateTime instante) {
        autorizacion = acceso.seleccionar(NegocioId.de(negocio), TipoContenidoPlan.AVISOS, List.of("g"), instante);
    }

    private void cuandoSePideLaClaveAEnLaZona() {
        autorizacion = acceso.seleccionar(NegocioId.de(negocio), TipoContenidoPlan.ZONA, List.of("a"), AHORA);
    }

    private void cuandoSeConsultanDosVecesLosProductosDeLaMismaZona() {
        NegocioId negocioId = NegocioId.de(negocio);
        primeraZona = filtrarZona(negocioId, TipoContenidoPlan.ZONA, unaZona("Mi zona", "A", "B", "C", "D"));
        segundaZona = filtrarZona(negocioId, TipoContenidoPlan.ZONA, unaZona("Mi zona", "E", "F", "G", "H"));
    }

    private void cuandoSeConsultanLasTresPestanasDeZona() {
        NegocioId negocioId = NegocioId.de(negocio);
        filtrarZona(negocioId, TipoContenidoPlan.ZONA, unaZona("Mi zona", "A", "B", "C", "D", "E", "F"));
        zonaCerca = filtrarZona(negocioId, TipoContenidoPlan.ZONA_CERCA,
                unaZona("Mi zona", "G", "H", "I", "J", "K", "L", "M"));
        otraZona = filtrarZona(negocioId, TipoContenidoPlan.ZONA_OTRA, unaZona("Otra zona", "N", "O"));
        zonaSinCupo = filtrarZona(negocioId, TipoContenidoPlan.ZONA, unaZona("Mi zona", "Z"));
    }

    private void cuandoSeGuardaUnCupoDeCadaTipoDeContenido() {
        for (TipoContenidoPlan tipo : TipoContenidoPlan.values()) {
            insertarCupo(tipo.name());
        }
    }

    private void cuandoSeIntentaGuardarUnCupoDeTipoDesconocido() {
        rechazo = catchThrowable(() -> insertarCupo("OTRO"));
    }

    private void cuandoSeDesbloqueanAvisosConDosConsultasConcurrentes() throws Exception {
        CountDownLatch salida = new CountDownLatch(1);
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var primero = ejecutor.submit(() -> {
                salida.await();
                return acceso.seleccionar(NegocioId.de(negocio), TipoContenidoPlan.AVISOS,
                        List.of("a", "b", "c", "d"), AHORA);
            });
            var segundo = ejecutor.submit(() -> {
                salida.await();
                return acceso.seleccionar(NegocioId.de(negocio), TipoContenidoPlan.AVISOS,
                        List.of("e", "f", "g", "h"), AHORA);
            });
            salida.countDown();
            admitidos = primero.get(15, TimeUnit.SECONDS).autorizadas().size()
                    + segundo.get(15, TimeUnit.SECONDS).autorizadas().size();
        }
    }

    private void cuandoSePidenLosSieteAvisos(OffsetDateTime instante) {
        autorizacion = acceso.seleccionar(NegocioId.de(negocio), TipoContenidoPlan.AVISOS, SIETE_CLAVES, instante);
    }

    private void cuandoSeIntentaGuardarOtroSistema(String id, String nombre) {
        rechazo = catchThrowable(() -> guardar(unSistema(id, nombre, 0)));
    }

    private void cuandoSeEditaElPrimerSistema() {
        guardar(unSistema("primero", "Nombre corregido", 0));
        sistemasListados = listarSistemas.execute(new ListarSistemasRequest(UsuarioId.de(usuario)));
    }

    private void cuandoSeIntentaConsumirLaPaginaOnce() {
        rechazo = catchThrowable(() -> consumo.registrar(NegocioId.de(negocio), AHORA));
    }

    private void cuandoSeDesbloqueaDesdeLaSesionDelUsuario() {
        autorizacion = desbloquear.execute(new DesbloquearContenidosRequest(
                UsuarioId.de(usuario), "AVISOS", SIETE_CLAVES));
    }

    private void cuandoSeIntentaReemplazarPorUnSistemaDeMilUnaFilas() {
        rechazo = catchThrowable(() -> guardar(unSistema("original", "Sistema local", 1001)));
    }

    // --- entonces ---

    private void entoncesNoSeAutorizaNada() {
        assertThat(autorizacion.autorizadas()).isEmpty();
    }

    private void entoncesSeAutorizaSoloLaClave(String clave) {
        assertThat(autorizacion.autorizadas()).containsExactly(clave);
    }

    private void entoncesSeComparteElCupoDeSeisSinFiltrarProductos() {
        assertThat(primeraZona.productos()).hasSize(4);
        assertThat(segundaZona.productos()).extracting(ZonaItem.ProductoItem::nombre).containsExactly("E", "F");
        assertThat(segundaZona.acceso().bloqueadas()).isEqualTo(2);
        assertThat(segundaZona.acceso().usadas()).isEqualTo(6);
    }

    private void entoncesCadaPestanaTieneSuPropioCupo() {
        assertThat(zonaCerca.productos()).hasSize(6);
        assertThat(zonaCerca.acceso().usadas()).isEqualTo(6);
        assertThat(zonaCerca.acceso().bloqueadas()).isEqualTo(1);
        assertThat(otraZona.productos()).hasSize(2);
        assertThat(otraZona.acceso().usadas()).isEqualTo(2);
        assertThat(zonaSinCupo.productos()).isEmpty();
    }

    private void entoncesLaBaseGuardoUnCupoPorTipo() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM contenido_plan_desbloqueado WHERE negocio_id=?",
                Integer.class, negocio)).isEqualTo(TipoContenidoPlan.values().length);
    }

    private void entoncesLaBaseRechazaElTipo() {
        assertThat(rechazo).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void entoncesSeAdmitenSeisEnTotal() {
        assertThat(admitidos).isEqualTo(6);
    }

    private void entoncesSeAutorizanLosSiete() {
        assertThat(autorizacion.autorizadas()).hasSize(7);
    }

    private void entoncesSeAutorizanSeisYSeBloqueaUno() {
        assertThat(autorizacion.autorizadas()).hasSize(6);
        assertThat(autorizacion.bloqueadas()).isEqualTo(1);
    }

    private void entoncesSeRechazaPor(String codigo) {
        assertThat(rechazo).isInstanceOf(RestriccionPlanException.class).extracting("codigo").isEqualTo(codigo);
    }

    private void entoncesElPrimeroQuedaConSuNuevoNombreSinRegistros() {
        assertThat(sistemasListados).hasSize(1);
        assertThat(listarSistemas.execute(new ListarSistemasRequest(UsuarioId.de(usuario)))).singleElement()
                .satisfies(sistema -> assertThat(sistema.name()).isEqualTo("Nombre corregido"));
        assertThat(uso.contarRegistrosProyecto(NegocioId.de(negocio),
                IdSistema.aId(NegocioId.de(negocio), "primero", ProyectoId::de))).isZero();
    }

    private void entoncesSeRechazaPorSinCambiosParciales(String codigo) {
        entoncesSeRechazaPor(codigo);
        assertThat(uso.contarRegistros(NegocioId.de(negocio))).isZero();
        assertThat(listarSistemas.execute(new ListarSistemasRequest(UsuarioId.de(usuario)))).hasSize(1);
    }

    // --- datos ---

    private void insertarCupo(String tipo) {
        jdbc.update("INSERT INTO contenido_plan_desbloqueado(negocio_id,tipo,periodo,clave,creado_en) "
                + "VALUES (?,?,?,?,?)", negocio, tipo, Date.valueOf("2026-10-01"), "x", AHORA);
    }

    private void guardar(SistemaItem sistema) {
        guardarSistema.execute(new GuardarSistemaRequest(UsuarioId.de(usuario), sistema.id(), sistema));
    }

    private SistemaItem unSistema(String id, String nombre, int filas) {
        List<Map<String, Object>> filasDelBloque = IntStream.range(0, filas)
                .<Map<String, Object>>mapToObj(i -> Map.of("id", id + "-fila-" + i))
                .toList();
        BloqueItem bloque = new BloqueItem(id + "-tabla", "table", "Productos", null, "blue", "half", List.of(),
                filasDelBloque, null, null, null);
        return new SistemaItem(id, nombre, AHORA.toString(), AHORA.toString(),
                List.of(new SeccionItem(id + "-general", "Vista general", List.of(bloque))), null, null, null, false);
    }

    private ZonaItem filtrarZona(NegocioId negocioId, TipoContenidoPlan tipo, ZonaItem item) {
        return item.filtrada(acceso.seleccionar(negocioId, tipo, item.clavesDeAcceso(), AHORA));
    }

    private ZonaItem unaZona(String nombre, String... productos) {
        return new ZonaItem(nombre, 5, Arrays.stream(productos).map(this::unProducto).toList());
    }

    private ZonaItem.ProductoItem unProducto(String nombre) {
        return new ZonaItem.ProductoItem(nombre, 5, BigDecimal.ONE, BigDecimal.TEN,
                BigDecimal.TEN, 5, BigDecimal.ONE);
    }
}
