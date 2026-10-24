package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.sistema.IdSistema;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.service.plan.AccesoContenidoService;
import com.ensambla.backend.domain.service.zona.AccesoZonaService;
import com.ensambla.backend.domain.useCase.plan.AccesoContenidoItem;
import com.ensambla.backend.domain.useCase.plan.desbloquearContenidos.DesbloquearContenidos;
import com.ensambla.backend.domain.useCase.plan.desbloquearContenidos.DesbloquearContenidosRequest;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.BloqueItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.SeccionItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem;
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
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CuposContenidoPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-22T15:00:00Z");

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private AccesoContenidoService acceso;
    @Autowired
    private AccesoZonaService zona;
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

    @BeforeEach
    void givenFreeNegocio() {
        usuario = UUID.randomUUID();
        negocio = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario(id,email,nombre) VALUES (?,?,'Cupos locales')", usuario, usuario + "@example.test");
        jdbc.update("INSERT INTO negocio(id,nombre,duenio_id) VALUES (?,'Cupos locales',?)", negocio, usuario);
        suscripcion.asignarGratuito(NegocioId.de(negocio), AHORA.minusDays(1));
    }

    @AfterEach
    void removeTestNegocio() {
        jdbc.update("DELETE FROM negocio WHERE id=?", negocio);
        jdbc.update("DELETE FROM usuario WHERE id=?", usuario);
    }

    @Test
    @DisplayName("El cupo persiste, es separado por función y se renueva al cambiar el mes UTC")
    void seleccionarShouldPersistAndRenewQuota() {
        acceso.seleccionar(NegocioId.de(negocio), TipoContenidoPlan.AVISOS, List.of("a", "b", "c", "d"), AHORA);

        AccesoContenidoItem repetido = acceso
                .seleccionar(NegocioId.de(negocio), TipoContenidoPlan.AVISOS, List.of("d"), AHORA);

        assertThat(repetido.autorizadas()).isEmpty();
        assertThat(acceso.seleccionar(NegocioId.de(negocio), TipoContenidoPlan.ZONA, List.of("a"), AHORA).autorizadas())
                .containsExactly("a");
        assertThat(acceso.seleccionar(NegocioId.de(negocio), TipoContenidoPlan.AVISOS, List.of("d"), AHORA
                .plusMonths(1))
                .autorizadas()).containsExactly("d");
    }

    @Test
    @DisplayName("Mi zona y Otra zona comparten tres productos; el cuarto no sale en el resultado")
    void filtrarShouldShareQuotaBetweenZonesWithoutLeakingProducts() {
        ZonaItem primera = zona.filtrar(NegocioId.de(negocio), givenZona("Mi zona", "A", "B"), AHORA);

        ZonaItem otra = zona.filtrar(NegocioId.de(negocio), givenZona("Otra zona", "C", "D"), AHORA);

        assertThat(primera.productos()).hasSize(2);
        assertThat(otra.productos()).extracting(ZonaItem.ProductoItem::nombre).containsExactly("C");
        assertThat(otra.acceso().bloqueadas()).isEqualTo(1);
        assertThat(otra.acceso().usadas()).isEqualTo(3);
    }

    @Test
    @DisplayName("Dos consultas concurrentes no admiten más de tres contenidos en total")
    void seleccionarShouldSerializeConcurrentUnlocks() throws Exception {
        CountDownLatch salida = new CountDownLatch(1);
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var primero = ejecutor.submit(() -> { salida.await(); return acceso.seleccionar(
                    NegocioId.de(negocio), TipoContenidoPlan.AVISOS, List.of("a", "b", "c"), AHORA); });
            var segundo = ejecutor.submit(() -> { salida.await(); return acceso.seleccionar(
                    NegocioId.de(negocio), TipoContenidoPlan.AVISOS, List.of("d", "e", "f"), AHORA); });
            salida.countDown();

            int admitidos = primero.get(15, TimeUnit.SECONDS).autorizadas().size()
                    + segundo.get(15, TimeUnit.SECONDS).autorizadas().size();

            assertThat(admitidos).isEqualTo(3);
        }
    }

    @Test
    @DisplayName("La prueba vigente permite todos los avisos y al vencer aplica el cupo gratuito")
    void seleccionarShouldRespectTrialExpiry() {
        prueba.iniciar(NegocioId.de(negocio), AHORA);
        assertThat(acceso.seleccionar(NegocioId.de(negocio), TipoContenidoPlan.AVISOS, List.of("a", "b", "c", "d"),
                AHORA)
                .autorizadas()).hasSize(4);

        AccesoContenidoItem vencido = acceso.seleccionar(NegocioId.de(negocio), TipoContenidoPlan.AVISOS,
                List.of("a", "b", "c", "d"), AHORA.plusDays(7));

        assertThat(vencido.autorizadas()).hasSize(3);
        assertThat(vencido.bloqueadas()).isEqualTo(1);
    }

    @Test
    @DisplayName("El guardado completo bloquea el segundo sistema y permite editar el primero")
    void guardarShouldEnforceFreeSystemLimit() {
        guardar(givenSistema("primero", "Sistema local", 0));

        assertThatThrownBy(() -> guardar(givenSistema("segundo", "Sistema local", 0)))
                .isInstanceOf(RestriccionPlanException.class).extracting("codigo").isEqualTo("LIMITE_SISTEMAS");

        guardar(givenSistema("primero", "Nombre corregido", 0));
        assertThat(listarSistemas.execute(new ListarSistemasRequest(UsuarioId.de(usuario)))).singleElement()
                .satisfies(sistema -> assertThat(sistema.name()).isEqualTo("Nombre corregido"));
        assertThat(uso.contarRegistrosProyecto(NegocioId.de(negocio), IdSistema.aId(NegocioId.de(negocio), "primero",
                ProyectoId::de))).isZero();
    }

    @Test
    @DisplayName("La undécima página real del gratuito se rechaza antes de llamar al proveedor")
    void registrarShouldRejectEleventhPage() {
        for (int i = 0; i < 10; i++) consumo.registrar(NegocioId.de(negocio), AHORA);

        assertThatThrownBy(() -> consumo.registrar(NegocioId.de(negocio), AHORA))
                .isInstanceOf(RestriccionPlanException.class).extracting("codigo").isEqualTo("LIMITE_IA_MENSUAL");
    }

    @Test
    @DisplayName("El desbloqueo identifica el negocio exclusivamente desde el usuario de la sesión")
    void executeShouldUseSessionNegocio() {
        AccesoContenidoItem resultado = desbloquear.execute(new DesbloquearContenidosRequest(
                UsuarioId.de(usuario), "AVISOS", List.of("uno", "dos", "tres", "cuatro")));

        assertThat(resultado.autorizadas()).hasSize(3);
        assertThat(resultado.bloqueadas()).isEqualTo(1);
    }

    private void guardar(SistemaItem sistema) {
        guardarSistema.execute(new GuardarSistemaRequest(UsuarioId.de(usuario), sistema.id(), sistema));
    }

    private SistemaItem givenSistema(String id, String nombre, int filas) {
        List<Map<String, Object>> filasDelBloque = IntStream.range(0, filas)
                .<Map<String, Object>>mapToObj(i -> Map.of("id", id + "-fila-" + i))
                .toList();
        BloqueItem bloque = new BloqueItem(id + "-tabla", "table", "Productos", null, "blue", "half", List.of(),
                filasDelBloque, null, null, null);
        return new SistemaItem(id, nombre, AHORA.toString(), AHORA.toString(),
                List.of(new SeccionItem(id + "-general", "Vista general", List.of(bloque))), null, null, null, false);
    }

    @Test
    @DisplayName("El guardado completo rechaza 1.001 filas y conserva el sistema previo sin cambios parciales")
    void guardarShouldRejectOversizedReplacementAtomically() {
        guardar(givenSistema("original", "Sistema local", 0));

        assertThatThrownBy(() -> guardar(givenSistema("original", "Sistema local", 1001)))
                .isInstanceOf(RestriccionPlanException.class).extracting("codigo").isEqualTo("LIMITE_REGISTROS");

        assertThat(uso.contarRegistros(NegocioId.de(negocio))).isZero();
        assertThat(listarSistemas.execute(new ListarSistemasRequest(UsuarioId.de(usuario)))).hasSize(1);
    }

    private ZonaItem givenZona(String nombre, String primero, String segundo) {
        return new ZonaItem(nombre, 5, List.of(givenProducto(primero), givenProducto(segundo)));
    }

    private ZonaItem.ProductoItem givenProducto(String nombre) {
        return new ZonaItem.ProductoItem(nombre, 5, BigDecimal.ONE, BigDecimal.TEN,
                BigDecimal.TEN, 5, BigDecimal.ONE);
    }
}
