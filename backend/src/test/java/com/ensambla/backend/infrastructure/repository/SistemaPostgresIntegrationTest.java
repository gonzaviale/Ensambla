package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.sistema.IdSistema;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.BloqueItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.CampoItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.OpcionesItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.SeccionItem;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.RegistroEntity;
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
    private RegistroRepository registroRepository;

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

    private UUID negocioId;
    private UUID negocioAjenoId;
    private Sistema leido;
    private RegistroId harinaId;
    private UUID filaDeWhatsapp;
    private UUID filaManual;

    @Test
    @DisplayName("Guardar un sistema nuevo lo inserta entero con los ids de la pantalla, las filas en orden y las conexiones")
    void guardarUnSistemaNuevoLoInsertaEnteroConLosIdsDeLaPantallaLasFilasEnOrdenYLasConexiones() {
        dadoUnNegocio();

        cuandoSeGuardaElSistema();

        entoncesSeInsertaEnteroConLosIdsDeLaPantalla();
        entoncesLosProductosTienenSusColumnasYSuConexion();
        entoncesLasFilasQuedanEnOrden();
    }

    @Test
    @DisplayName("Guardar dos veces el mismo sistema no duplica nada")
    void guardarDosVecesElMismoSistemaNoDuplicaNada() {
        dadoUnNegocio();

        cuandoSeGuardaDosVecesElMismoSistema();

        entoncesNoSeDuplicaNada();
    }

    @Test
    @DisplayName("Guardar de nuevo aplica cambios: renombra, quita bloque, columna y fila, y una columna nueva puede reusar la clave de la quitada")
    void guardarDeNuevoAplicaCambiosRenombraQuitaBloqueColumnaYFilaYUnaColumnaNuevaPuedeReusarLaClaveDeLaQuitada() {
        dadoUnSistemaGuardado();

        cuandoSeGuardaElSistemaModificado();

        entoncesSeAplicanLosCambios();
    }

    @Test
    @DisplayName("Los sistemas de otro negocio no aparecen ni se tocan")
    void losSistemasDeOtroNegocioNoAparecenNiSeTocan() {
        dadoUnSistemaGuardadoYUnNegocioAjeno();

        entoncesElNegocioAjenoNoVeNada();
    }

    @Test
    @DisplayName("Guardar conserva la descripción y la plantilla de un proyecto que ya existía")
    void guardarConservaLaDescripcionYLaPlantillaDeUnProyectoQueYaExistia() {
        dadoUnSistemaGuardadoConDescripcionCreadaAMano();

        cuandoSeGuardaElSistemaModificado();

        entoncesSeConservaLaDescripcion();
    }

    @Test
    @DisplayName("Guardar con la hora de lectura conserva las filas que Sami cargó por WhatsApp después de esa lectura")
    void guardarConLaHoraDeLecturaConservaLasFilasQueSamiCargoPorWhatsappDespuesDeEsaLectura() {
        dadoUnSistemaGuardadoConUnaFilaDeWhatsappCargadaDespues();

        cuandoSeGuardaLeidoAhora();

        entoncesSeConservaLaFilaDeWhatsapp();
    }

    @Test
    @DisplayName("Guardar elimina la fila de WhatsApp si el cliente ya la conocía, y las que no son de WhatsApp siempre")
    void guardarEliminaLaFilaDeWhatsappSiElClienteYaLaConociaYLasQueNoSonDeWhatsappSiempre() {
        dadoUnSistemaGuardadoConUnaFilaDeWhatsappYOtraManual();

        cuandoSeGuardaLeidoDentroDeDosHoras();

        entoncesSeEliminanAmbasFilas();
    }

    @Test
    @DisplayName("Guardar sin hora de lectura es el reemplazo exacto de siempre, también con filas de WhatsApp")
    void guardarSinHoraDeLecturaEsElReemplazoExactoDeSiempreTambienConFilasDeWhatsapp() {
        dadoUnSistemaGuardadoConUnaFilaDeWhatsappCargadaDespues();

        cuandoSeGuardaElSistema();

        entoncesSeEliminaLaFilaDeWhatsapp();
    }

    @Test
    @DisplayName("La posición de la próxima fila es una más que la última del bloque")
    void laPosicionDeLaProximaFilaEsUnaMasQueLaUltimaDelBloque() {
        dadoUnSistemaGuardado();

        entoncesLaProximaPosicionDeProductosEsLaDos();
    }

    @Test
    @DisplayName("La posición de la próxima fila es 0 si el bloque no tiene filas")
    void laPosicionDeLaProximaFilaEs0SiElBloqueNoTieneFilas() {
        dadoUnSistemaGuardado();

        entoncesLaProximaPosicionDeNotasEsCero();
    }

    // --- dado ---

    private void dadoUnNegocio() {
        negocioId = crearNegocio();
    }

    private void dadoUnSistemaGuardado() {
        dadoUnNegocio();
        guardar(unSistema());
    }

    private void dadoUnSistemaGuardadoYUnNegocioAjeno() {
        dadoUnSistemaGuardado();
        negocioAjenoId = crearNegocio();
    }

    private void dadoUnSistemaGuardadoConDescripcionCreadaAMano() {
        dadoUnSistemaGuardado();
        var entidad = proyectos.findById(SISTEMA_ID).orElseThrow();
        entidad.setDescripcion("Creado a mano");
        proyectos.saveAndFlush(entidad);
        entityManager.clear();
    }

    private void dadoUnSistemaGuardadoConUnaFilaDeWhatsappCargadaDespues() {
        dadoUnSistemaGuardado();
        filaDeWhatsapp = unaFilaCargadaDespues("WhatsApp");
    }

    private void dadoUnSistemaGuardadoConUnaFilaDeWhatsappYOtraManual() {
        dadoUnSistemaGuardado();
        filaDeWhatsapp = unaFilaCargadaDespues("WhatsApp");
        filaManual = unaFilaCargadaDespues(null);
    }

    // --- cuando ---

    private void cuandoSeGuardaElSistema() {
        guardar(unSistema());
    }

    private void cuandoSeGuardaDosVecesElMismoSistema() {
        guardar(unSistema());
        guardar(unSistema());
    }

    private void cuandoSeGuardaElSistemaModificado() {
        harinaId = registro(HARINA);
        guardar(unSistemaModificado());
    }

    private void cuandoSeGuardaLeidoAhora() {
        guardarLeidoEn(unSistema(), OffsetDateTime.now());
    }

    private void cuandoSeGuardaLeidoDentroDeDosHoras() {
        guardarLeidoEn(unSistema(), OffsetDateTime.now().plusHours(2));
    }

    // --- entonces ---

    private void entoncesSeInsertaEnteroConLosIdsDeLaPantalla() {
        leido = sistemaRepository.findAllByNegocioId(NegocioId.de(negocioId)).getFirst();
        assertThat(leido.id()).isEqualTo(ProyectoId.de(SISTEMA_ID));
        assertThat(leido.proyecto().getNombre()).isEqualTo("Mi almacén");
        assertThat(leido.proyecto().getConMovimientos()).isTrue();
        assertThat(leido.secciones()).hasSize(2);
        assertThat(leido.proyecto().getBloques()).hasSize(3);
        assertThat(sistemaRepository.findNegocioIdByProyectoId(ProyectoId.de(SISTEMA_ID)))
                .contains(NegocioId.de(negocioId));
    }

    private void entoncesLosProductosTienenSusColumnasYSuConexion() {
        Bloque productos = bloqueDeProductos();
        assertThat(productos.getCampos()).extracting("clave").containsExactly("name", "category", "client");
        assertThat(productos.getCampos().getLast().getTipo()).isEqualTo(TipoCampo.RELACION);
        assertThat(productos.getCampos().getLast().getBloqueRelacionadoId())
                .isEqualTo(IdSistema.aId(NegocioId.de(negocioId), CLIENTES, BloqueId::de));
    }

    private void entoncesLasFilasQuedanEnOrden() {
        assertThat(leido.registrosPorBloque().get(idDeProductos())).extracting("id")
                .containsExactly(registro(HARINA), registro(AZUCAR));
    }

    private void entoncesNoSeDuplicaNada() {
        assertThat(proyectos.findAllByNegocioIdOrderByCreadoEnDescIdAsc(negocioId)).hasSize(1);
        assertThat(secciones.findAllByNegocioIdOrderByPosicionAscIdAsc(negocioId)).hasSize(2);
        assertThat(bloques.findAllByNegocioIdOrderByPosicionAscIdAsc(negocioId)).hasSize(3);
        assertThat(campos.findAllByNegocioIdOrderByPosicionAscIdAsc(negocioId)).hasSize(4);
        assertThat(registros.findAllByNegocioIdOrderByOrdenAscCreadoEnAscIdAsc(negocioId)).hasSize(3);
    }

    private void entoncesSeAplicanLosCambios() {
        leido = sistemaRepository.findAllByNegocioId(NegocioId.de(negocioId)).getFirst();
        assertThat(leido.proyecto().getNombre()).isEqualTo("Almacén renombrado");
        assertThat(leido.secciones()).hasSize(1);
        assertThat(leido.proyecto().getBloques()).extracting("id").containsExactly(idDeProductos());
        var columnas = leido.proyecto().getBloques().getFirst().getCampos();
        assertThat(columnas).extracting("clave").containsExactly("name", "category");
        assertThat(columnas.getFirst().getNombre()).isEqualTo("Nombre del producto");
        assertThat(columnas.getLast().getTipo()).isEqualTo(TipoCampo.TEXTO);
        var filas = leido.registrosPorBloque().get(idDeProductos());
        assertThat(filas).extracting("id").containsExactly(registro(AZUCAR), harinaId);
        assertThat(filas.getFirst().getOrden()).isZero();
        assertThat(filas.getLast().getValores().values()).contains("Harina integral");
        assertThat(clientesEnBase()).isZero();
    }

    private void entoncesElNegocioAjenoNoVeNada() {
        assertThat(sistemaRepository.findAllByNegocioId(NegocioId.de(negocioAjenoId))).isEmpty();
        assertThat(sistemaRepository.findNegocioIdByProyectoId(ProyectoId.nuevo())).isEqualTo(Optional.empty());
    }

    private void entoncesSeConservaLaDescripcion() {
        assertThat(proyectos.findById(SISTEMA_ID).orElseThrow().getDescripcion()).isEqualTo("Creado a mano");
    }

    private void entoncesSeConservaLaFilaDeWhatsapp() {
        assertThat(registros.findById(filaDeWhatsapp)).isPresent();
    }

    private void entoncesSeEliminanAmbasFilas() {
        assertThat(registros.findById(filaDeWhatsapp)).isEmpty();
        assertThat(registros.findById(filaManual)).isEmpty();
    }

    private void entoncesSeEliminaLaFilaDeWhatsapp() {
        assertThat(registros.findById(filaDeWhatsapp)).isEmpty();
    }

    private void entoncesLaProximaPosicionDeProductosEsLaDos() {
        assertThat(registroRepository.siguienteOrden(idDeProductos(), NegocioId.de(negocioId))).isEqualTo(2);
    }

    private void entoncesLaProximaPosicionDeNotasEsCero() {
        BloqueId notasId = IdSistema.aId(NegocioId.de(negocioId), NOTAS, BloqueId::de);
        assertThat(registroRepository.siguienteOrden(notasId, NegocioId.de(negocioId))).isZero();
    }

    // --- datos ---

    private void guardar(SistemaItem item) {
        sistemaRepository.guardar(item.aSistema(NegocioId.de(negocioId), SISTEMA_ID.toString(), OffsetDateTime.now()));
        entityManager.flush();
        entityManager.clear();
    }

    private void guardarLeidoEn(SistemaItem item, OffsetDateTime leidoEn) {
        sistemaRepository.guardar(
                item.aSistema(NegocioId.de(negocioId), SISTEMA_ID.toString(), OffsetDateTime.now()), leidoEn);
        entityManager.flush();
        entityManager.clear();
    }

    /** Una fila de Productos que el cliente no conoce: creada una hora después de la lectura, con ese origen. */
    private UUID unaFilaCargadaDespues(String origen) {
        return registros.saveAndFlush(RegistroEntity.builder().negocioId(negocioId)
                .bloqueId(idDeProductos().valor())
                .valores(Map.of("x", "y")).origen(origen).orden(5).creadoEn(OffsetDateTime.now().plusHours(1))
                .actualizadoEn(OffsetDateTime.now()).build()).getId();
    }

    private BloqueId idDeProductos() {
        return IdSistema.aId(NegocioId.de(negocioId), PRODUCTOS, BloqueId::de);
    }

    private Bloque bloqueDeProductos() {
        return leido.proyecto().getBloques().stream()
                .filter(bloque -> bloque.getId().equals(idDeProductos())).findFirst().orElseThrow();
    }

    private RegistroId registro(String idDelFront) {
        return IdSistema.aId(NegocioId.de(negocioId), idDelFront, RegistroId::de);
    }

    private long clientesEnBase() {
        BloqueId clientesId = IdSistema.aId(NegocioId.de(negocioId), CLIENTES, BloqueId::de);
        return bloques.findAllByNegocioIdOrderByPosicionAscIdAsc(negocioId).stream()
                .filter(bloque -> bloque.getId().equals(clientesId.valor())).count();
    }

    private SistemaItem unSistema() {
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
    private SistemaItem unSistemaModificado() {
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

    private UUID crearNegocio() {
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
