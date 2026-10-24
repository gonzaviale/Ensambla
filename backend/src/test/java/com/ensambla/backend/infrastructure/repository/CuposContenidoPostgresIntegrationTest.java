package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.dto.plan.AccesoContenidoItem;
import com.ensambla.backend.domain.dto.plan.DesbloquearContenidosRequest;
import com.ensambla.backend.domain.dto.zona.ZonaItem;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.service.plan.AccesoContenidoService;
import com.ensambla.backend.domain.service.plan.ConsumoIaInmediatoService;
import com.ensambla.backend.domain.service.plan.PruebaPremiumService;
import com.ensambla.backend.domain.service.plan.SuscripcionService;
import com.ensambla.backend.domain.service.sistema.SistemaService;
import com.ensambla.backend.domain.service.zona.AccesoZonaService;
import com.ensambla.backend.domain.useCase.plan.DesbloquearContenidosUseCase;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
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
    private SuscripcionService suscripcion;
    @Autowired
    private PruebaPremiumService prueba;
    @Autowired
    private ConsumoIaInmediatoService consumo;
    @Autowired
    private SistemaService sistemas;
    @Autowired
    private UsoPlanRepository uso;
    @Autowired
    private DesbloquearContenidosUseCase desbloquear;

    private UUID negocio;
    private UUID usuario;

    @BeforeEach
    void givenFreeNegocio() {
        usuario = UUID.randomUUID();
        negocio = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario(id,email,nombre) VALUES (?,?,'Cupos locales')", usuario, usuario + "@example.test");
        jdbc.update("INSERT INTO negocio(id,nombre,duenio_id) VALUES (?,'Cupos locales',?)", negocio, usuario);
        suscripcion.asignarGratuito(negocio, AHORA.minusDays(1));
    }

    @AfterEach
    void removeTestNegocio() {
        jdbc.update("DELETE FROM negocio WHERE id=?", negocio);
        jdbc.update("DELETE FROM usuario WHERE id=?", usuario);
    }

    @Test
    @DisplayName("El cupo persiste, es separado por función y se renueva al cambiar el mes UTC")
    void seleccionarShouldPersistAndRenewQuota() {
        acceso.seleccionar(negocio, TipoContenidoPlan.AVISOS, List.of("a", "b", "c", "d"), AHORA);

        AccesoContenidoItem repetido = acceso.seleccionar(negocio, TipoContenidoPlan.AVISOS, List.of("d"), AHORA);

        assertThat(repetido.autorizadas()).isEmpty();
        assertThat(acceso.seleccionar(negocio, TipoContenidoPlan.ZONA, List.of("a"), AHORA).autorizadas())
                .containsExactly("a");
        assertThat(acceso.seleccionar(negocio, TipoContenidoPlan.AVISOS, List.of("d"), AHORA.plusMonths(1))
                .autorizadas()).containsExactly("d");
    }

    @Test
    @DisplayName("Mi zona y Otra zona comparten tres productos; el cuarto no sale en el resultado")
    void filtrarShouldShareQuotaBetweenZonesWithoutLeakingProducts() {
        ZonaItem primera = zona.filtrar(negocio, givenZona("Mi zona", "A", "B"), AHORA);

        ZonaItem otra = zona.filtrar(negocio, givenZona("Otra zona", "C", "D"), AHORA);

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
                    negocio, TipoContenidoPlan.AVISOS, List.of("a", "b", "c"), AHORA); });
            var segundo = ejecutor.submit(() -> { salida.await(); return acceso.seleccionar(
                    negocio, TipoContenidoPlan.AVISOS, List.of("d", "e", "f"), AHORA); });
            salida.countDown();

            int admitidos = primero.get(15, TimeUnit.SECONDS).autorizadas().size()
                    + segundo.get(15, TimeUnit.SECONDS).autorizadas().size();

            assertThat(admitidos).isEqualTo(3);
        }
    }

    @Test
    @DisplayName("La prueba vigente permite todos los avisos y al vencer aplica el cupo gratuito")
    void seleccionarShouldRespectTrialExpiry() {
        prueba.iniciar(negocio, AHORA);
        assertThat(acceso.seleccionar(negocio, TipoContenidoPlan.AVISOS, List.of("a", "b", "c", "d"), AHORA)
                .autorizadas()).hasSize(4);

        AccesoContenidoItem vencido = acceso.seleccionar(negocio, TipoContenidoPlan.AVISOS,
                List.of("a", "b", "c", "d"), AHORA.plusDays(7));

        assertThat(vencido.autorizadas()).hasSize(3);
        assertThat(vencido.bloqueadas()).isEqualTo(1);
    }

    @Test
    @DisplayName("El guardado completo bloquea el segundo sistema y permite editar el primero")
    void guardarShouldEnforceFreeSystemLimit() {
        Sistema primero = givenSistema();
        sistemas.guardar(negocio, primero);

        assertThatThrownBy(() -> sistemas.guardar(negocio, givenSistema()))
                .isInstanceOf(RestriccionPlanException.class).extracting("codigo").isEqualTo("LIMITE_SISTEMAS");

        primero.proyecto().renombrar("Nombre corregido", AHORA);
        sistemas.guardar(negocio, primero);
        assertThat(sistemas.listar(negocio)).singleElement()
                .satisfies(sistema -> assertThat(sistema.proyecto().getNombre()).isEqualTo("Nombre corregido"));
        assertThat(uso.contarRegistrosProyecto(negocio, primero.id())).isZero();
    }

    @Test
    @DisplayName("La undécima página real del gratuito se rechaza antes de llamar al proveedor")
    void registrarShouldRejectEleventhPage() {
        for (int i = 0; i < 10; i++) consumo.registrar(negocio, AHORA);

        assertThatThrownBy(() -> consumo.registrar(negocio, AHORA))
                .isInstanceOf(RestriccionPlanException.class).extracting("codigo").isEqualTo("LIMITE_IA_MENSUAL");
    }

    @Test
    @DisplayName("El desbloqueo identifica el negocio exclusivamente desde el usuario de la sesión")
    void executeShouldUseSessionNegocio() {
        AccesoContenidoItem resultado = desbloquear.execute(new DesbloquearContenidosRequest(
                usuario, "AVISOS", List.of("uno", "dos", "tres", "cuatro")));

        assertThat(resultado.autorizadas()).hasSize(3);
        assertThat(resultado.bloqueadas()).isEqualTo(1);
    }

    private Sistema givenSistema() {
        return new Sistema(Proyecto.builder().id(UUID.randomUUID()).negocioId(negocio).nombre("Sistema local")
                .bloques(List.of()).creadoEn(AHORA).actualizadoEn(AHORA).build(), List.of(), Map.of());
    }

    @Test
    @DisplayName("El guardado completo rechaza 1.001 filas y conserva el sistema previo sin cambios parciales")
    void guardarShouldRejectOversizedReplacementAtomically() {
        Sistema original = givenSistema();
        sistemas.guardar(negocio, original);
        List<Registro> filas = IntStream.range(0, 1001).mapToObj(i -> Registro.builder()
                .id(UUID.randomUUID()).negocioId(negocio).valores(Map.of()).build()).toList();
        Sistema excesivo = new Sistema(original.proyecto(), List.of(), Map.of(UUID.randomUUID(), filas));

        assertThatThrownBy(() -> sistemas.guardar(negocio, excesivo))
                .isInstanceOf(RestriccionPlanException.class).extracting("codigo").isEqualTo("LIMITE_REGISTROS");

        assertThat(uso.contarRegistros(negocio)).isZero();
        assertThat(sistemas.listar(negocio)).hasSize(1);
    }

    private ZonaItem givenZona(String nombre, String primero, String segundo) {
        return new ZonaItem(nombre, 5, List.of(givenProducto(primero), givenProducto(segundo)));
    }

    private ZonaItem.ProductoItem givenProducto(String nombre) {
        return new ZonaItem.ProductoItem(nombre, 5, BigDecimal.ONE, BigDecimal.TEN,
                BigDecimal.TEN, 5, BigDecimal.ONE);
    }
}
