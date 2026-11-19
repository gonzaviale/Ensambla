package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.model.aviso.AccionEditarRegistro;
import com.ensambla.backend.domain.model.aviso.AccionRegistrar;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.aviso.ReglaAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.precio.HistorialPrecioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.useCase.aviso.ejecutarMotorAvisos.EjecutarMotorAvisos;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Corre el motor de avisos sobre los datos de prueba de V10 (almacén de ejemplo de San Justo). Usa su propia base:
 * los demás tests de repositorio borran negocios y usuarios, y estos datos tienen que estar intactos.
 *
 * <p>Hoy el motor sólo trae la regla de stock; cada regla nueva suma acá su caso positivo y su caso de control.
 */
@SpringBootTest
@ActiveProfiles("postgres-test")
@Transactional
class MotorAvisosDatosPruebaTest {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17-alpine")).withUrlParam("sslmode", "disable");

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private List<ReglaAviso> reglas;

    @Autowired
    private NegocioRepository negocioRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private RegistroRepository registroRepository;

    @Autowired
    private ZonaRepository zonaRepository;

    @Autowired
    private AgregadoZonaRepository agregadoZonaRepository;

    @Autowired
    private HistorialPrecioRepository historialPrecioRepository;

    @Autowired
    private MetricasVentas metricasVentas;

    @Autowired
    private TransaccionPort transaccionPort;

    @Autowired
    private AvisoRepository avisoRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    private UUID demoId;
    private Aviso avisoDeYerba;

    @Test
    @DisplayName("El almacén demo recibe un aviso por cada producto agotado o bajo el mínimo y ninguno por los demás")
    void elAlmacenDemoRecibeUnAvisoPorCadaProductoAgotadoOBajoElMinimoYNingunoPorLosDemas() {
        dadoElNegocioDemo();

        cuandoSeEjecutaElMotor(AHORA);

        entoncesHayUnAvisoDeStockPorCadaProductoAgotadoOBajoElMinimo();
    }

    @Test
    @DisplayName("Los avisos del demo ofrecen registrar la entrada en la tabla de entradas del proyecto")
    void losAvisosDelDemoOfrecenRegistrarLaEntradaEnLaTablaDeEntradasDelProyecto() {
        dadoElNegocioDemo();

        cuandoSeEjecutaElMotor(AHORA);

        entoncesCadaAvisoDeStockOfreceRegistrarLaEntrada();
    }

    @Test
    @DisplayName("Correr el motor dos veces no duplica los avisos")
    void correrElMotorDosVecesNoDuplicaLosAvisos() {
        dadoElNegocioDemo();

        cuandoSeEjecutaElMotorDosVeces();

        entoncesSigueHabiendoTresAvisosDeStock();
    }

    @Test
    @DisplayName("Un aviso descartado no reaparece mientras los datos de origen no cambien")
    void unAvisoDescartadoNoReapareceMientrasLosDatosDeOrigenNoCambien() {
        dadoElNegocioDemo();
        dadoQueSeDescartoElAvisoDeLaYerba();

        cuandoSeEjecutaElMotor(AHORA.plusMinutes(5));

        entoncesQuedanDosAvisosDeStockPendientes();
    }

    @Test
    @DisplayName("Un aviso descartado reaparece cuando cambian los datos de origen")
    void unAvisoDescartadoReapareceCuandoCambianLosDatosDeOrigen() {
        dadoElNegocioDemo();
        dadoQueSeDescartoElAvisoDeLaYerba();
        dadoQueLaYerbaBajoAUnaUnidad();

        cuandoSeEjecutaElMotor(AHORA.plusMinutes(10));

        entoncesElAvisoDeLaYerbaVuelveAEstarPendiente();
    }

    @Test
    @DisplayName("Un producto que repone stock deja de tener aviso y el aviso vence")
    void unProductoQueReponeStockDejaDeTenerAvisoYElAvisoVence() {
        dadoElNegocioDemo();
        dadoQueElMotorYaCorrio();
        dadoQueElAceiteRepusoCincuentaUnidades();

        cuandoSeEjecutaElMotor(AHORA.plusMinutes(5));

        entoncesElAvisoDelAceiteVencio();
    }

