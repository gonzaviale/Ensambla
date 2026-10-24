package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.sistema.IdSistema;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.BloqueItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.CampoItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.OpcionesItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.SeccionItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IBloqueJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICampoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IProyectoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IRegistroJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ISeccionJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class SistemaPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private static final UUID SISTEMA_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final String SECCION = "e" + UUID.fromString("d1000000-0000-4000-8000-000000000001");
    private static final String OTRA_SECCION = "e" + UUID.fromString("d1000000-0000-4000-8000-000000000002");
    private static final String PRODUCTOS = "e" + UUID.fromString("d2000000-0000-4000-8000-000000000001");
    private static final String CLIENTES = "e" + UUID.fromString("d2000000-0000-4000-8000-000000000002");
    private static final String NOTAS = "e" + UUID.fromString("d2000000-0000-4000-8000-000000000003");
    private static final String HARINA = "e" + UUID.fromString("d3000000-0000-4000-8000-000000000001");
    private static final String AZUCAR = "e" + UUID.fromString("d3000000-0000-4000-8000-000000000002");
    private static final String JUAN = "e" + UUID.fromString("d3000000-0000-4000-8000-000000000003");

    @Autowired
    private SistemaRepository sistemaRepository;

    @Autowired
    private IUsuarioJpaRepository usuarios;

    @Autowired
    private INegocioJpaRepository negocios;

    @Autowired
    private IProyectoJpaRepository proyectos;

    @Autowired
    private ISeccionJpaRepository secciones;

    @Autowired
    private IBloqueJpaRepository bloques;

    @Autowired
    private ICampoJpaRepository campos;

    @Autowired
    private IRegistroJpaRepository registros;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Guardar un sistema nuevo lo inserta entero con los ids de la pantalla, las filas en orden y las conexiones")
    void guardarShouldInsertWholeSistemaKeepingIds() {
        UUID negocioId = givenNegocio();

        guardar(negocioId, givenItem());

        Sistema leido = sistemaRepository.findAllByNegocioId(negocioId).getFirst();
        assertThat(leido.id()).isEqualTo(SISTEMA_ID);
        assertThat(leido.proyecto().getNombre()).isEqualTo("Mi almacén");
        assertThat(leido.proyecto().getConMovimientos()).isTrue();
        assertThat(leido.secciones()).hasSize(2);
        assertThat(leido.proyecto().getBloques()).hasSize(3);
        UUID productosId = IdSistema.aUuid(negocioId, PRODUCTOS);
        UUID clientesId = IdSistema.aUuid(negocioId, CLIENTES);
        var productos = leido.proyecto().getBloques().stream()
                .filter(bloque -> bloque.getId().equals(productosId)).findFirst().orElseThrow();
        assertThat(productos.getCampos()).extracting("clave").containsExactly("name", "category", "client");
        assertThat(productos.getCampos().getLast().getTipo()).isEqualTo(TipoCampo.RELACION);
        assertThat(productos.getCampos().getLast().getBloqueRelacionadoId()).isEqualTo(clientesId);
        assertThat(leido.registrosPorBloque().get(productosId)).extracting("id")
                .containsExactly(IdSistema.aUuid(negocioId, HARINA), IdSistema.aUuid(negocioId, AZUCAR));
        assertThat(sistemaRepository.findNegocioIdByProyectoId(SISTEMA_ID)).contains(negocioId);
    }

    @Test
    @DisplayName("Guardar dos veces el mismo sistema no duplica nada")
    void guardarShouldBeIdempotent() {
        UUID negocioId = givenNegocio();

        guardar(negocioId, givenItem());
        guardar(negocioId, givenItem());

        assertThat(proyectos.findAllByNegocioIdOrderByCreadoEnDescIdAsc(negocioId)).hasSize(1);
        assertThat(secciones.findAllByNegocioIdOrderByPosicionAscIdAsc(negocioId)).hasSize(2);
        assertThat(bloques.findAllByNegocioIdOrderByPosicionAscIdAsc(negocioId)).hasSize(3);
        assertThat(campos.findAllByNegocioIdOrderByPosicionAscIdAsc(negocioId)).hasSize(4);
        assertThat(registros.findAllByNegocioIdOrderByOrdenAscCreadoEnAscIdAsc(negocioId)).hasSize(3);
    }

    @Test
    @DisplayName("Guardar de nuevo aplica cambios: renombra, quita bloque, columna y fila, y una columna nueva puede reusar la clave de la quitada")
    void guardarShouldApplyChanges() {
        UUID negocioId = givenNegocio();
        guardar(negocioId, givenItem());
        UUID harinaId = IdSistema.aUuid(negocioId, HARINA);
        UUID productosId = IdSistema.aUuid(negocioId, PRODUCTOS);

        guardar(negocioId, givenItemModificado());

        Sistema leido = sistemaRepository.findAllByNegocioId(negocioId).getFirst();
        assertThat(leido.proyecto().getNombre()).isEqualTo("Almacén renombrado");
        assertThat(leido.secciones()).hasSize(1);
        assertThat(leido.proyecto().getBloques()).extracting("id").containsExactly(productosId);
        var columnas = leido.proyecto().getBloques().getFirst().getCampos();
        assertThat(columnas).extracting("clave").containsExactly("name", "category");
        assertThat(columnas.getFirst().getNombre()).isEqualTo("Nombre del producto");
        assertThat(columnas.getLast().getTipo()).isEqualTo(TipoCampo.TEXTO);
        var filas = leido.registrosPorBloque().get(productosId);
        assertThat(filas).extracting("id").containsExactly(IdSistema.aUuid(negocioId, AZUCAR), harinaId);
        assertThat(filas.getFirst().getOrden()).isZero();
        assertThat(filas.getLast().getValores().values()).contains("Harina integral");
        assertThat(clientesEnBase(negocioId)).isZero();
    }

    @Test
    @DisplayName("Los sistemas de otro negocio no aparecen ni se tocan")
    void findAllShouldNotLeakOtherNegocios() {
        UUID negocioId = givenNegocio();
        UUID ajenoId = givenNegocio();
        guardar(negocioId, givenItem());

        assertThat(sistemaRepository.findAllByNegocioId(ajenoId)).isEmpty();
        assertThat(sistemaRepository.findNegocioIdByProyectoId(UUID.randomUUID())).isEqualTo(Optional.empty());
    }

    @Test
    @DisplayName("Guardar conserva la descripción y la plantilla de un proyecto que ya existía")
    void guardarShouldKeepDescriptionOfExistingProyecto() {
        UUID negocioId = givenNegocio();
        guardar(negocioId, givenItem());
        var entidad = proyectos.findById(SISTEMA_ID).orElseThrow();
        entidad.setDescripcion("Creado a mano");
        proyectos.saveAndFlush(entidad);
        entityManager.clear();

        guardar(negocioId, givenItemModificado());

        assertThat(proyectos.findById(SISTEMA_ID).orElseThrow().getDescripcion()).isEqualTo("Creado a mano");
    }

    // --- act ---
    private void guardar(UUID negocioId, SistemaItem item) {
        sistemaRepository.guardar(item.aSistema(negocioId, SISTEMA_ID.toString(), OffsetDateTime.now()));
        entityManager.flush();
        entityManager.clear();
    }

    private long clientesEnBase(UUID negocioId) {
        return bloques.findAllByNegocioIdOrderByPosicionAscIdAsc(negocioId).stream()
                .filter(bloque -> bloque.getId().equals(IdSistema.aUuid(negocioId, CLIENTES))).count();
    }

    // --- arrange ---
    private SistemaItem givenItem() {
        BloqueItem productos = new BloqueItem(PRODUCTOS, "inventory", "Productos", "", "orange", "half", List.of(
                new CampoItem("name", "Producto", "text", true, null, null, null),
                new CampoItem("category", "Categoría", "select", false, List.of("Insumos", "Terminados"), null, null),
                new CampoItem("client", "Cliente", "relation", false, null, null, CLIENTES)),
                List.of(fila(HARINA, "name", "Harina", "category", "Insumos", "client", JUAN),
                        fila(AZUCAR, "name", "Azúcar", "category", "Insumos")),
                "", false, null);
        BloqueItem clientes = new BloqueItem(CLIENTES, "clients", "Clientes", "", "blue", "half", List.of(
                new CampoItem("name", "Nombre", "text", true, null, null, null)),
                List.of(fila(JUAN, "name", "Juan")), "", true, null);
        BloqueItem notas = new BloqueItem(NOTAS, "note", "Notas", "", "yellow", "full", List.of(), List.of(),
                "Llamar al proveedor", false, null);
        return new SistemaItem(SISTEMA_ID.toString(), "Mi almacén", "2026-01-10T09:00:00Z", null, List.of(
                new SeccionItem(SECCION, "Vista general", List.of(productos, clientes)),
                new SeccionItem(OTRA_SECCION, "Más", List.of(notas))), "shop", new OpcionesItem(true, false),
                "mosaic", false);
    }

    /** Renombra, quita Clientes y Notas, cambia la columna category por otra con la misma clave y reordena las filas. */
    private SistemaItem givenItemModificado() {
        BloqueItem productos = new BloqueItem(PRODUCTOS, "inventory", "Productos", "", "orange", "half", List.of(
                new CampoItem("name", "Nombre del producto", "text", true, null, null, null),
                new CampoItem("category", "Categoría nueva", "text", false, null, null, null)),
                List.of(fila(AZUCAR, "name", "Azúcar", "category", "Insumos"),
                        fila(HARINA, "name", "Harina integral")),
                "", false, null);
        return new SistemaItem(SISTEMA_ID.toString(), "Almacén renombrado", "2026-01-10T09:00:00Z", null,
                List.of(new SeccionItem(SECCION, "Vista general", List.of(productos))), "shop",
                new OpcionesItem(true, false), "mosaic", false);
    }

    private Map<String, Object> fila(String id, Object... clavesYValores) {
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("id", id);
        for (int i = 0; i < clavesYValores.length; i += 2) {
            fila.put((String) clavesYValores[i], clavesYValores[i + 1]);
        }
        return fila;
    }

    private UUID givenNegocio() {
        UUID usuarioId = usuarios.saveAndFlush(UsuarioEntity.builder()
                .nombre("Persona")
                .email(UUID.randomUUID() + "@example.com")
                .estado(EstadoUsuario.ACTIVO)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
        return negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio")
                .duenioId(usuarioId)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }
}
