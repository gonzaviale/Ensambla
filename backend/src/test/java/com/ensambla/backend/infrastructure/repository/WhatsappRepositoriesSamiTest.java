package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.BloquePropuesto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.ConsumoIa;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.TipoRegistroCuaderno;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.EstadoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.EstadoPropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.FilaPropuesta;
import com.ensambla.backend.domain.model.whatsapp.MensajeEntrante;
import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.OrigenPropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TipoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VerificacionWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoPropuesto;
import com.ensambla.backend.infrastructure.repository.entity.MensajeWhatsappEntity;
import com.ensambla.backend.infrastructure.repository.entity.PropuestaWhatsappEntity;
import com.ensambla.backend.infrastructure.repository.entity.VerificacionWhatsappEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.implementation.MensajeWhatsappRepositoryImpl;
import com.ensambla.backend.infrastructure.repository.jpaRepository.implementation.PropuestaWhatsappRepositoryImpl;
import com.ensambla.backend.infrastructure.repository.jpaRepository.implementation.VerificacionWhatsappRepositoryImpl;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IMensajeWhatsappJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IPropuestaWhatsappJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IVerificacionWhatsappJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.PropuestaWhatsappRepositoryMapper;
import com.ensambla.backend.infrastructure.repository.mapper.WhatsappRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WhatsappRepositoriesSamiTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-27T12:00:00Z");
    private static final String TELEFONO = "5491122334455";

    private final WhatsappRepositoryMapper mapper = new WhatsappRepositoryMapper();
    private final PropuestaWhatsappRepositoryMapper propuestaMapper =
            new PropuestaWhatsappRepositoryMapper(JsonMapper.builder().build());
    private final IMensajeWhatsappJpaRepository mensajeJpa = mock(IMensajeWhatsappJpaRepository.class);
    private final IVerificacionWhatsappJpaRepository verificacionJpa = mock(IVerificacionWhatsappJpaRepository.class);
    private final IPropuestaWhatsappJpaRepository propuestaJpa = mock(IPropuestaWhatsappJpaRepository.class);
    private final MensajeWhatsappRepositoryImpl mensajes = new MensajeWhatsappRepositoryImpl(mensajeJpa, mapper);
    private final VerificacionWhatsappRepositoryImpl verificaciones =
            new VerificacionWhatsappRepositoryImpl(verificacionJpa, mapper);
    private final PropuestaWhatsappRepositoryImpl propuestas =
            new PropuestaWhatsappRepositoryImpl(propuestaJpa, propuestaMapper);

    private MensajeWhatsapp mensaje;
    private PropuestaWhatsapp propuesta;
    private PropuestaWhatsappEntity entidadDePropuesta;
    private PropuestaWhatsapp propuestaDevuelta;
    private Optional<MensajeWhatsapp> reclamado;
    private List<MensajeWhatsapp> vencidos;
    private VerificacionWhatsapp verificacionGuardada;
    private Optional<VerificacionWhatsapp> verificacionEncontrada;
    private long enviosRecientes;
    private PropuestaWhatsapp propuestaGuardada;
    private Optional<PropuestaWhatsapp> propuestaAbierta;

    @Test
    @DisplayName("El mapper conserva los datos de la cola de un mensaje: reclamo, intentos, atención y error")
    void elMapperConservaLosDatosDeLaColaDeUnMensajeReclamoIntentosAtencionYError() {
        dadoUnMensajeReclamadoQueFallo();

        entoncesElMensajeIdaYVueltaNoPierdeDatos();
    }

    @Test
    @DisplayName("El mapper pasa la verificación de la entidad al dominio y de vuelta sin perder datos")
    void elMapperPasaLaVerificacionDeLaEntidadAlDominioYDeVueltaSinPerderDatos() {
        entoncesLaVerificacionIdaYVueltaNoPierdeDatos(unaVerificacion());
    }

    @Test
    @DisplayName("El mapper guarda las filas, las tablas ofrecidas y la lectura como JSON y las recupera iguales")
    void elMapperGuardaLasFilasLasTablasOfrecidasYLaLecturaComoJsonYLasRecuperaIguales() {
        dadoUnaPropuestaDeFotoConUnaFilaPendiente();

        cuandoSePasaLaPropuestaPorLaEntidadYDeVuelta();

        entoncesLaEntidadGuardaLasFilasYLaPropuestaVuelveIgual();
    }

    @Test
    @DisplayName("Las filas con vínculos y con lo que había antes de sumar vuelven iguales del JSON")
    void lasFilasConVinculosYConLoQueHabiaAntesDeSumarVuelvenIgualesDelJson() {
        dadoUnaPropuestaDeTextoConVinculosYValoresPrevios();

        cuandoSePasaLaPropuestaPorLaEntidadYDeVuelta();

        entoncesLaPropuestaVuelveIgual();
    }

    @Test
    @DisplayName("Una fila guardada antes de los vínculos se lee sin ellos")
    void unaFilaGuardadaAntesDeLosVinculosSeLeeSinEllos() {
        dadoUnaFilaGuardadaAntesDeLosVinculos();

        cuandoSeLeeLaEntidad();

        entoncesLaFilaSeLeeSinVinculosNiValoresPrevios();
    }

    @Test
    @DisplayName("Una propuesta de texto no tiene lectura y vuelve sin ella")
    void unaPropuestaDeTextoNoTieneLecturaYVuelveSinElla() {
        dadoUnaPropuestaDeTexto();

        cuandoSePasaLaPropuestaPorLaEntidadYDeVuelta();

        entoncesLaPropuestaNoTieneLecturaYEstaPendiente();
    }

    @Test
    @DisplayName("El repositorio de mensajes reclama el siguiente y lo deja atendiéndose")
    void elRepositorioDeMensajesReclamaElSiguienteYLoDejaAtendiendose() {
        dadoUnMensajeRecibidoPendienteDeAtencion();

        cuandoSeReclamaElSiguienteMensaje();

        entoncesElMensajeQuedaAtendiendoseConSuReclamo();
    }

    @Test
    @DisplayName("El repositorio de mensajes no reclama nada cuando no hay mensajes recibidos")
    void elRepositorioDeMensajesNoReclamaNadaCuandoNoHayMensajesRecibidos() {
        dadoQueNoHayMensajesRecibidos();

        cuandoSeReclamaElSiguienteMensaje();

        entoncesNoSeReclamaNada();
    }

    @Test
    @DisplayName("El repositorio de mensajes lista los vencidos")
    void elRepositorioDeMensajesListaLosVencidos() {
        dadoUnMensajeAtendiendoseConElReclamoVencido();

        cuandoSeListanLosMensajesVencidos();

        entoncesHayUnMensajeVencido();
    }

    @Test
    @DisplayName("El repositorio de verificaciones guarda y busca por usuario")
    void elRepositorioDeVerificacionesGuardaYBuscaPorUsuario() {
        dadoUnaVerificacionPersistida();

        cuandoSeGuardaYSeBuscaLaVerificacion();

        entoncesSeGuardaYSeEncuentraLaVerificacion();
    }

    @Test
    @DisplayName("El repositorio de verificaciones borra y cuenta los envíos a un número")
    void elRepositorioDeVerificacionesBorraYCuentaLosEnviosAUnNumero() {
        dadoQueSeEnviaronDosVerificacionesAlNumero();

        cuandoSeBorraLaVerificacionYSeCuentanLosEnvios();

        entoncesSeBorraEnElRepositorioJpaYSeCuentanDosEnvios();
    }

    @Test
    @DisplayName("El repositorio de propuestas guarda y busca la abierta de un teléfono")
    void elRepositorioDePropuestasGuardaYBuscaLaAbiertaDeUnTelefono() {
        dadoUnaPropuestaDeTextoPersistida();

        cuandoSeGuardaYSeBuscaLaPropuestaAbierta();

        entoncesSeGuardaYSeEncuentraLaPropuestaAbierta();
    }

    // --- dado ---

    private void dadoUnMensajeReclamadoQueFallo() {
        mensaje = unMensaje();
        mensaje.reclamar(AHORA, Duration.ofMinutes(2));
        mensaje.fallar("se cayó", AHORA);
    }

    private void dadoUnaPropuestaDeFotoConUnaFilaPendiente() {
        propuesta = PropuestaWhatsapp.eligiendoTabla(NEGOCIO_ID, TELEFONO, OrigenPropuestaWhatsapp.FOTO, "pie",
                unaLectura(), List.of(BLOQUE_ID), AHORA);
        propuesta.aPendiente(BLOQUE_ID, List.of(new FilaPropuesta(Map.of("c1", "5000"), Set.of("c1"))));
    }

    private void dadoUnaPropuestaDeTextoConVinculosYValoresPrevios() {
        FilaPropuesta fila = new FilaPropuesta(Map.of("c1", "+10", "c2", "Marta"), Set.of())
                .actualizando("r-1", "Leche", "c3", Map.of("c1", 5))
                .vinculando("c2", new VinculoPropuesto("Marta", null, "b-2", "Clientes"));
        propuesta = PropuestaWhatsapp.pendiente(NEGOCIO_ID, TELEFONO, OrigenPropuestaWhatsapp.TEXTO, "vendí",
                BLOQUE_ID, List.of(fila), AHORA);
    }

    private void dadoUnaPropuestaDeTexto() {
        propuesta = unaPropuestaDeTexto();
    }

    private void dadoUnaFilaGuardadaAntesDeLosVinculos() {
        entidadDePropuesta = propuestaMapper.toEntity(unaPropuestaDeTexto());
        entidadDePropuesta.setFilas("[{\"valores\":{\"c1\":\"5000\"},\"dudosos\":[],\"registroId\":null,"
                + "\"nombreExistente\":null,\"campoNombre\":null}]");
    }

    private void dadoUnMensajeRecibidoPendienteDeAtencion() {
        MensajeWhatsappEntity entidad = mapper.toEntity(unMensaje());
        when(mensajeJpa.findSiguienteRecibidoBloqueado()).thenReturn(Optional.of(entidad));
        when(mensajeJpa.saveAndFlush(any(MensajeWhatsappEntity.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void dadoQueNoHayMensajesRecibidos() {
        when(mensajeJpa.findSiguienteRecibidoBloqueado()).thenReturn(Optional.empty());
    }

    private void dadoUnMensajeAtendiendoseConElReclamoVencido() {
        when(mensajeJpa.findByEstadoAndReclamadoHastaBefore(EstadoMensajeWhatsapp.ATENDIENDO, AHORA))
                .thenReturn(List.of(mapper.toEntity(unMensaje())));
    }

    private void dadoUnaVerificacionPersistida() {
        VerificacionWhatsappEntity entidad = mapper.toEntity(unaVerificacion());
        when(verificacionJpa.saveAndFlush(any(VerificacionWhatsappEntity.class))).thenReturn(entidad);
        when(verificacionJpa.findByUsuarioId(USUARIO_ID.valor())).thenReturn(Optional.of(entidad));
    }

    private void dadoQueSeEnviaronDosVerificacionesAlNumero() {
        when(verificacionJpa.countByTelefonoAndEnviadoEnGreaterThanEqual(TELEFONO, AHORA)).thenReturn(2L);
    }

    private void dadoUnaPropuestaDeTextoPersistida() {
        PropuestaWhatsappEntity entidad = propuestaMapper.toEntity(unaPropuestaDeTexto());
        when(propuestaJpa.saveAndFlush(any(PropuestaWhatsappEntity.class))).thenReturn(entidad);
        when(propuestaJpa.findByTelefonoAndEstadoIn(any(), any())).thenReturn(Optional.of(entidad));
    }

    // --- cuando ---

    private void cuandoSePasaLaPropuestaPorLaEntidadYDeVuelta() {
        entidadDePropuesta = propuestaMapper.toEntity(propuesta);
        propuestaDevuelta = propuestaMapper.toDomain(entidadDePropuesta);
    }

    private void cuandoSeLeeLaEntidad() {
        propuestaDevuelta = propuestaMapper.toDomain(entidadDePropuesta);
    }

    private void cuandoSeReclamaElSiguienteMensaje() {
        reclamado = mensajes.reclamarSiguiente(AHORA, Duration.ofMinutes(2));
    }

    private void cuandoSeListanLosMensajesVencidos() {
        vencidos = mensajes.vencidos(AHORA);
    }

    private void cuandoSeGuardaYSeBuscaLaVerificacion() {
        verificacionGuardada = verificaciones.save(unaVerificacion());
        verificacionEncontrada = verificaciones.findByUsuarioId(USUARIO_ID);
    }

    private void cuandoSeBorraLaVerificacionYSeCuentanLosEnvios() {
        verificaciones.deleteByUsuarioId(USUARIO_ID);
        enviosRecientes = verificaciones.contarEnviosRecientesA(TELEFONO, AHORA);
    }

    private void cuandoSeGuardaYSeBuscaLaPropuestaAbierta() {
        propuestaGuardada = propuestas.save(unaPropuestaDeTexto());
        propuestaAbierta = propuestas.findAbiertaByTelefono(TELEFONO);
    }

    // --- entonces ---

    private void entoncesElMensajeIdaYVueltaNoPierdeDatos() {
        assertThat(mapper.toDomain(mapper.toEntity(mensaje))).usingRecursiveComparison().isEqualTo(mensaje);
    }

    private void entoncesLaVerificacionIdaYVueltaNoPierdeDatos(VerificacionWhatsapp verificacion) {
        assertThat(mapper.toDomain(mapper.toEntity(verificacion))).usingRecursiveComparison()
                .isEqualTo(verificacion);
    }

    private void entoncesLaEntidadGuardaLasFilasYLaPropuestaVuelveIgual() {
        assertThat(entidadDePropuesta.getFilas()).contains("5000");
        entoncesLaPropuestaVuelveIgual();
    }

    private void entoncesLaPropuestaVuelveIgual() {
        assertThat(propuestaDevuelta).usingRecursiveComparison().isEqualTo(propuesta);
    }

    private void entoncesLaFilaSeLeeSinVinculosNiValoresPrevios() {
        FilaPropuesta fila = propuestaDevuelta.getFilas().getFirst();
        assertThat(fila.valores()).containsEntry("c1", "5000");
        assertThat(fila.antes()).isEmpty();
        assertThat(fila.vinculos()).isEmpty();
    }

    private void entoncesLaPropuestaNoTieneLecturaYEstaPendiente() {
        assertThat(entidadDePropuesta.getLectura()).isNull();
        assertThat(propuestaDevuelta.getLectura()).isNull();
        assertThat(entidadDePropuesta.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.PENDIENTE);
    }

    private void entoncesElMensajeQuedaAtendiendoseConSuReclamo() {
        assertThat(reclamado).get().satisfies(mensajeReclamado -> {
            assertThat(mensajeReclamado.getEstado()).isEqualTo(EstadoMensajeWhatsapp.ATENDIENDO);
            assertThat(mensajeReclamado.getReclamadoHasta()).isEqualTo(AHORA.plusMinutes(2));
        });
    }

    private void entoncesNoSeReclamaNada() {
        assertThat(reclamado).isEmpty();
    }

    private void entoncesHayUnMensajeVencido() {
        assertThat(vencidos).hasSize(1);
    }

    private void entoncesSeGuardaYSeEncuentraLaVerificacion() {
        assertThat(verificacionGuardada.getTelefono()).isEqualTo(TELEFONO);
        assertThat(verificacionEncontrada).isPresent();
    }

    private void entoncesSeBorraEnElRepositorioJpaYSeCuentanDosEnvios() {
        assertThat(enviosRecientes).isEqualTo(2L);
        verify(verificacionJpa).deleteByUsuarioId(USUARIO_ID.valor());
    }

    private void entoncesSeGuardaYSeEncuentraLaPropuestaAbierta() {
        assertThat(propuestaGuardada.getBloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(propuestaAbierta).isPresent();
    }

    // --- datos ---

    private static VerificacionWhatsapp unaVerificacion() {
        return VerificacionWhatsapp.nueva(USUARIO_ID, new TelefonoWhatsapp(TELEFONO), "123456", AHORA);
    }

    private static PropuestaWhatsapp unaPropuestaDeTexto() {
        return PropuestaWhatsapp.pendiente(NEGOCIO_ID, TELEFONO, OrigenPropuestaWhatsapp.TEXTO, "gasté",
                BLOQUE_ID, List.of(), AHORA);
    }

    private static MensajeWhatsapp unMensaje() {
        return MensajeWhatsapp.recibido(NEGOCIO_ID, new MensajeEntrante(TELEFONO, "ID1",
                TipoMensajeWhatsapp.IMAGEN, "pie", "image/jpeg", null, AHORA));
    }

    private static LecturaCuaderno unaLectura() {
        return new LecturaCuaderno(TipoRegistroCuaderno.OTRO, new BloquePropuesto("Gastos", BLOQUE_ID,
                List.of(new CampoPropuesto("monto", "Monto", TipoCampo.MONEDA, List.of()))),
                List.of(new FilaLeida(1, 1, Map.of("monto", new DatoLeido("5000", 0.9)))), List.of("aviso"),
                new ConsumoIa("simulado", "demo", "v1", 1, 10, 20, 30));
    }
}
