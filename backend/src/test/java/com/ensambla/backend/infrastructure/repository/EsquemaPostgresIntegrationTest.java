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
import com.ensambla.backend.domain.model.precio.CambioPrecio;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.domain.repository.precio.HistorialPrecioRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Verifica que las migraciones (V1 a V10) y las entidades coinciden (el perfil usa ddl-auto=validate) y que las reglas de
 * aislamiento por negocio las hace cumplir la base. Cada test hace rollback.
 */
@Transactional
class EsquemaPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-09T12:00:00Z");
    private static final Map<String, Object> ESTRUCTURA_DE_PLANTILLA =
            Map.of("bloques", List.of(Map.of("nombre", "Clientes")));

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
    private HistorialPrecioRepository historialPrecio;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private NegocioId negocioId;
    private UsuarioId usuarioDelDuenio;
    private NegocioId negocioA;
    private NegocioId negocioB;
    private ProyectoId proyectoId;
    private BloqueId bloqueId;
    private CampoId campoId;
    private RegistroId registroId;
    private LoteId loteId;
    private FotoId fotoId;
    private UUID lecturaId;
    private ZonaId zonaId;
    private AvisoId avisoId;
    private PlantillaEntity plantilla;
    private RegistroEntity registro;
    private CampoEntity campo;
    private BloqueEntity bloque;
    private List<PlantillaEntity> plantillasActivas;
    private List<CambioPrecio> cambios;
    private Optional<UsuarioEntity> usuarioEncontrado;
    private Throwable rechazo;
    private UUID agregadoId;
    private int metricasInsertadas;
    private int puntosInsertados;
    private Integer cantidad;
    private Integer zonasDelSnapshot;
    private Integer metricasDelSnapshot;
    private Integer puntosDelSnapshot;

    @Test
    @DisplayName("Persiste y relee el grafo completo usuario → negocio → proyecto → bloque → campo → registro")
    void persisteYReleeElGrafoCompletoUsuarioNegocioProyectoBloqueCampoRegistro() {
        dadoUnGrafoConUnCampoDeOpcionesYUnRegistro();

        cuandoSeReleenElRegistroYElCampo();

        entoncesElRegistroConservaSuValor();
        entoncesElCampoConservaSusOpcionesYSuTipo();
    }

    @Test
    @DisplayName("Guarda y relee la estructura JSON de una plantilla")
    void guardaYReleeLaEstructuraJsonDeUnaPlantilla() {
        dadoUnaPlantillaConEstructura();

        cuandoSeReleeLaPlantilla();

        entoncesConservaLaEstructura();
    }

    @Test
    @DisplayName("Lista sólo las plantillas activas, ordenadas por nombre")
    void listaSoloLasPlantillasActivasOrdenadasPorNombre() {
        dadoPlantillasActivasEInactivas();

        cuandoSeListanLasPlantillasActivas();

        entoncesSoloSalenLasActivasOrdenadasPorNombre();
    }

    @Test
    @DisplayName("Encuentra al usuario por su email normalizado")
    void encuentraAlUsuarioPorSuEmailNormalizado() {
        dadoUnUsuarioConEmail("persona@example.com");

        cuandoSeBuscaAlUsuarioPorEmail("persona@example.com");

        entoncesSeEncuentraAlUsuario();
    }

    @Test
    @DisplayName("No encuentra usuarios con otro email")
    void noEncuentraUsuariosConOtroEmail() {
        dadoUnUsuarioConEmail("persona@example.com");

        cuandoSeBuscaAlUsuarioPorEmail("otra@example.com");

        entoncesNoSeEncuentraAlUsuario();
    }

    @Test
    @DisplayName("La base rechaza un bloque cuyo negocio no es el del proyecto")
    void laBaseRechazaUnBloqueCuyoNegocioNoEsElDelProyecto() {
        dadoUnProyectoDelNegocioA();

        cuandoElNegocioBIntentaCrearUnBloqueEnEseProyecto();

        entoncesLaBaseRechazaElDato();
    }

    @Test
    @DisplayName("La base rechaza un registro cuyo negocio no es el del bloque")
    void laBaseRechazaUnRegistroCuyoNegocioNoEsElDelBloque() {
        dadoUnBloqueDelNegocioA();

        cuandoElNegocioBIntentaCrearUnRegistroEnEseBloque();

        entoncesLaBaseRechazaElDato();
    }

    @Test
    @DisplayName("La base rechaza un campo de tipo OPCION sin opciones")
    void laBaseRechazaUnCampoDeTipoOpcionSinOpciones() {
        dadoUnBloqueDeUnNegocio();

        cuandoSeIntentaCrearUnCampoDeOpcionesSinOpciones();

        entoncesLaBaseRechazaElDato();
    }

    @Test
    @DisplayName("Borrar un proyecto borra en cascada sus bloques, campos y registros")
    void borrarUnProyectoBorraEnCascadaSusBloquesCamposYRegistros() {
        dadoUnProyectoConBloqueCampoYRegistro();

        cuandoSeBorraElProyecto();

        entoncesNoQuedanSusBloquesCamposNiRegistros();
    }

    @Test
    @DisplayName("Persiste un lote con su foto, su lectura y un registro vinculado a la foto de origen")
    void persisteUnLoteConSuFotoSuLecturaYUnRegistroVinculadoALaFotoDeOrigen() {
        dadoUnBloqueDeUnNegocio();

        cuandoSePersisteUnLoteConFotoLecturaYRegistroDeOrigen();

        entoncesSeReleenElLoteLaLecturaYElRegistroVinculado();
    }

    @Test
    @DisplayName("La base rechaza una foto cuyo negocio no es el del lote")
    void laBaseRechazaUnaFotoCuyoNegocioNoEsElDelLote() {
        dadoUnLoteDelNegocioA();

        cuandoElNegocioBIntentaSubirUnaFotoAEseLote();

        entoncesLaBaseRechazaElDato();
    }

    @Test
    @DisplayName("La base rechaza un registro que apunta a la foto de otro negocio")
    void laBaseRechazaUnRegistroQueApuntaALaFotoDeOtroNegocio() {
        dadoUnaFotoDelNegocioAYUnBloqueDelNegocioB();

        cuandoSeIntentaCrearUnRegistroDeBConLaFotoDeA();

        entoncesLaBaseRechazaElDato();
    }

    @Test
    @DisplayName("La base exige que una lectura tenga resultado o motivo de error, no ambos")
    void laBaseExigeQueUnaLecturaTengaResultadoOMotivoDeErrorNoAmbos() {
        dadoUnLoteSinBloqueDestino();

        cuandoSeIntentaGuardarUnaLecturaConResultadoYError();

        entoncesLaBaseRechazaElDato();
    }

    @Test
    @DisplayName("Borrar el lote borra sus fotos y deja los registros sin foto de origen")
    void borrarElLoteBorraSusFotosYDejaLosRegistrosSinFotoDeOrigen() {
        dadoUnLoteConUnaFotoYUnRegistroDeOrigen();

        cuandoSeBorraElLote();

        entoncesLaFotoDesapareceYElRegistroQuedaSinFotoDeOrigen();
    }

    @Test
    @DisplayName("La base rechaza una sección cuyo negocio no es el del proyecto")
    void laBaseRechazaUnaSeccionCuyoNegocioNoEsElDelProyecto() {
        dadoUnProyectoDelNegocioA();

        cuandoElNegocioBIntentaCrearUnaSeccionEnEseProyecto();

        entoncesLaPersistenciaRechazaElDato();
    }

    @Test
    @DisplayName("Un bloque nuevo nace con los valores por defecto del frontend")
    void unBloqueNuevoNaceConLosValoresPorDefectoDelFrontend() {
        dadoUnBloqueDeUnNegocio();

        cuandoSeReleeElBloque();

        entoncesElBloqueTieneLosValoresPorDefecto();
    }

    @Test
    @DisplayName("La base rechaza un negocio con una sola coordenada")
    void laBaseRechazaUnNegocioConUnaSolaCoordenada() {
        dadoUnDuenioSinNegocio();

        cuandoSeIntentaGuardarUnNegocioSoloConLatitud();

        entoncesLaBaseRechazaElDato();
    }

    @Test
    @DisplayName("La base rechaza un agregado de zona con menos de 3 comercios")
    void laBaseRechazaUnAgregadoDeZonaConMenosDeTresComercios() {
        dadoUnaZona("Zona de prueba");

        cuandoSeIntentaGuardarUnAgregadoConComercios(2);

        entoncesLaPersistenciaRechazaElDato();
    }

    @Test
    @DisplayName("Guarda un agregado de zona con 3 o más comercios")
    void guardaUnAgregadoDeZonaConTresOMasComercios() {
        dadoUnaZona("Zona de prueba");

        cuandoSeGuardaUnAgregadoConComercios(3);

        entoncesElAgregadoQuedaGuardado();
    }

    @Test
    @DisplayName("Registra fuentes trazables para los datos híbridos de zona")
    void registraFuentesTrazablesParaLosDatosHibridosDeZona() {
        cuandoSeContanLasFuentesHibridasDeZona();

        entoncesHaySeisFuentes();
    }

    @Test
    @DisplayName("Carga el snapshot inicial de CABA y La Matanza completo")
    void cargaElSnapshotInicialDeCabaYLaMatanzaCompleto() {
        cuandoSeContaElSnapshotInicialDeDondeAbrir();

        entoncesElSnapshotTrae13Zonas130MetricasYMasDeDosMilPuntos();
    }

    @Test
    @DisplayName("Guarda una métrica y un punto de interés trazables para una zona")
    void guardaUnaMetricaYUnPuntoDeInteresTrazablesParaUnaZona() {
        dadoUnaZona("Zona híbrida");

        cuandoSeGuardanUnaMetricaYUnPuntoDeInteres();

        entoncesSeGuardaUnaMetricaYUnPunto();
    }

    @Test
    @DisplayName("La base exige rubro sólo para métricas de competencia")
    void laBaseExigeRubroSoloParaMetricasDeCompetencia() {
        dadoUnaZona("Zona con competencia");

        cuandoSeIntentaGuardarUnaMetricaDeCompetenciaSinRubro();

        entoncesLaBaseRechazaElDato();
    }

    @Test
    @DisplayName("La base exige fecha de resolución sólo en los avisos aplicados o descartados")
    void laBaseExigeFechaDeResolucionSoloEnLosAvisosAplicadosODescartados() {
        dadoUnNegocio();

        cuandoSeIntentaGuardarUnAvisoAplicadoSinFechaDeResolucion();

        entoncesLaPersistenciaRechazaElDato();
    }

    @Test
    @DisplayName("Un aviso descartado guarda cuándo se descartó")
    void unAvisoDescartadoGuardaCuandoSeDescarto() {
        dadoUnNegocio();

        cuandoSeGuardaUnAvisoDescartado();

        entoncesElAvisoQuedaDescartado();
    }

    @Test
    @DisplayName("La base no admite dos avisos pendientes del mismo tipo sobre la misma entidad")
    void laBaseNoAdmiteDosAvisosPendientesDelMismoTipoSobreLaMismaEntidad() {
        dadoUnNegocio();
        dadoUnAvisoPendienteSobreElRegistro();

        cuandoSeIntentaGuardarOtroAvisoPendienteSobreElMismoRegistro();

        entoncesLaPersistenciaRechazaElDato();
    }

    @Test
    @DisplayName("Un aviso descartado no impide un pendiente nuevo sobre la misma entidad")
    void unAvisoDescartadoNoImpideUnPendienteNuevoSobreLaMismaEntidad() {
        dadoUnNegocio();
        dadoUnAvisoDescartadoSobreElRegistro();

        cuandoSeGuardaOtroAvisoPendienteSobreElMismoRegistro();

        entoncesElAvisoPendienteQuedaGuardado();
    }

    @Test
    @DisplayName("La base admite avisos de revisar el precio")
    void laBaseAdmiteAvisosDeRevisarElPrecio() {
        dadoUnNegocio();

        cuandoSeGuardaUnAvisoDeRevisarElPrecio();

        entoncesElAvisoQuedaGuardado();
    }

    @Test
    @DisplayName("El historial de precio devuelve los cambios del período del más viejo al más nuevo")
    void elHistorialDePrecioDevuelveLosCambiosDelPeriodoDelMasViejoAlMasNuevo() {
        dadoUnProductoConTresCambiosDePrecio();

        cuandoSeConsultanLosCambiosDeLosUltimosTreintaDias();

        entoncesSalenLosDosCambiosDelPeriodoEnOrden();
        entoncesElPrimerCambioConservaSuPrecioAnteriorYSuProducto();
    }

    @Test
    @DisplayName("La base no admite un precio nuevo que no sea positivo")
    void laBaseNoAdmiteUnPrecioNuevoQueNoSeaPositivo() {
        dadoUnProductoConPrecio();

        cuandoSeIntentaGuardarUnCambioDePrecioEnCero();

        entoncesLaBaseRechazaElDato();
    }

    // --- dado ---

    private void dadoUnNegocio() {
        negocioId = insertarNegocio("a@example.com");
    }

    private void dadoUnGrafoConUnCampoDeOpcionesYUnRegistro() {
        negocioId = insertarNegocio("dueña@example.com");
        proyectoId = insertarProyecto(negocioId, "Mi inventario");
        bloqueId = insertarBloque(negocioId, proyectoId, "Productos");
        campoId = insertarCampo(negocioId, bloqueId, "Rubro", TipoCampo.OPCION, List.of("Velas", "Moldes"));
        registroId = insertarRegistro(negocioId, bloqueId, Map.of(campoId.toString(), "Velas"));
        entityManager.clear();
    }

    private void dadoUnaPlantillaConEstructura() {
        plantilla = plantillas.saveAndFlush(PlantillaEntity.builder()
                .nombre("Clientes de prueba").descripcion("Tu agenda").estructura(ESTRUCTURA_DE_PLANTILLA)
                .activa(true).creadoEn(AHORA).actualizadoEn(AHORA).build());
        entityManager.clear();
    }

    private void dadoPlantillasActivasEInactivas() {
        plantillas.deleteAllInBatch();
        insertarPlantilla("Pedidos", true);
        insertarPlantilla("Clientes", true);
        insertarPlantilla("Vieja", false);
    }

    private void dadoUnUsuarioConEmail(String email) {
        insertarUsuario(email);
    }

    private void dadoDosNegocios() {
        negocioA = insertarNegocio("a@example.com");
        negocioB = insertarNegocio("b@example.com");
    }

    private void dadoUnProyectoDelNegocioA() {
        dadoDosNegocios();
        proyectoId = insertarProyecto(negocioA, "Proyecto de A");
    }

    private void dadoUnBloqueDelNegocioA() {
        dadoUnProyectoDelNegocioA();
        bloqueId = insertarBloque(negocioA, proyectoId, "Productos");
    }

    private void dadoUnBloqueDeUnNegocio() {
        dadoUnNegocio();
        proyectoId = insertarProyecto(negocioId, "Proyecto");
        bloqueId = insertarBloque(negocioId, proyectoId, "Productos");
    }

    private void dadoUnProyectoConBloqueCampoYRegistro() {
        dadoUnBloqueDeUnNegocio();
        campoId = insertarCampo(negocioId, bloqueId, "Nombre", TipoCampo.TEXTO, List.of());
        registroId = insertarRegistro(negocioId, bloqueId, Map.of(campoId.toString(), "Vela"));
    }

    private void dadoUnLoteDelNegocioA() {
        dadoDosNegocios();
        loteId = insertarLote(negocioA, null);
    }

    private void dadoUnaFotoDelNegocioAYUnBloqueDelNegocioB() {
        dadoDosNegocios();
        fotoId = insertarFoto(negocioA, insertarLote(negocioA, null), "negocios/a/1.jpg");
        proyectoId = insertarProyecto(negocioB, "Proyecto de B");
        bloqueId = insertarBloque(negocioB, proyectoId, "Ventas");
    }

    private void dadoUnLoteSinBloqueDestino() {
        dadoUnNegocio();
        loteId = insertarLote(negocioId, null);
    }

    private void dadoUnLoteConUnaFotoYUnRegistroDeOrigen() {
        dadoUnBloqueDeUnNegocio();
        loteId = insertarLote(negocioId, bloqueId);
        fotoId = insertarFoto(negocioId, loteId, "negocios/a/1.jpg");
        registroId = insertarRegistro(negocioId, bloqueId, Map.of(), fotoId);
    }

    private void dadoUnDuenioSinNegocio() {
        usuarioDelDuenio = insertarUsuario("a@example.com");
    }

    private void dadoUnaZona(String nombre) {
        zonaId = ZonaId.de(persistirYVaciar(ZonaEntity.builder()
                .nombre(nombre).localidad("San Justo").partido("La Matanza").provincia("Buenos Aires")
                .latitud(new BigDecimal("-34.670300")).longitud(new BigDecimal("-58.562800")).radioMetros(1500)
                .creadoEn(AHORA).actualizadoEn(AHORA).build()).getId());
    }

    private void dadoUnAvisoPendienteSobreElRegistro() {
        insertarAviso(negocioId, "stock:1:a", "registro:1", EstadoAviso.PENDIENTE, null);
    }

    private void dadoUnAvisoDescartadoSobreElRegistro() {
        insertarAviso(negocioId, "stock:1:a", "registro:1", EstadoAviso.DESCARTADO, AHORA);
    }

    private void dadoUnProductoConPrecio() {
        dadoUnNegocio();
        proyectoId = insertarProyecto(negocioId, "Almacén");
        bloqueId = insertarBloque(negocioId, proyectoId, "Inventario");
        registroId = insertarRegistro(negocioId, bloqueId, Map.of());
    }

    private void dadoUnProductoConTresCambiosDePrecio() {
        dadoUnProductoConPrecio();
        historialPrecio.save(new CambioPrecio(negocioId, registroId, null, new BigDecimal("900"),
                AHORA.minusDays(40)));
        historialPrecio.save(new CambioPrecio(negocioId, registroId, new BigDecimal("1200"),
                new BigDecimal("1500"), AHORA.minusDays(2)));
        historialPrecio.save(new CambioPrecio(negocioId, registroId, new BigDecimal("900"),
                new BigDecimal("1200"), AHORA.minusDays(10)));
        entityManager.clear();
    }

    // --- cuando ---

    private void cuandoSeReleenElRegistroYElCampo() {
        registro = registros.findById(registroId.valor()).orElseThrow();
        campo = campos.findById(campoId.valor()).orElseThrow();
    }

    private void cuandoSeReleeLaPlantilla() {
        plantilla = plantillas.findById(plantilla.getId()).orElseThrow();
    }

    private void cuandoSeListanLasPlantillasActivas() {
        plantillasActivas = plantillas.findByActivaTrueOrderByNombreAscIdAsc();
    }

    private void cuandoSeBuscaAlUsuarioPorEmail(String email) {
        usuarioEncontrado = usuarios.findByEmail(email);
    }

    private void cuandoElNegocioBIntentaCrearUnBloqueEnEseProyecto() {
        rechazo = catchThrowable(() -> insertarBloque(negocioB, proyectoId, "Intruso"));
    }

    private void cuandoElNegocioBIntentaCrearUnRegistroEnEseBloque() {
        rechazo = catchThrowable(() -> insertarRegistro(negocioB, bloqueId, Map.of()));
    }

    private void cuandoSeIntentaCrearUnCampoDeOpcionesSinOpciones() {
        rechazo = catchThrowable(() -> insertarCampo(negocioId, bloqueId, "Rubro", TipoCampo.OPCION, List.of()));
    }

    private void cuandoSeBorraElProyecto() {
        proyectos.deleteById(proyectoId.valor());
        entityManager.flush();
        entityManager.clear();
    }

    private void cuandoSePersisteUnLoteConFotoLecturaYRegistroDeOrigen() {
        loteId = insertarLote(negocioId, bloqueId);
        fotoId = insertarFoto(negocioId, loteId, "negocios/a/lote/1.jpg");
        lecturaId = insertarLectura(negocioId, loteId, Map.of("tipoRegistro", "VENTAS"), null);
        registroId = insertarRegistro(negocioId, bloqueId, Map.of(), fotoId);
        entityManager.clear();
    }

    private void cuandoElNegocioBIntentaSubirUnaFotoAEseLote() {
        rechazo = catchThrowable(() -> insertarFoto(negocioB, loteId, "negocios/b/intrusa.jpg"));
    }

    private void cuandoSeIntentaCrearUnRegistroDeBConLaFotoDeA() {
        rechazo = catchThrowable(() -> insertarRegistro(negocioB, bloqueId, Map.of(), fotoId));
    }

    private void cuandoSeIntentaGuardarUnaLecturaConResultadoYError() {
        rechazo = catchThrowable(() -> insertarLectura(negocioId, loteId, Map.of("tipoRegistro", "VENTAS"), "falló"));
    }

    private void cuandoSeBorraElLote() {
        lotes.deleteById(loteId.valor());
        entityManager.flush();
        entityManager.clear();
    }

    private void cuandoElNegocioBIntentaCrearUnaSeccionEnEseProyecto() {
        rechazo = catchThrowable(() -> insertarSeccion(negocioB, proyectoId, "Vista general"));
    }

    private void cuandoSeReleeElBloque() {
        entityManager.clear();
        bloque = bloques.findById(bloqueId.valor()).orElseThrow();
    }

    private void cuandoSeIntentaGuardarUnNegocioSoloConLatitud() {
        rechazo = catchThrowable(() -> negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Sin longitud").duenioId(usuarioDelDuenio.valor()).latitud(new BigDecimal("-34.670300"))
                .creadoEn(AHORA).actualizadoEn(AHORA).build()));
    }

    private void cuandoSeIntentaGuardarUnAgregadoConComercios(int comercios) {
        rechazo = catchThrowable(() -> insertarAgregado(comercios));
    }

    private void cuandoSeGuardaUnAgregadoConComercios(int comercios) {
        agregadoId = insertarAgregado(comercios);
    }

    private void cuandoSeContanLasFuentesHibridasDeZona() {
        cantidad = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM fuente_dato_zona
                WHERE codigo IN ('ENSAMBLA', 'OSM', 'CABA_DATOS', 'PBA_DATOS', 'INDEC_CENSO_2022', 'USUARIO')
                """, Integer.class);
    }

    private void cuandoSeContaElSnapshotInicialDeDondeAbrir() {
        zonasDelSnapshot = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM zona
                WHERE nombre IN ('Almagro', 'Boedo', 'Caballito Norte', 'Caballito Sur', 'Chacarita', 'Colegiales',
                    'Villa Ortúzar', 'Paternal', 'Villa Crespo Sur', 'Villa del Parque', 'Flores Norte',
                    'Ramos Mejía', 'San Justo - UNLaM')
                """, Integer.class);
        metricasDelSnapshot = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM metrica_zona WHERE fuente_codigo = 'OSM'", Integer.class);
        puntosDelSnapshot = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM punto_interes_zona WHERE fuente_codigo = 'OSM'", Integer.class);
    }

    private void cuandoSeGuardanUnaMetricaYUnPuntoDeInteres() {
        metricasInsertadas = jdbcTemplate.update("""
                INSERT INTO metrica_zona
                    (zona_id, tipo, valor, unidad, fuente_codigo, observada_en, confianza)
                VALUES (?, 'TRANSPORTE', 7, 'lugares_500m', 'OSM', DATE '2026-11-10', 80)
                """, zonaId.valor());
        puntosInsertados = jdbcTemplate.update("""
                INSERT INTO punto_interes_zona
                    (zona_id, clave_externa, tipo, nombre, latitud, longitud, fuente_codigo, observada_en)
                VALUES (?, 'node/1', 'TRANSPORTE', 'Estación de prueba', -34.67, -58.56, 'OSM', DATE '2026-11-10')
                """, zonaId.valor());
    }

    private void cuandoSeIntentaGuardarUnaMetricaDeCompetenciaSinRubro() {
        rechazo = catchThrowable(() -> jdbcTemplate.update("""
                INSERT INTO metrica_zona
                    (zona_id, tipo, valor, unidad, fuente_codigo, observada_en)
                VALUES (?, 'COMPETENCIA', 3, 'comercios_500m', 'OSM', DATE '2026-11-10')
                """, zonaId.valor()));
    }

    private void cuandoSeIntentaGuardarUnAvisoAplicadoSinFechaDeResolucion() {
        rechazo = catchThrowable(() -> insertarAviso(negocioId, "stock:1", "registro:1", EstadoAviso.APLICADO, null));
    }

    private void cuandoSeGuardaUnAvisoDescartado() {
        avisoId = insertarAviso(negocioId, "stock:1", "registro:1", EstadoAviso.DESCARTADO, AHORA);
        entityManager.clear();
    }

    private void cuandoSeIntentaGuardarOtroAvisoPendienteSobreElMismoRegistro() {
        rechazo = catchThrowable(() -> insertarAviso(negocioId, "stock:1:b", "registro:1",
                EstadoAviso.PENDIENTE, null));
    }

    private void cuandoSeGuardaOtroAvisoPendienteSobreElMismoRegistro() {
        avisoId = insertarAviso(negocioId, "stock:1:b", "registro:1", EstadoAviso.PENDIENTE, null);
    }

    private void cuandoSeGuardaUnAvisoDeRevisarElPrecio() {
        avisoId = AvisoId.de(persistirYVaciar(AvisoEntity.builder()
                .negocioId(negocioId.valor()).tipo(TipoAviso.REVISAR_PRECIO).grupo(GrupoAviso.VENTAS)
                .prioridad((short) 2).entidad("registro:1").huella("revisar:1").titulo("Yerba vende menos")
                .detalle("Revisá el precio").estado(EstadoAviso.PENDIENTE).creadoEn(AHORA).actualizadoEn(AHORA)
                .build()).getId());
    }

    private void cuandoSeConsultanLosCambiosDeLosUltimosTreintaDias() {
        cambios = historialPrecio.findByNegocioIdDesde(negocioId, AHORA.minusDays(30));
    }

    private void cuandoSeIntentaGuardarUnCambioDePrecioEnCero() {
        rechazo = catchThrowable(() -> historialPrecio.save(
                new CambioPrecio(negocioId, registroId, null, BigDecimal.ZERO, AHORA)));
    }

    // --- entonces ---

    private void entoncesElRegistroConservaSuValor() {
        assertThat(registro.getValores()).containsEntry(campoId.toString(), "Velas");
    }

    private void entoncesElCampoConservaSusOpcionesYSuTipo() {
        assertThat(campo.getOpciones()).containsExactly("Velas", "Moldes");
        assertThat(campo.getTipo()).isEqualTo(TipoCampo.OPCION);
    }

    private void entoncesConservaLaEstructura() {
        assertThat(plantilla.getEstructura()).isEqualTo(ESTRUCTURA_DE_PLANTILLA);
    }

    private void entoncesSoloSalenLasActivasOrdenadasPorNombre() {
        assertThat(plantillasActivas).extracting(PlantillaEntity::getNombre).containsExactly("Clientes", "Pedidos");
    }

    private void entoncesSeEncuentraAlUsuario() {
        assertThat(usuarioEncontrado).isPresent();
    }

    private void entoncesNoSeEncuentraAlUsuario() {
        assertThat(usuarioEncontrado).isEmpty();
    }

    private void entoncesLaBaseRechazaElDato() {
        assertThat(rechazo).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void entoncesLaPersistenciaRechazaElDato() {
        assertThat(rechazo).isInstanceOf(PersistenceException.class);
    }

    private void entoncesNoQuedanSusBloquesCamposNiRegistros() {
        assertThat(bloques.findById(bloqueId.valor())).isEmpty();
        assertThat(campos.findById(campoId.valor())).isEmpty();
        assertThat(registros.findById(registroId.valor())).isEmpty();
    }

    private void entoncesSeReleenElLoteLaLecturaYElRegistroVinculado() {
        assertThat(lotes.findById(loteId.valor()).orElseThrow().getEstado()).isEqualTo(EstadoLote.PENDIENTE);
        assertThat(lecturas.findById(lecturaId).orElseThrow().getResultado())
                .containsEntry("tipoRegistro", "VENTAS");
        assertThat(registros.findById(registroId.valor()).orElseThrow().getFotoOrigenId())
                .isEqualTo(fotoId.valor());
    }

    private void entoncesLaFotoDesapareceYElRegistroQuedaSinFotoDeOrigen() {
        assertThat(fotos.findById(fotoId.valor())).isEmpty();
        RegistroEntity conservado = registros.findById(registroId.valor()).orElseThrow();
        assertThat(conservado.getFotoOrigenId()).isNull();
        assertThat(conservado.getNegocioId()).isEqualTo(negocioId.valor());
    }

    private void entoncesElBloqueTieneLosValoresPorDefecto() {
        assertThat(bloque.getTipo().name()).isEqualTo("FORMULARIO");
        assertThat(bloque.getColor().name()).isEqualTo("CELESTE");
        assertThat(bloque.getAncho().name()).isEqualTo("MEDIO");
        assertThat(bloque.getEmpiezaFila()).isFalse();
        assertThat(bloque.getDireccionStock()).isNull();
    }

    private void entoncesElAgregadoQuedaGuardado() {
        assertThat(agregadoId).isNotNull();
    }

    private void entoncesHaySeisFuentes() {
        assertThat(cantidad).isEqualTo(6);
    }

    private void entoncesElSnapshotTrae13Zonas130MetricasYMasDeDosMilPuntos() {
        assertThat(zonasDelSnapshot).isEqualTo(13);
        assertThat(metricasDelSnapshot).isEqualTo(130);
        assertThat(puntosDelSnapshot).isGreaterThan(2_000);
    }

    private void entoncesSeGuardaUnaMetricaYUnPunto() {
        assertThat(metricasInsertadas).isOne();
        assertThat(puntosInsertados).isOne();
    }

    private void entoncesElAvisoQuedaDescartado() {
        assertThat(entityManager.find(AvisoEntity.class, avisoId.valor()).getEstado())
                .isEqualTo(EstadoAviso.DESCARTADO);
    }

    private void entoncesElAvisoPendienteQuedaGuardado() {
        assertThat(avisoId).isNotNull();
    }

    private void entoncesElAvisoQuedaGuardado() {
        assertThat(avisoId).isNotNull();
    }

    private void entoncesSalenLosDosCambiosDelPeriodoEnOrden() {
        assertThat(cambios).extracting(CambioPrecio::nuevo)
                .containsExactly(new BigDecimal("1200.00"), new BigDecimal("1500.00"));
    }

    private void entoncesElPrimerCambioConservaSuPrecioAnteriorYSuProducto() {
        assertThat(cambios.getFirst().anterior()).isEqualByComparingTo("900");
        assertThat(cambios.getFirst().registroId()).isEqualTo(registroId);
    }

    // --- datos ---

    private UsuarioId insertarUsuario(String email) {
        return UsuarioId.de(usuarios.saveAndFlush(UsuarioEntity.builder()
                .nombre("Persona").email(email).hashContrasenia("{bcrypt}x").estado(EstadoUsuario.ACTIVO)
                .creadoEn(AHORA).actualizadoEn(AHORA).build()).getId());
    }

    private NegocioId insertarNegocio(String emailDuenio) {
        UsuarioId duenioId = insertarUsuario(emailDuenio);
        return NegocioId.de(negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio de " + emailDuenio).duenioId(duenioId.valor())
                .creadoEn(AHORA).actualizadoEn(AHORA).build()).getId());
    }

    private void insertarPlantilla(String nombre, boolean activa) {
        plantillas.saveAndFlush(PlantillaEntity.builder()
                .nombre(nombre).descripcion("Descripción").estructura(Map.of("bloques", List.of())).activa(activa)
                .creadoEn(AHORA).actualizadoEn(AHORA).build());
    }

    private ProyectoId insertarProyecto(NegocioId negocio, String nombre) {
        return ProyectoId.de(proyectos.saveAndFlush(ProyectoEntity.builder()
                .negocioId(negocio.valor()).nombre(nombre).creadoEn(AHORA).actualizadoEn(AHORA).build()).getId());
    }

    private BloqueId insertarBloque(NegocioId negocio, ProyectoId proyecto, String nombre) {
        return BloqueId.de(bloques.saveAndFlush(BloqueEntity.builder()
                .negocioId(negocio.valor()).proyectoId(proyecto.valor()).nombre(nombre).posicion((short) 0)
                .creadoEn(AHORA).actualizadoEn(AHORA).build()).getId());
    }

    private CampoId insertarCampo(NegocioId negocio, BloqueId bloquePropio, String nombre, TipoCampo tipo,
            List<String> opciones) {
        return CampoId.de(campos.saveAndFlush(CampoEntity.builder()
                .negocioId(negocio.valor()).bloqueId(bloquePropio.valor()).nombre(nombre).tipo(tipo)
                .obligatorio(false).opciones(opciones).posicion((short) 0)
                .creadoEn(AHORA).actualizadoEn(AHORA).build()).getId());
    }

    private RegistroId insertarRegistro(NegocioId negocio, BloqueId bloquePropio, Map<String, Object> valores) {
        return insertarRegistro(negocio, bloquePropio, valores, null);
    }

    private RegistroId insertarRegistro(NegocioId negocio, BloqueId bloquePropio, Map<String, Object> valores,
            FotoId fotoOrigen) {
        return RegistroId.de(registros.saveAndFlush(RegistroEntity.builder()
                .negocioId(negocio.valor()).bloqueId(bloquePropio.valor()).valores(valores)
                .fotoOrigenId(Identificador.valorDe(fotoOrigen))
                .creadoEn(AHORA).actualizadoEn(AHORA).build()).getId());
    }

    private LoteId insertarLote(NegocioId negocio, BloqueId bloqueDestino) {
        return LoteId.de(lotes.saveAndFlush(LoteSubidaEntity.builder()
                .negocioId(negocio.valor()).bloqueDestinoId(Identificador.valorDe(bloqueDestino))
                .estado(EstadoLote.PENDIENTE).intentos((short) 0).creadoEn(AHORA).actualizadoEn(AHORA)
                .build()).getId());
    }

    private FotoId insertarFoto(NegocioId negocio, LoteId lote, String clave) {
        return FotoId.de(fotos.saveAndFlush(FotoCuadernoEntity.builder()
                .negocioId(negocio.valor()).loteId(lote.valor()).claveAlmacenamiento(clave)
                .tipoContenido("image/jpeg").tamanioBytes(1024L).rotacion((short) 0).posicion((short) 0)
                .creadoEn(AHORA).actualizadoEn(AHORA).build()).getId());
    }

    private UUID insertarLectura(NegocioId negocio, LoteId lote, Map<String, Object> resultado,
            String motivoError) {
        return lecturas.saveAndFlush(LecturaIaEntity.builder()
                .negocioId(negocio.valor()).loteId(lote.valor()).proveedor("simulado").modelo("lectura-de-ejemplo")
                .versionPrompt("v1").resultado(resultado).motivoError(motivoError).paginas((short) 1)
                .tokensEntrada(0).tokensSalida(0).costoEstimadoUsd(BigDecimal.ZERO).duracionMs(10)
                .creadoEn(AHORA).actualizadoEn(AHORA).build()).getId();
    }

    private UUID insertarSeccion(NegocioId negocio, ProyectoId proyecto, String nombre) {
        return persistirYVaciar(SeccionEntity.builder()
                .negocioId(negocio.valor()).proyectoId(proyecto.valor()).nombre(nombre).posicion((short) 0)
                .creadoEn(AHORA).actualizadoEn(AHORA).build()).getId();
    }

    private UUID insertarAgregado(int comercios) {
        return persistirYVaciar(AgregadoZonaProductoEntity.builder()
                .zonaId(Identificador.valorDe(zonaId)).productoNormalizado("yerba").nombre("yerba")
                .comercios(comercios).comerciosVenden(comercios).precioMinimo(new BigDecimal("3500"))
                .precioPromedio(new BigDecimal("3900")).precioMaximo(new BigDecimal("4300"))
                .unidadesSemana(new BigDecimal("30")).periodoDesde(LocalDate.parse("2026-09-21"))
                .periodoHasta(LocalDate.parse("2026-10-18")).calculadoEn(AHORA).build()).getId();
    }

    private AvisoId insertarAviso(NegocioId negocio, String huella, String entidad, EstadoAviso estado,
            OffsetDateTime resueltoEn) {
        return AvisoId.de(persistirYVaciar(AvisoEntity.builder()
                .negocioId(negocio.valor()).tipo(TipoAviso.STOCK).grupo(GrupoAviso.STOCK).prioridad((short) 2)
                .entidad(entidad).huella(huella).titulo("Yerba por debajo del mínimo").detalle("Te quedan 4")
                .estado(estado).resueltoEn(resueltoEn).creadoEn(AHORA).actualizadoEn(AHORA).build()).getId());
    }

    private <T> T persistirYVaciar(T entidad) {
        entityManager.persist(entidad);
        entityManager.flush();
        return entidad;
    }
}
