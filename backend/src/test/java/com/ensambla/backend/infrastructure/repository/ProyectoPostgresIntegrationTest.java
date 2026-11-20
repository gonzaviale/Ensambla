package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plantilla.Plantilla;
import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.PlantillaEntity;
import com.ensambla.backend.infrastructure.repository.entity.RegistroEntity;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IBloqueJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICampoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IPlantillaJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IProyectoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IRegistroJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.ensambla.backend.support.ProyectosDePrueba;
import com.ensambla.backend.support.SuscripcionesDePrueba;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@Transactional
class ProyectoPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private static final UUID INVENTARIO_ID = UUID.fromString("00000000-0000-4000-8000-0000000000a1");

    @Autowired
    private ProyectosDePrueba proyectosDePrueba;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private PlantillaRepository plantillaRepository;

    @Autowired
    private IUsuarioJpaRepository usuarios;

    @Autowired
    private INegocioJpaRepository negocios;

    @Autowired
    private SuscripcionesDePrueba suscripcionService;

    @Autowired
    private IProyectoJpaRepository proyectos;

    @Autowired
    private IPlantillaJpaRepository plantillas;

    @Autowired
    private IBloqueJpaRepository bloques;

    @Autowired
    private ICampoJpaRepository campos;

    @Autowired
    private IRegistroJpaRepository registros;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private UUID negocioId;
    private UUID negocioAjenoId;
    private UUID plantillaInvalidaId;
    private UUID usuarioDelRollbackId;
    private Map<String, Object> estructuraOriginal;
    private Proyecto primero;
    private Proyecto segundo;
    private Proyecto proyecto;
    private Proyecto ajeno;
    private UUID registroId;
    private List<Plantilla> sembradas;
    private Throwable rechazo;

    @AfterEach
    void limpiarLoCreadoFueraDeLaTransaccion() {
        if (plantillaInvalidaId == null) {
            return;
        }
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            negocios.deleteById(negocioId);
            negocios.flush();
            usuarios.deleteById(usuarioDelRollbackId);
            plantillas.deleteById(plantillaInvalidaId);
        });
    }

    @Test
    @DisplayName("V3 carga las tres plantillas con descripción y estructura")
    void v3CargaLasTresPlantillasConDescripcionYEstructura() {
        cuandoSeListanLasPlantillasActivas();

        entoncesEstanLasPlantillasConDescripcionYEstructura();
    }

    @Test
    @DisplayName("Copiar dos veces persiste árboles independientes sin alterar la plantilla")
    void copiarDosVecesPersisteArbolesIndependientesSinAlterarLaPlantilla() {
        dadoUnNegocioPropio();

        cuandoSeCopiaDosVecesLaPlantillaDeInventario();

        entoncesLosArbolesSonIndependientesYLaPlantillaNoCambio();
    }

    @Test
    @DisplayName("Eliminar un proyecto borra sus bloques, campos y registros en cascada")
    void eliminarUnProyectoBorraSusBloquesCamposYRegistrosEnCascada() {
        dadoUnProyectoConUnRegistro();

        cuandoSeEliminaElProyecto();

        entoncesSeBorraTodoElGrafoMenosLaPlantilla();
    }

    @Test
    @DisplayName("Las consultas y el borrado no atraviesan el límite de negocio")
    void lasConsultasYElBorradoNoAtraviesanElLimiteDeNegocio() {
        dadoUnProyectoDeOtroNegocio();

        cuandoUnNegocioIntentaBorrarElProyectoAjeno();

        entoncesElProyectoAjenoSigueIntactoYInvisibleParaElNegocioPropio();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Si falla un campo al copiar se revierte también la creación del proyecto")
    void siFallaUnCampoAlCopiarSeRevierteTambienLaCreacionDelProyecto() {
        dadoUnaPlantillaConUnCampoDeOpcionSinOpciones();

        cuandoSeIntentaCopiarLaPlantillaInvalida();

        entoncesNoQuedaNingunProyectoDelNegocio();
    }

    // --- dado ---

    private void dadoUnNegocioPropio() {
        negocioId = crearNegocio();
    }

    private void dadoUnProyectoConUnRegistro() {
        dadoUnNegocioPropio();
        proyecto = copiarInventarioPara(negocioId);
        var bloque = proyecto.getBloques().getFirst();
        registroId = registros.saveAndFlush(RegistroEntity.builder()
                .negocioId(negocioId)
                .bloqueId(bloque.getId().valor())
                .valores(Map.of(bloque.getCampos().getFirst().getId().toString(), "Producto"))
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private void dadoUnProyectoDeOtroNegocio() {
        dadoUnNegocioPropio();
        negocioAjenoId = crearNegocio();
        ajeno = copiarInventarioPara(negocioAjenoId);
    }

    private void dadoUnaPlantillaConUnCampoDeOpcionSinOpciones() {
        TransactionTemplate transaccion = new TransactionTemplate(transactionManager);
        negocioId = transaccion.execute(status -> crearNegocio());
        plantillaInvalidaId = transaccion.execute(status -> plantillas.saveAndFlush(PlantillaEntity.builder()
                .nombre("Inválida " + UUID.randomUUID())
                .descripcion("Para probar rollback")
                .activa(true)
                .estructura(Map.of("bloques", List.of(Map.of("nombre", "Productos", "campos", List.of(
                        Map.of("nombre", "Nombre", "tipo", "TEXTO"),
                        Map.of("nombre", "Estado", "tipo", "OPCION", "opciones", List.of()))))))
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId());
        usuarioDelRollbackId = negocios.findById(negocioId).orElseThrow().getDuenioId();
    }

    // --- cuando ---

    private void cuandoSeListanLasPlantillasActivas() {
        sembradas = plantillaRepository.findAllActivas();
    }

    private void cuandoSeCopiaDosVecesLaPlantillaDeInventario() {
        estructuraOriginal = Map.copyOf(plantillas.findById(INVENTARIO_ID).orElseThrow().getEstructura());
        primero = copiarInventarioPara(negocioId);
        segundo = copiarInventarioPara(negocioId);
        entityManager.flush();
        entityManager.clear();
    }

    private void cuandoSeEliminaElProyecto() {
        proyectoRepository.deleteByIdAndNegocioId(proyecto.getId(), NegocioId.de(negocioId));
        entityManager.flush();
        entityManager.clear();
    }

    private void cuandoUnNegocioIntentaBorrarElProyectoAjeno() {
        proyectos.deleteByIdAndNegocioId(ajeno.getId().valor(), negocioId);
    }

    private void cuandoSeIntentaCopiarLaPlantillaInvalida() {
        rechazo = catchThrowable(() -> proyectosDePrueba.crearDesdePlantilla(
                NegocioId.de(negocioId), plantillaRepository.obtenerParaProyecto(PlantillaId.de(plantillaInvalidaId)),
                AHORA));
    }

    // --- entonces ---

    private void entoncesEstanLasPlantillasConDescripcionYEstructura() {
        assertThat(sembradas).extracting("nombre").contains("Clientes", "Inventario", "Pedidos");
        assertThat(plantillaRepository.obtenerParaProyecto(PlantillaId.de(INVENTARIO_ID)).estructura().bloques())
                .hasSize(1);
        assertThat(plantillas.findById(INVENTARIO_ID).orElseThrow().getDescripcion()).isNotBlank();
    }

    private void entoncesLosArbolesSonIndependientesYLaPlantillaNoCambio() {
        Proyecto recargado = proyectoRepository.obtener(NegocioId.de(negocioId), primero.getId());
        assertThat(recargado.getBloques()).hasSize(1);
        var bloque = recargado.getBloques().getFirst();
        assertThat(bloque.getId()).isNotEqualTo(segundo.getBloques().getFirst().getId());
        assertThat(bloque.getCampos()).hasSize(3);
        assertThat(bloque.getCampos().getFirst().getId())
                .isNotEqualTo(segundo.getBloques().getFirst().getCampos().getFirst().getId());
        assertThat(bloque.getCampos().get(1).getValorInicial()).isEqualTo("0");
        assertThat(bloque.getCampos().get(2).getOpciones()).containsExactly("Insumos", "Terminados");
        assertThat(plantillas.findById(INVENTARIO_ID).orElseThrow().getEstructura()).isEqualTo(estructuraOriginal);
    }

    private void entoncesSeBorraTodoElGrafoMenosLaPlantilla() {
        var bloque = proyecto.getBloques().getFirst();
        assertThat(proyectos.existsById(proyecto.getId().valor())).isFalse();
        assertThat(bloques.existsById(bloque.getId().valor())).isFalse();
        assertThat(campos.existsById(bloque.getCampos().getFirst().getId().valor())).isFalse();
        assertThat(registros.existsById(registroId)).isFalse();
        assertThat(plantillas.existsById(INVENTARIO_ID)).isTrue();
    }

    private void entoncesElProyectoAjenoSigueIntactoYInvisibleParaElNegocioPropio() {
        assertThat(proyectos.findByIdAndNegocioId(ajeno.getId().valor(), negocioId)).isEmpty();
        assertThat(proyectoRepository.findAllByNegocioId(NegocioId.de(negocioId))).isEmpty();
        assertThat(proyectos.findByIdAndNegocioId(ajeno.getId().valor(), negocioAjenoId)).isPresent();
        assertThat(bloques.findAllByProyectoIdAndNegocioIdOrderByPosicionAscIdAsc(ajeno.getId().valor(), negocioId))
                .isEmpty();
        assertThat(campos.findAllByBloqueIdInAndNegocioIdOrderByPosicionAscIdAsc(
                List.of(ajeno.getBloques().getFirst().getId().valor()), negocioId)).isEmpty();
    }

    private void entoncesNoQuedaNingunProyectoDelNegocio() {
        assertThat(rechazo).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(proyectos.findAllByNegocioIdOrderByCreadoEnDescIdAsc(negocioId)).isEmpty();
    }

    // --- datos ---

    private Proyecto copiarInventarioPara(UUID negocio) {
        return proyectosDePrueba.crearDesdePlantilla(NegocioId.de(negocio),
                plantillaRepository.obtenerParaProyecto(PlantillaId.de(INVENTARIO_ID)), AHORA);
    }

    private UUID crearNegocio() {
        UUID usuarioId = usuarios.saveAndFlush(UsuarioEntity.builder()
                .nombre("Persona")
                .email(UUID.randomUUID() + "@example.com")
                .estado(EstadoUsuario.ACTIVO)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
        UUID creadoId = negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio")
                .duenioId(usuarioId)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
        suscripcionService.asignarGratuito(NegocioId.de(creadoId), AHORA.minusDays(1));
        return creadoId;
    }
}
