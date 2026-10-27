package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.GrupoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.lote.EstadoLote;
import com.ensambla.backend.domain.model.lote.FotoId;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.infrastructure.repository.entity.AgregadoZonaProductoEntity;
import com.ensambla.backend.infrastructure.repository.entity.AvisoEntity;
import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import com.ensambla.backend.infrastructure.repository.entity.FotoCuadernoEntity;
import com.ensambla.backend.infrastructure.repository.entity.LecturaIaEntity;
import com.ensambla.backend.infrastructure.repository.entity.LoteSubidaEntity;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.PlantillaEntity;
import com.ensambla.backend.infrastructure.repository.entity.ProyectoEntity;
import com.ensambla.backend.infrastructure.repository.entity.RegistroEntity;
import com.ensambla.backend.infrastructure.repository.entity.SeccionEntity;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.entity.ZonaEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IBloqueJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICampoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IFotoCuadernoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ILecturaIaJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ILoteSubidaJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IPlantillaJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IProyectoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IRegistroJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica que las migraciones (V1 a V10) y las entidades coinciden (el perfil usa ddl-auto=validate) y que las reglas de
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
    private ILoteSubidaJpaRepository lotes;

    @Autowired
    private IFotoCuadernoJpaRepository fotos;

    @Autowired
    private ILecturaIaJpaRepository lecturas;

    @Autowired
    private EntityManager entityManager;


    @Test
    @DisplayName("Persiste y relee el grafo completo usuario → negocio → proyecto → bloque → campo → registro")
    void shouldPersistAndReloadWholeGraph() {
        NegocioId negocioId = NegocioId.de(givenNegocio("dueña@example.com"));
        ProyectoId proyectoId = ProyectoId.de(givenProyecto(negocioId, "Mi inventario"));
        BloqueId bloqueId = BloqueId.de(givenBloque(negocioId, proyectoId, "Productos"));
        CampoId campoId = CampoId.de(givenCampo(negocioId, bloqueId, "Rubro", TipoCampo.OPCION, List
                .of("Velas", "Moldes")));
        RegistroId registroId = RegistroId.de(givenRegistro(negocioId, bloqueId, Map.of(campoId.toString(), "Velas")));
        entityManager.clear();

        RegistroEntity registro = registros.findById(registroId.valor()).orElseThrow();
        CampoEntity campo = campos.findById(campoId.valor()).orElseThrow();

        assertThat(registro.getValores()).containsEntry(campoId.toString(), "Velas");
        assertThat(campo.getOpciones()).containsExactly("Velas", "Moldes");
        assertThat(campo.getTipo()).isEqualTo(TipoCampo.OPCION);
    }

    @Test
    @DisplayName("Guarda y relee la estructura JSON de una plantilla")
    void shouldPersistPlantillaEstructura() {
        Map<String, Object> estructura = Map.of("bloques", List.of(Map.of("nombre", "Clientes")));
        PlantillaEntity plantilla = plantillas.saveAndFlush(PlantillaEntity.builder()
                .nombre("Clientes de prueba").descripcion("Tu agenda").estructura(estructura).activa(true)
                .creadoEn(AHORA).actualizadoEn(AHORA).build());
        entityManager.clear();

        PlantillaEntity reloaded = plantillas.findById(plantilla.getId()).orElseThrow();

        assertThat(reloaded.getEstructura()).isEqualTo(estructura);
    }

    @Test
    @DisplayName("Lista sólo las plantillas activas, ordenadas por nombre")
    void findByActivaTrueShouldReturnOnlyActivasOrderedByNombre() {
        plantillas.deleteAllInBatch();
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
        UUID proyectoDeA = givenProyecto(NegocioId.de(negocioA), "Proyecto de A");

        assertThatThrownBy(() -> givenBloque(NegocioId.de(negocioB), ProyectoId.de(proyectoDeA), "Intruso"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("La base rechaza un registro cuyo negocio no es el del bloque")
    void shouldRejectRegistroFromAnotherNegocio() {
        UUID negocioA = givenNegocio("a@example.com");
        UUID negocioB = givenNegocio("b@example.com");
        UUID bloqueDeA = givenBloque(NegocioId.de(negocioA), ProyectoId
                .de(givenProyecto(NegocioId.de(negocioA), "Proyecto de A")), "Productos");

        assertThatThrownBy(() -> givenRegistro(NegocioId.de(negocioB), BloqueId.de(bloqueDeA), Map.of()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("La base rechaza un campo de tipo OPCION sin opciones")
    void shouldRejectOpcionCampoWithoutOptions() {
        NegocioId negocioId = NegocioId.de(givenNegocio("a@example.com"));
        BloqueId bloqueId = BloqueId
                .de(givenBloque(negocioId, ProyectoId.de(givenProyecto(negocioId, "Proyecto")), "Productos"));

        assertThatThrownBy(() -> givenCampo(negocioId, bloqueId, "Rubro", TipoCampo.OPCION, List.of()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Borrar un proyecto borra en cascada sus bloques, campos y registros")
    void shouldCascadeDeleteFromProyecto() {
        NegocioId negocioId = NegocioId.de(givenNegocio("a@example.com"));
        ProyectoId proyectoId = ProyectoId.de(givenProyecto(negocioId, "Proyecto"));
        BloqueId bloqueId = BloqueId.de(givenBloque(negocioId, proyectoId, "Productos"));
        CampoId campoId = CampoId.de(givenCampo(negocioId, bloqueId, "Nombre", TipoCampo.TEXTO, List.of()));
        RegistroId registroId = RegistroId.de(givenRegistro(negocioId, bloqueId, Map.of(campoId.toString(), "Vela")));

        proyectos.deleteById(proyectoId.valor());
        entityManager.flush();
        entityManager.clear();

        assertThat(bloques.findById(bloqueId.valor())).isEmpty();
        assertThat(campos.findById(campoId.valor())).isEmpty();
        assertThat(registros.findById(registroId.valor())).isEmpty();
    }

    @Test
    @DisplayName("Persiste un lote con su foto, su lectura y un registro vinculado a la foto de origen")
    void shouldPersistLoteFotoLecturaAndRegistroConFotoOrigen() {
        NegocioId negocioId = NegocioId.de(givenNegocio("a@example.com"));
        BloqueId bloqueId = BloqueId
                .de(givenBloque(negocioId, ProyectoId.de(givenProyecto(negocioId, "Proyecto")), "Ventas"));
        LoteId loteId = LoteId.de(givenLote(negocioId, bloqueId));
        FotoId fotoId = FotoId.de(givenFoto(negocioId, loteId, "negocios/a/lote/1.jpg"));
        UUID lecturaId = givenLectura(negocioId, loteId, Map.of("tipoRegistro", "VENTAS"), null);
        RegistroId registroId = RegistroId.de(givenRegistro(negocioId, bloqueId, Map.of(), fotoId));
        entityManager.clear();

        assertThat(lotes.findById(loteId.valor()).orElseThrow().getEstado()).isEqualTo(EstadoLote.PENDIENTE);
        assertThat(lecturas.findById(lecturaId).orElseThrow().getResultado()).containsEntry("tipoRegistro", "VENTAS");
        assertThat(registros.findById(registroId.valor()).orElseThrow().getFotoOrigenId()).isEqualTo(fotoId.valor());
    }

    @Test
    @DisplayName("La base rechaza una foto cuyo negocio no es el del lote")
    void shouldRejectFotoFromAnotherNegocio() {
        UUID negocioA = givenNegocio("a@example.com");
        UUID negocioB = givenNegocio("b@example.com");
        UUID loteDeA = givenLote(NegocioId.de(negocioA), null);

        assertThatThrownBy(() -> givenFoto(NegocioId.de(negocioB), LoteId.de(loteDeA), "negocios/b/intrusa.jpg"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("La base rechaza un registro que apunta a la foto de otro negocio")
    void shouldRejectRegistroWithFotoOrigenFromAnotherNegocio() {
        UUID negocioA = givenNegocio("a@example.com");
        UUID negocioB = givenNegocio("b@example.com");
        UUID fotoDeA = givenFoto(NegocioId.de(negocioA), LoteId
                .de(givenLote(NegocioId.de(negocioA), null)), "negocios/a/1.jpg");
        UUID bloqueDeB = givenBloque(NegocioId.de(negocioB), ProyectoId
                .de(givenProyecto(NegocioId.de(negocioB), "Proyecto de B")), "Ventas");

        assertThatThrownBy(() -> givenRegistro(NegocioId.de(negocioB), BloqueId.de(bloqueDeB), Map.of(),
                FotoId.de(fotoDeA)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("La base exige que una lectura tenga resultado o motivo de error, no ambos")
    void shouldRejectLecturaWithResultadoAndError() {
        NegocioId negocioId = NegocioId.de(givenNegocio("a@example.com"));
        LoteId loteId = LoteId.de(givenLote(negocioId, null));

        assertThatThrownBy(() -> givenLectura(negocioId, loteId, Map.of("tipoRegistro", "VENTAS"), "falló"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Borrar el lote borra sus fotos y deja los registros sin foto de origen")
    void shouldKeepRegistroWithoutFotoOrigenWhenLoteIsDeleted() {
        NegocioId negocioId = NegocioId.de(givenNegocio("a@example.com"));
        BloqueId bloqueId = BloqueId
                .de(givenBloque(negocioId, ProyectoId.de(givenProyecto(negocioId, "Proyecto")), "Ventas"));
        LoteId loteId = LoteId.de(givenLote(negocioId, bloqueId));
        FotoId fotoId = FotoId.de(givenFoto(negocioId, loteId, "negocios/a/1.jpg"));
        RegistroId registroId = RegistroId.de(givenRegistro(negocioId, bloqueId, Map.of(), fotoId));

        lotes.deleteById(loteId.valor());
        entityManager.flush();
        entityManager.clear();

        assertThat(fotos.findById(fotoId.valor())).isEmpty();
        RegistroEntity registro = registros.findById(registroId.valor()).orElseThrow();
        assertThat(registro.getFotoOrigenId()).isNull();
        assertThat(registro.getNegocioId()).isEqualTo(negocioId.valor());
    }

    @Test
    @DisplayName("La base rechaza una sección cuyo negocio no es el del proyecto")
    void shouldRejectSeccionWhoseNegocioIsNotTheProyectoOne() {
        UUID negocioA = givenNegocio("a@example.com");
        UUID negocioB = givenNegocio("b@example.com");
        UUID proyectoDeA = givenProyecto(NegocioId.de(negocioA), "Proyecto de A");

        assertThatThrownBy(() -> givenSeccion(NegocioId.de(negocioB), ProyectoId.de(proyectoDeA), "Vista general"))
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("Un bloque nuevo nace con los valores por defecto del frontend")
    void shouldPersistBloqueWithDefaultAttributes() {
        NegocioId negocioId = NegocioId.de(givenNegocio("a@example.com"));
        BloqueId bloqueId = BloqueId
                .de(givenBloque(negocioId, ProyectoId.de(givenProyecto(negocioId, "Proyecto")), "Notas"));
        entityManager.clear();

        BloqueEntity bloque = bloques.findById(bloqueId.valor()).orElseThrow();

        assertThat(bloque.getTipo().name()).isEqualTo("FORMULARIO");
        assertThat(bloque.getColor().name()).isEqualTo("CELESTE");
        assertThat(bloque.getAncho().name()).isEqualTo("MEDIO");
        assertThat(bloque.getEmpiezaFila()).isFalse();
        assertThat(bloque.getDireccionStock()).isNull();
    }

    @Test
    @DisplayName("La base rechaza un negocio con una sola coordenada")
    void shouldRejectNegocioWithOnlyLatitud() {
        UsuarioId duenioId = UsuarioId.de(givenUsuario("a@example.com"));

        assertThatThrownBy(() -> negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Sin longitud").duenioId(duenioId.valor()).latitud(new BigDecimal("-34.670300"))
                .creadoEn(AHORA).actualizadoEn(AHORA).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("La base rechaza un agregado de zona con menos de 3 comercios")
    void shouldRejectAgregadoWithFewerThanThreeComercios() {
        ZonaId zonaId = ZonaId.de(givenZona("Zona de prueba"));

        assertThatThrownBy(() -> givenAgregado(zonaId, "yerba", 2, 2))
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("Guarda un agregado de zona con 3 o más comercios")
    void shouldPersistAgregadoWithThreeComercios() {
        ZonaId zonaId = ZonaId.de(givenZona("Zona de prueba"));

        UUID agregadoId = givenAgregado(zonaId, "yerba", 3, 3);

        assertThat(agregadoId).isNotNull();
    }

    @Test
    @DisplayName("La base exige fecha de resolución sólo en los avisos aplicados o descartados")
    void shouldRequireResueltoEnOnlyForResolvedAvisos() {
        NegocioId negocioId = NegocioId.de(givenNegocio("a@example.com"));

        assertThatThrownBy(() -> givenAviso(negocioId, "stock:1", EstadoAviso.APLICADO, null))
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("Un aviso descartado guarda cuándo se descartó")
    void shouldPersistDescartadoAvisoWithResueltoEn() {
        NegocioId negocioId = NegocioId.de(givenNegocio("a@example.com"));

        AvisoId avisoId = AvisoId.de(givenAviso(negocioId, "stock:1", EstadoAviso.DESCARTADO, AHORA));
        entityManager.clear();

        assertThat(entityManager.find(AvisoEntity.class, avisoId.valor()).getEstado())
                .isEqualTo(EstadoAviso.DESCARTADO);
    }

    @Test
    @DisplayName("La base no admite dos avisos pendientes del mismo tipo sobre la misma entidad")
    void shouldRejectTwoPendingAvisosOfSameTipoAndEntidad() {
        NegocioId negocioId = NegocioId.de(givenNegocio("a@example.com"));
        givenAviso(negocioId, "stock:1:a", "registro:1", EstadoAviso.PENDIENTE, null);

        assertThatThrownBy(() -> givenAviso(negocioId, "stock:1:b", "registro:1", EstadoAviso.PENDIENTE, null))
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("Un aviso descartado no impide un pendiente nuevo sobre la misma entidad")
    void shouldAllowPendingAvisoWhenPreviousOneWasDiscarded() {
        NegocioId negocioId = NegocioId.de(givenNegocio("a@example.com"));
        givenAviso(negocioId, "stock:1:a", "registro:1", EstadoAviso.DESCARTADO, AHORA);

        AvisoId avisoId = AvisoId.de(givenAviso(negocioId, "stock:1:b", "registro:1", EstadoAviso.PENDIENTE, null));

        assertThat(avisoId).isNotNull();
    }

    // --- arrange (modelo del frontend) ---
    private UUID givenSeccion(NegocioId negocioId, ProyectoId proyectoId, String nombre) {
        SeccionEntity seccion = SeccionEntity.builder()
                .negocioId(negocioId.valor()).proyectoId(proyectoId.valor()).nombre(nombre).posicion((short) 0)
                .creadoEn(AHORA).actualizadoEn(AHORA).build();
        return persistirYVaciar(seccion).getId();
    }

    private UUID givenZona(String nombre) {
        ZonaEntity zona = ZonaEntity.builder()
                .nombre(nombre).localidad("San Justo").partido("La Matanza").provincia("Buenos Aires")
                .latitud(new BigDecimal("-34.670300")).longitud(new BigDecimal("-58.562800")).radioMetros(1500)
                .creadoEn(AHORA).actualizadoEn(AHORA).build();
        return persistirYVaciar(zona).getId();
    }

    private UUID givenAgregado(ZonaId zonaId, String producto, int comercios, int comerciosVenden) {
        AgregadoZonaProductoEntity agregado = AgregadoZonaProductoEntity.builder()
                .zonaId(Identificador.valorDe(zonaId)).productoNormalizado(producto).nombre(producto)
                .comercios(comercios)
                .comerciosVenden(comerciosVenden).precioMinimo(new BigDecimal("3500"))
                .precioPromedio(new BigDecimal("3900")).precioMaximo(new BigDecimal("4300"))
                .unidadesSemana(new BigDecimal("30")).periodoDesde(LocalDate.parse("2026-09-21"))
                .periodoHasta(LocalDate.parse("2026-10-18")).calculadoEn(AHORA).build();
        return persistirYVaciar(agregado).getId();
    }

    private UUID givenAviso(NegocioId negocioId, String huella, EstadoAviso estado, OffsetDateTime resueltoEn) {
        return givenAviso(negocioId, huella, "registro:1", estado, resueltoEn);
    }

    private UUID givenAviso(NegocioId negocioId, String huella, String entidad, EstadoAviso estado,
                            OffsetDateTime resueltoEn) {
        AvisoEntity aviso = AvisoEntity.builder()
                .negocioId(negocioId.valor()).tipo(TipoAviso.STOCK).grupo(GrupoAviso.STOCK).prioridad((short) 2)
                .entidad(entidad).huella(huella).titulo("Yerba por debajo del mínimo").detalle("Te quedan 4").estado(estado)
                .resueltoEn(resueltoEn).creadoEn(AHORA).actualizadoEn(AHORA).build();
        return persistirYVaciar(aviso).getId();
    }

    private <T> T persistirYVaciar(T entidad) {
        entityManager.persist(entidad);
        entityManager.flush();
        return entidad;
    }

    // --- arrange ---
    private UUID givenLote(NegocioId negocioId, BloqueId bloqueDestinoId) {
        return lotes.saveAndFlush(LoteSubidaEntity.builder()
                .negocioId(negocioId.valor()).bloqueDestinoId(Identificador.valorDe(bloqueDestinoId))
                        .estado(EstadoLote.PENDIENTE)
                .intentos((short) 0).creadoEn(AHORA).actualizadoEn(AHORA).build()).getId();
    }

    private UUID givenFoto(NegocioId negocioId, LoteId loteId, String clave) {
        return fotos.saveAndFlush(FotoCuadernoEntity.builder()
                .negocioId(negocioId.valor()).loteId(loteId.valor()).claveAlmacenamiento(clave)
                .tipoContenido("image/jpeg")
                .tamanioBytes(1024L).rotacion((short) 0).posicion((short) 0)
                .creadoEn(AHORA).actualizadoEn(AHORA).build()).getId();
    }

    private UUID givenLectura(NegocioId negocioId, LoteId loteId, Map<String, Object> resultado, String motivoError) {
        return lecturas.saveAndFlush(LecturaIaEntity.builder()
                .negocioId(negocioId.valor()).loteId(loteId.valor()).proveedor("simulado").modelo("lectura-de-ejemplo")
                .versionPrompt("v1").resultado(resultado).motivoError(motivoError).paginas((short) 1)
                .tokensEntrada(0).tokensSalida(0).costoEstimadoUsd(BigDecimal.ZERO).duracionMs(10)
                .creadoEn(AHORA).actualizadoEn(AHORA).build()).getId();
    }

    private UUID givenRegistro(NegocioId negocioId, BloqueId bloqueId, Map<String, Object> valores,
            FotoId fotoOrigenId) {
        return registros.saveAndFlush(RegistroEntity.builder()
                .negocioId(negocioId.valor()).bloqueId(bloqueId.valor()).valores(valores).fotoOrigenId(Identificador
                        .valorDe(fotoOrigenId))
                .creadoEn(AHORA).actualizadoEn(AHORA).build()).getId();
    }

    private UUID givenUsuario(String email) {
        return usuarios.saveAndFlush(UsuarioEntity.builder()
                .nombre("Persona").email(email).hashContrasenia("{bcrypt}x").estado(EstadoUsuario.ACTIVO)
                .creadoEn(AHORA).actualizadoEn(AHORA).build()).getId();
    }

    private UUID givenNegocio(String emailDuenio) {
        UsuarioId duenioId = UsuarioId.de(givenUsuario(emailDuenio));
        return negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio de " + emailDuenio).duenioId(duenioId.valor())
                .creadoEn(AHORA).actualizadoEn(AHORA).build()).getId();
    }

    private void givenPlantilla(String nombre, boolean activa) {
        plantillas.saveAndFlush(PlantillaEntity.builder()
                .nombre(nombre).descripcion("Descripción").estructura(Map.of("bloques", List.of())).activa(activa)
                .creadoEn(AHORA).actualizadoEn(AHORA).build());
    }

    private UUID givenProyecto(NegocioId negocioId, String nombre) {
        return proyectos.saveAndFlush(ProyectoEntity.builder()
                .negocioId(negocioId.valor()).nombre(nombre).creadoEn(AHORA).actualizadoEn(AHORA).build()).getId();
    }

    private UUID givenBloque(NegocioId negocioId, ProyectoId proyectoId, String nombre) {
        return bloques.saveAndFlush(BloqueEntity.builder()
                .negocioId(negocioId.valor()).proyectoId(proyectoId.valor()).nombre(nombre).posicion((short) 0)
                .creadoEn(AHORA).actualizadoEn(AHORA).build()).getId();
    }

    private UUID givenCampo(NegocioId negocioId, BloqueId bloqueId, String nombre, TipoCampo tipo,
            List<String> opciones) {
        return campos.saveAndFlush(CampoEntity.builder()
                .negocioId(negocioId.valor()).bloqueId(bloqueId.valor()).nombre(nombre).tipo(tipo).obligatorio(false)
                .opciones(opciones).posicion((short) 0).creadoEn(AHORA).actualizadoEn(AHORA).build()).getId();
    }

    private UUID givenRegistro(NegocioId negocioId, BloqueId bloqueId, Map<String, Object> valores) {
        return givenRegistro(negocioId, bloqueId, valores, null);
    }
}