    @Test
    @DisplayName("El azúcar del demo, cara y sin ventas hace más de un mes, recibe el aviso de rebajar el precio")
    void elAzucarDelDemoCaraYSinVentasHaceMasDeUnMesRecibeElAvisoDeRebajarElPrecio() {
        dadoElNegocioDemo();

        cuandoSeEjecutaElMotor(OffsetDateTime.now());

        entoncesElAzucarRecibeElAvisoDeRebajarElPrecio();
    }

    @Test
    @DisplayName("Los productos que el demo vende normalmente no reciben el aviso de producto estancado")
    void losProductosQueElDemoVendeNormalmenteNoRecibenElAvisoDeProductoEstancado() {
        dadoElNegocioDemo();

        cuandoSeEjecutaElMotor(OffsetDateTime.now());

        entoncesLaYerbaYLaCocaNoTienenAvisoDeEstancado();
    }

    @Test
    @DisplayName("Rebajar el azúcar hace desaparecer el aviso de producto estancado")
    void rebajarElAzucarHaceDesaparecerElAvisoDeProductoEstancado() {
        dadoElNegocioDemo();
        dadoQueElMotorYaCorrio(OffsetDateTime.now());
        dadoQueSeRebajoElAzucarA1150();

        cuandoSeEjecutaElMotor(OffsetDateTime.now().plusMinutes(5));

        entoncesNoHayAvisosDeEstancado();
    }

    // --- dado ---

    private void dadoElNegocioDemo() {
        demoId = jdbc.queryForObject("SELECT n.id FROM negocio n JOIN usuario u ON u.id = n.duenio_id"
                + " WHERE u.email = 'demo@ensambla.test'", UUID.class);
    }

    private void dadoQueElMotorYaCorrio() {
        dadoQueElMotorYaCorrio(AHORA);
    }

    private void dadoQueElMotorYaCorrio(OffsetDateTime instante) {
        cuandoSeEjecutaElMotor(instante);
    }

    private void dadoQueSeDescartoElAvisoDeLaYerba() {
        cuandoSeEjecutaElMotor(AHORA);
        avisoDeYerba = pendientesDe(TipoAviso.STOCK).stream()
                .filter(aviso -> aviso.getTitulo().startsWith("Yerba")).findFirst().orElseThrow();
        avisoDeYerba.descartar(AHORA);
        avisoRepository.save(avisoDeYerba);
    }

    private void dadoQueLaYerbaBajoAUnaUnidad() {
        cuandoSeEjecutaElMotor(AHORA.plusMinutes(5));
        jdbc.update("UPDATE registro SET valores = jsonb_set(valores, ARRAY[(SELECT c.id::text FROM campo c"
                + " WHERE c.bloque_id = registro.bloque_id AND c.clave = 'quantity')], '1') WHERE id = ?",
                UUID.fromString(avisoDeYerba.getEntidad().substring("registro:".length())));
        entityManager.clear();
    }

    private void dadoQueElAceiteRepusoCincuentaUnidades() {
        jdbc.update("UPDATE registro SET valores = jsonb_set(valores, ARRAY[(SELECT c.id::text FROM campo c"
                + " WHERE c.bloque_id = registro.bloque_id AND c.clave = 'quantity')], '50')"
                + " WHERE negocio_id = ? AND valores::text LIKE '%Aceite Natura%'", demoId);
        entityManager.clear();
    }

    private void dadoQueSeRebajoElAzucarA1150() {
        jdbc.update("UPDATE registro SET valores = jsonb_set(valores, ARRAY[(SELECT c.id::text FROM campo c"
                + " WHERE c.bloque_id = registro.bloque_id AND c.clave = 'price')], '1150')"
                + " WHERE negocio_id = ? AND valores::text LIKE '%Azúcar Ledesma%'", demoId);
        entityManager.clear();
    }

    // --- cuando ---

    /** El motor real sobre el negocio demo y en el instante del test; los demás negocios de la base no se tocan. */
    private void cuandoSeEjecutaElMotor(OffsetDateTime ahora) {
        NegocioId negocioId = NegocioId.de(demoId);
        NegocioRepository soloEsteNegocio = mock(NegocioRepository.class);
        when(soloEsteNegocio.findAllIds()).thenReturn(List.of(negocioId));
        when(soloEsteNegocio.findZonaIdById(negocioId)).thenReturn(negocioRepository.findZonaIdById(negocioId));
        new EjecutarMotorAvisos(soloEsteNegocio, reglas, proyectoRepository, registroRepository, zonaRepository,
                agregadoZonaRepository, historialPrecioRepository, metricasVentas, avisoRepository, transaccionPort,
                Clock.fixed(ahora.toInstant(), ahora.getOffset()))
                .execute();
    }

