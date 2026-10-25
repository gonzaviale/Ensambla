package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.model.negocio.NegocioId;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    @DisplayName("V3 carga las tres plantillas con descripción y estructura")
    void migrationShouldSeedTemplates() {
        var result = plantillaRepository.findAllActivas();

        assertThat(result).extracting("nombre").contains("Clientes", "Inventario", "Pedidos");
        assertThat(plantillaRepository.obtenerParaProyecto(PlantillaId.de(INVENTARIO_ID)).estructura().bloques())
                .hasSize(1);
        assertThat(plantillas.findById(INVENTARIO_ID).orElseThrow().getDescripcion()).isNotBlank();
    }

    @Test
    @DisplayName("Copiar dos veces persiste árboles independientes sin alterar la plantilla")
    void crearDesdePlantillaShouldPersistIndependentTrees() {
        UUID negocioId = givenNegocio();
        var plantilla = plantillaRepository.obtenerParaProyecto(PlantillaId.de(INVENTARIO_ID));
        Map<String, Object> original = Map.copyOf(plantillas.findById(INVENTARIO_ID).orElseThrow().getEstructura());

        Proyecto primero = proyectosDePrueba.crearDesdePlantilla(NegocioId.de(negocioId), plantilla, AHORA);
        Proyecto segundo = proyectosDePrueba.crearDesdePlantilla(NegocioId.de(negocioId), plantilla, AHORA);
        entityManager.flush();
        entityManager.clear();

        Proyecto recargado = proyectoRepository.obtener(NegocioId.de(negocioId), primero.getId());
        assertThat(recargado.getBloques()).hasSize(1);
        var bloque = recargado.getBloques().getFirst();
        assertThat(bloque.getId()).isNotEqualTo(segundo.getBloques().getFirst().getId());
        assertThat(bloque.getCampos()).hasSize(3);
        assertThat(bloque.getCampos().getFirst().getId())
                .isNotEqualTo(segundo.getBloques().getFirst().getCampos().getFirst().getId());
        assertThat(bloque.getCampos().get(1).getValorInicial()).isEqualTo("0");
        assertThat(bloque.getCampos().get(2).getOpciones()).containsExactly("Insumos", "Terminados");
        assertThat(plantillas.findById(INVENTARIO_ID).orElseThrow().getEstructura()).isEqualTo(original);
    }

    @Test
    @DisplayName("Eliminar un proyecto borra sus bloques, campos y registros en cascada")
    void eliminarShouldCascadeThroughWholeGraph() {
        UUID negocioId = givenNegocio();
        Proyecto proyecto = proyectosDePrueba.crearDesdePlantilla(
                NegocioId.de(negocioId), plantillaRepository.obtenerParaProyecto(PlantillaId.de(INVENTARIO_ID)), AHORA);
        var bloque = proyecto.getBloques().getFirst();
        UUID registroId = registros.saveAndFlush(RegistroEntity.builder()
                .negocioId(negocioId)
                .bloqueId(bloque.getId().valor())
                .valores(Map.of(bloque.getCampos().getFirst().getId().toString(), "Producto"))
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();

        proyectoRepository.deleteByIdAndNegocioId(proyecto.getId(), NegocioId.de(negocioId));
        entityManager.flush();
        entityManager.clear();

        assertThat(proyectos.existsById(proyecto.getId().valor())).isFalse();
        assertThat(bloques.existsById(bloque.getId().valor())).isFalse();
        assertThat(campos.existsById(bloque.getCampos().getFirst().getId().valor())).isFalse();
        assertThat(registros.existsById(registroId)).isFalse();
        assertThat(plantillas.existsById(INVENTARIO_ID)).isTrue();
    }

    @Test
    @DisplayName("Las consultas y el borrado no atraviesan el límite de negocio")
    void repositoryShouldIsolateNegocios() {
        UUID propioId = givenNegocio();
        UUID ajenoId = givenNegocio();
        Proyecto ajeno = proyectosDePrueba.crearDesdePlantilla(
                NegocioId.de(ajenoId), plantillaRepository.obtenerParaProyecto(PlantillaId.de(INVENTARIO_ID)), AHORA);

        proyectos.deleteByIdAndNegocioId(ajeno.getId().valor(), propioId);

        assertThat(proyectos.findByIdAndNegocioId(ajeno.getId().valor(), propioId)).isEmpty();
        assertThat(proyectoRepository.findAllByNegocioId(NegocioId.de(propioId))).isEmpty();
        assertThat(proyectos.findByIdAndNegocioId(ajeno.getId().valor(), ajenoId)).isPresent();
        assertThat(bloques.findAllByProyectoIdAndNegocioIdOrderByPosicionAscIdAsc(ajeno.getId().valor(), propioId))
                .isEmpty();
        assertThat(campos.findAllByBloqueIdInAndNegocioIdOrderByPosicionAscIdAsc(
                List.of(ajeno.getBloques().getFirst().getId().valor()), propioId)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Si falla un campo al copiar se revierte también la creación del proyecto")
    void crearDesdePlantillaShouldRollbackWholeGraph() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        UUID negocioId = transaction.execute(status -> givenNegocio());
        UUID plantillaId = transaction.execute(status -> plantillas.saveAndFlush(PlantillaEntity.builder()
                .nombre("Inválida " + UUID.randomUUID())
                .descripcion("Para probar rollback")
                .activa(true)
                .estructura(Map.of("bloques", List.of(Map.of("nombre", "Productos", "campos", List.of(
                        Map.of("nombre", "Nombre", "tipo", "TEXTO"),
                        Map.of("nombre", "Estado", "tipo", "OPCION", "opciones", List.of()))))))
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId());
        UUID usuarioId = negocios.findById(negocioId).orElseThrow().getDuenioId();

        try {
            assertThatThrownBy(() -> proyectosDePrueba.crearDesdePlantilla(
                    NegocioId.de(negocioId), plantillaRepository.obtenerParaProyecto(PlantillaId.de(plantillaId)),
                            AHORA))
                    .isInstanceOf(DataIntegrityViolationException.class);

            assertThat(proyectos.findAllByNegocioIdOrderByCreadoEnDescIdAsc(negocioId)).isEmpty();
        } finally {
            transaction.executeWithoutResult(status -> {
                negocios.deleteById(negocioId);
                negocios.flush();
                usuarios.deleteById(usuarioId);
                plantillas.deleteById(plantillaId);
            });
        }
    }

    // --- arrange ---
    private UUID givenNegocio() {
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

