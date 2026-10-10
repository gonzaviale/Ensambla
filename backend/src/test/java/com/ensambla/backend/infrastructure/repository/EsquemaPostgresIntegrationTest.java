package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.model.enums.EstadoUsuario;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.PlantillaEntity;
import com.ensambla.backend.infrastructure.repository.entity.ProyectoEntity;
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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica que V1 y las entidades coinciden (el perfil usa ddl-auto=validate) y que las reglas de
 * aislamiento por negocio las hace cumplir la base. Cada test hace rollback.
 */
@Transactional
class EsquemaPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-09T12:00:00Z");

    @Autowired
    private IUsuarioJpaRepository usuarios;

    @Autowired
    private INegocioJpaRepository negocios;

    @Autowired
    private IPlantillaJpaRepository plantillas;

    @Autowired
    private IProyectoJpaRepository proyectos;

    @Autowired
    private IBloqueJpaRepository bloques;

    @Autowired
    private ICampoJpaRepository campos;

    @Autowired
    private IRegistroJpaRepository registros;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Persiste y relee el grafo completo usuario → negocio → proyecto → bloque → campo → registro")
    void shouldPersistAndReloadWholeGraph() {
        UUID negocioId = givenNegocio("dueña@example.com");
        UUID proyectoId = givenProyecto(negocioId, "Mi inventario");
        UUID bloqueId = givenBloque(negocioId, proyectoId, "Productos");
        UUID campoId = givenCampo(negocioId, bloqueId, "Rubro", TipoCampo.OPCION, List.of("Velas", "Moldes"));
        UUID registroId = givenRegistro(negocioId, bloqueId, Map.of(campoId.toString(), "Velas"));
        entityManager.clear();

        RegistroEntity registro = registros.findById(registroId).orElseThrow();
        CampoEntity campo = campos.findById(campoId).orElseThrow();

        assertThat(registro.getValores()).containsEntry(campoId.toString(), "Velas");
        assertThat(campo.getOpciones()).containsExactly("Velas", "Moldes");
        assertThat(campo.getTipo()).isEqualTo(TipoCampo.OPCION);
    }

    @Test
    @DisplayName("Guarda y relee la estructura JSON de una plantilla")
    void shouldPersistPlantillaEstructura() {
        Map<String, Object> estructura = Map.of("bloques", List.of(Map.of("nombre", "Clientes")));
        PlantillaEntity plantilla = plantillas.saveAndFlush(PlantillaEntity.builder()
                .nombre("Clientes").descripcion("Tu agenda").estructura(estructura).activa(true)
                .creadoEn(AHORA).actualizadoEn(AHORA).build());
        entityManager.clear();

        PlantillaEntity reloaded = plantillas.findById(plantilla.getId()).orElseThrow();

        assertThat(reloaded.getEstructura()).isEqualTo(estructura);
    }

    @Test
    @DisplayName("Lista sólo las plantillas activas, ordenadas por nombre")
    void findByActivaTrueShouldReturnOnlyActivasOrderedByNombre() {
        givenPlantilla("Pedidos", true);
        givenPlantilla("Clientes", true);
        givenPlantilla("Vieja", false);

        List<PlantillaEntity> result = plantillas.findByActivaTrueOrderByNombreAscIdAsc();

        assertThat(result).extracting(PlantillaEntity::getNombre).containsExactly("Clientes", "Pedidos");
    }

    @Test
    @DisplayName("Encuentra al usuario por su email normalizado")
    void findByEmailShouldReturnUsuario() {
        givenUsuario("persona@example.com");

        assertThat(usuarios.findByEmail("persona@example.com")).isPresent();
        assertThat(usuarios.findByEmail("otra@example.com")).isEmpty();
    }

    @Test
    @DisplayName("La base rechaza un bloque cuyo negocio no es el del proyecto")
    void shouldRejectBloqueFromAnotherNegocio() {
        UUID negocioA = givenNegocio("a@example.com");
        UUID negocioB = givenNegocio("b@example.com");
        UUID proyectoDeA = givenProyecto(negocioA, "Proyecto de A");

        assertThatThrownBy(() -> givenBloque(negocioB, proyectoDeA, "Intruso"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("La base rechaza un registro cuyo negocio no es el del bloque")
    void shouldRejectRegistroFromAnotherNegocio() {
        UUID negocioA = givenNegocio("a@example.com");
        UUID negocioB = givenNegocio("b@example.com");
        UUID bloqueDeA = givenBloque(negocioA, givenProyecto(negocioA, "Proyecto de A"), "Productos");

        assertThatThrownBy(() -> givenRegistro(negocioB, bloqueDeA, Map.of()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("La base rechaza un campo de tipo OPCION sin opciones")
    void shouldRejectOpcionCampoWithoutOptions() {
        UUID negocioId = givenNegocio("a@example.com");
        UUID bloqueId = givenBloque(negocioId, givenProyecto(negocioId, "Proyecto"), "Productos");

        assertThatThrownBy(() -> givenCampo(negocioId, bloqueId, "Rubro", TipoCampo.OPCION, List.of()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Borrar un proyecto borra en cascada sus bloques, campos y registros")
    void shouldCascadeDeleteFromProyecto() {
        UUID negocioId = givenNegocio("a@example.com");
        UUID proyectoId = givenProyecto(negocioId, "Proyecto");
        UUID bloqueId = givenBloque(negocioId, proyectoId, "Productos");
        UUID campoId = givenCampo(negocioId, bloqueId, "Nombre", TipoCampo.TEXTO, List.of());
        UUID registroId = givenRegistro(negocioId, bloqueId, Map.of(campoId.toString(), "Vela"));

        proyectos.deleteById(proyectoId);
        entityManager.flush();
        entityManager.clear();

        assertThat(bloques.findById(bloqueId)).isEmpty();
        assertThat(campos.findById(campoId)).isEmpty();
        assertThat(registros.findById(registroId)).isEmpty();
    }

    // --- arrange ---
    private UUID givenUsuario(String email) {
        return usuarios.saveAndFlush(UsuarioEntity.builder()
                .nombre("Persona").email(email).hashContrasenia("{bcrypt}x").estado(EstadoUsuario.ACTIVO)
                .creadoEn(AHORA).actualizadoEn(AHORA).build()).getId();
    }

    private UUID givenNegocio(String emailDuenio) {
        UUID duenioId = givenUsuario(emailDuenio);
        return negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio de " + emailDuenio).duenioId(duenioId)
                .creadoEn(AHORA).actualizadoEn(AHORA).build()).getId();
    }

    private void givenPlantilla(String nombre, boolean activa) {
        plantillas.saveAndFlush(PlantillaEntity.builder()
                .nombre(nombre).descripcion("Descripción").estructura(Map.of("bloques", List.of())).activa(activa)
                .creadoEn(AHORA).actualizadoEn(AHORA).build());
    }

    private UUID givenProyecto(UUID negocioId, String nombre) {
        return proyectos.saveAndFlush(ProyectoEntity.builder()
                .negocioId(negocioId).nombre(nombre).creadoEn(AHORA).actualizadoEn(AHORA).build()).getId();
    }

    private UUID givenBloque(UUID negocioId, UUID proyectoId, String nombre) {
        return bloques.saveAndFlush(BloqueEntity.builder()
                .negocioId(negocioId).proyectoId(proyectoId).nombre(nombre).posicion((short) 0)
                .creadoEn(AHORA).actualizadoEn(AHORA).build()).getId();
    }

    private UUID givenCampo(UUID negocioId, UUID bloqueId, String nombre, TipoCampo tipo, List<String> opciones) {
        return campos.saveAndFlush(CampoEntity.builder()
                .negocioId(negocioId).bloqueId(bloqueId).nombre(nombre).tipo(tipo).obligatorio(false)
                .opciones(opciones).posicion((short) 0).creadoEn(AHORA).actualizadoEn(AHORA).build()).getId();
    }

    private UUID givenRegistro(UUID negocioId, UUID bloqueId, Map<String, Object> valores) {
        return registros.saveAndFlush(RegistroEntity.builder()
                .negocioId(negocioId).bloqueId(bloqueId).valores(valores)
                .creadoEn(AHORA).actualizadoEn(AHORA).build()).getId();
    }
}