    private void cuandoSeEjecutaElMotorDosVeces() {
        cuandoSeEjecutaElMotor(AHORA);
        cuandoSeEjecutaElMotor(AHORA.plusMinutes(5));
    }

    // --- entonces ---

    private void entoncesHayUnAvisoDeStockPorCadaProductoAgotadoOBajoElMinimo() {
        List<Aviso> avisos = pendientesDe(TipoAviso.STOCK);
        assertThat(avisos).extracting(Aviso::getTitulo).containsExactlyInAnyOrder(
                "Yerba Playadito 1 kg está por debajo del mínimo",
                "Te quedaste sin Aceite Natura 900 ml",
                "Lavandina Ayudín 1 L está por debajo del mínimo");
        assertThat(avisos).filteredOn(aviso -> aviso.getPrioridad() == 3).hasSize(1);
    }

    private void entoncesCadaAvisoDeStockOfreceRegistrarLaEntrada() {
        assertThat(pendientesDe(TipoAviso.STOCK))
                .allSatisfy(aviso -> assertThat(aviso.getAcciones())
                        .hasAtLeastOneElementOfType(AccionRegistrar.class));
    }

    private void entoncesSigueHabiendoTresAvisosDeStock() {
        assertThat(delTipo(avisoRepository.findByNegocioId(NegocioId.de(demoId)), TipoAviso.STOCK)).hasSize(3);
    }

    private void entoncesQuedanDosAvisosDeStockPendientes() {
        assertThat(pendientesDe(TipoAviso.STOCK)).hasSize(2);
    }

    private void entoncesElAvisoDeLaYerbaVuelveAEstarPendiente() {
        assertThat(pendientesDe(TipoAviso.STOCK))
                .extracting(Aviso::getTitulo).anyMatch(titulo -> titulo.startsWith("Yerba"));
    }

    private void entoncesElAvisoDelAceiteVencio() {
        assertThat(pendientesDe(TipoAviso.STOCK)).hasSize(2);
        assertThat(delTipo(avisoRepository.findByNegocioIdAndEstado(NegocioId.de(demoId), EstadoAviso.VENCIDO),
                TipoAviso.STOCK)).hasSize(1);
    }

    private void entoncesElAzucarRecibeElAvisoDeRebajarElPrecio() {
        assertThat(pendientesDe(TipoAviso.ESTANCADO)).singleElement().satisfies(aviso -> {
            assertThat(aviso.getTitulo()).isEqualTo("Azúcar Ledesma 1 kg está caro y no se mueve");
            assertThat(aviso.getPrioridad()).isEqualTo((short) 2);
            assertThat(aviso.getAcciones()).anySatisfy(accion -> assertThat(accion)
                    .isInstanceOfSatisfying(AccionEditarRegistro.class,
                            editar -> assertThat(editar.texto()).isEqualTo("Rebajar a $1.150")));
            assertThat(aviso.getMotivos().getFirst().toString()).startsWith("En tu zona, 8 comercios lo venden");
        });
    }

    private void entoncesLaYerbaYLaCocaNoTienenAvisoDeEstancado() {
        assertThat(pendientesDe(TipoAviso.ESTANCADO)).extracting(Aviso::getTitulo)
                .noneMatch(titulo -> titulo.startsWith("Yerba") || titulo.startsWith("Coca"));
    }

    private void entoncesNoHayAvisosDeEstancado() {
        assertThat(pendientesDe(TipoAviso.ESTANCADO)).isEmpty();
    }

    // --- datos ---

    private List<Aviso> pendientesDe(TipoAviso tipo) {
        return delTipo(avisoRepository.findByNegocioIdAndEstado(NegocioId.de(demoId), EstadoAviso.PENDIENTE), tipo);
    }

    private static List<Aviso> delTipo(List<Aviso> avisos, TipoAviso tipo) {
        return avisos.stream().filter(aviso -> aviso.getTipo() == tipo).toList();
    }
}
