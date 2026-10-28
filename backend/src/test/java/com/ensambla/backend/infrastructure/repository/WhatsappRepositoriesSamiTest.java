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
import com.ensambla.backend.domain.model.whatsapp.VinculoPropuesto;
import com.ensambla.backend.domain.model.whatsapp.MensajeEntrante;
import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.OrigenPropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TipoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VerificacionWhatsapp;
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

    @Test
    @DisplayName("El mapper conserva los datos de la cola de un mensaje: reclamo, intentos, atención y error")
    void mapperShouldRoundTripQueueFields() {
        MensajeWhatsapp mensaje = givenMensaje();
        mensaje.reclamar(AHORA, Duration.ofMinutes(2));
        mensaje.fallar("se cayó", AHORA);

        assertThat(mapper.toDomain(mapper.toEntity(mensaje))).usingRecursiveComparison().isEqualTo(mensaje);
    }

    @Test
    @DisplayName("El mapper pasa la verificación de la entidad al dominio y de vuelta sin perder datos")
    void mapperShouldRoundTripVerificacion() {
        VerificacionWhatsapp verificacion = givenVerificacion();

        assertThat(mapper.toDomain(mapper.toEntity(verificacion))).usingRecursiveComparison()
                .isEqualTo(verificacion);
    }

    @Test
    @DisplayName("El mapper guarda las filas, las tablas ofrecidas y la lectura como JSON y las recupera iguales")
    void propuestaMapperShouldRoundTripJson() {
        PropuestaWhatsapp propuesta = PropuestaWhatsapp.eligiendoTabla(NEGOCIO_ID, "5491122334455",
                OrigenPropuestaWhatsapp.FOTO, "pie", givenLectura(), List.of(BLOQUE_ID), AHORA);
        propuesta.aPendiente(BLOQUE_ID, List.of(new FilaPropuesta(Map.of("c1", "5000"), Set.of("c1"))));

        PropuestaWhatsappEntity entity = propuestaMapper.toEntity(propuesta);
        PropuestaWhatsapp devuelta = propuestaMapper.toDomain(entity);

        assertThat(entity.getFilas()).contains("5000");
        assertThat(devuelta).usingRecursiveComparison().isEqualTo(propuesta);
    }

    @Test
    @DisplayName("Las filas con vínculos y con lo que había antes de sumar vuelven iguales del JSON")
    void propuestaMapperShouldRoundTripLinksAndPreviousValues() {
        PropuestaWhatsapp propuesta = PropuestaWhatsapp.pendiente(NEGOCIO_ID, "5491122334455",
                OrigenPropuestaWhatsapp.TEXTO, "vendí", BLOQUE_ID, List.of(new FilaPropuesta(Map.of("c1", "+10",
                        "c2", "Marta"), Set.of()).actualizando("r-1", "Leche", "c3", Map.of("c1", 5))
                        .vinculando("c2", new VinculoPropuesto("Marta", null, "b-2", "Clientes"))), AHORA);

        PropuestaWhatsapp devuelta = propuestaMapper.toDomain(propuestaMapper.toEntity(propuesta));

        assertThat(devuelta).usingRecursiveComparison().isEqualTo(propuesta);
    }

    @Test
    @DisplayName("Una fila guardada antes de los vínculos se lee sin ellos")
    void propuestaMapperShouldReadRowsSavedBeforeLinks() {
        PropuestaWhatsappEntity entity = propuestaMapper.toEntity(givenPropuestaDeTexto());
        entity.setFilas("[{\"valores\":{\"c1\":\"5000\"},\"dudosos\":[],\"registroId\":null,"
                + "\"nombreExistente\":null,\"campoNombre\":null}]");

        FilaPropuesta fila = propuestaMapper.toDomain(entity).getFilas().getFirst();

        assertThat(fila.valores()).containsEntry("c1", "5000");
        assertThat(fila.antes()).isEmpty();
        assertThat(fila.vinculos()).isEmpty();
    }

    @Test
    @DisplayName("Una propuesta de texto no tiene lectura y vuelve sin ella")
    void propuestaMapperShouldHandleMissingLectura() {
        PropuestaWhatsapp propuesta = givenPropuestaDeTexto();

        PropuestaWhatsappEntity entity = propuestaMapper.toEntity(propuesta);

        assertThat(entity.getLectura()).isNull();
        assertThat(propuestaMapper.toDomain(entity).getLectura()).isNull();
        assertThat(entity.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.PENDIENTE);
    }

    @Test
    @DisplayName("El repositorio de mensajes reclama el siguiente, lo deja atendiéndose y lista los vencidos")
    void mensajeRepositoryShouldClaimAndListExpired() {
        MensajeWhatsappEntity entity = mapper.toEntity(givenMensaje());
        when(mensajeJpa.findSiguienteRecibidoBloqueado()).thenReturn(Optional.of(entity), Optional.empty());
        when(mensajeJpa.saveAndFlush(any(MensajeWhatsappEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(mensajeJpa.findByEstadoAndReclamadoHastaBefore(EstadoMensajeWhatsapp.ATENDIENDO, AHORA))
                .thenReturn(List.of(entity));

        Optional<MensajeWhatsapp> reclamado = mensajes.reclamarSiguiente(AHORA, Duration.ofMinutes(2));

        assertThat(reclamado).get().satisfies(m -> {
            assertThat(m.getEstado()).isEqualTo(EstadoMensajeWhatsapp.ATENDIENDO);
            assertThat(m.getReclamadoHasta()).isEqualTo(AHORA.plusMinutes(2));
        });
        assertThat(mensajes.reclamarSiguiente(AHORA, Duration.ofMinutes(2))).isEmpty();
        assertThat(mensajes.vencidos(AHORA)).hasSize(1);
    }

    @Test
    @DisplayName("El repositorio de verificaciones guarda, busca, borra y cuenta los envíos a un número")
    void verificacionRepositoryShouldDelegate() {
        VerificacionWhatsapp verificacion = givenVerificacion();
        VerificacionWhatsappEntity entity = mapper.toEntity(verificacion);
        when(verificacionJpa.saveAndFlush(any(VerificacionWhatsappEntity.class))).thenReturn(entity);
        when(verificacionJpa.findByUsuarioId(USUARIO_ID.valor())).thenReturn(Optional.of(entity));
        when(verificacionJpa.countByTelefonoAndEnviadoEnGreaterThanEqual("5491122334455", AHORA)).thenReturn(2L);

        assertThat(verificaciones.save(verificacion).getTelefono()).isEqualTo("5491122334455");
        assertThat(verificaciones.findByUsuarioId(USUARIO_ID)).isPresent();
        verificaciones.deleteByUsuarioId(USUARIO_ID);
        assertThat(verificaciones.contarEnviosRecientesA("5491122334455", AHORA)).isEqualTo(2L);
        verify(verificacionJpa).deleteByUsuarioId(USUARIO_ID.valor());
    }

    @Test
    @DisplayName("El repositorio de propuestas guarda y busca la abierta de un teléfono")
    void propuestaRepositoryShouldDelegate() {
        PropuestaWhatsappEntity entity = propuestaMapper.toEntity(givenPropuestaDeTexto());
        when(propuestaJpa.saveAndFlush(any(PropuestaWhatsappEntity.class))).thenReturn(entity);
        when(propuestaJpa.findByTelefonoAndEstadoIn(any(), any())).thenReturn(Optional.of(entity));

        assertThat(propuestas.save(givenPropuestaDeTexto()).getBloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(propuestas.findAbiertaByTelefono("5491122334455")).isPresent();
    }

    private static VerificacionWhatsapp givenVerificacion() {
        return VerificacionWhatsapp.nueva(USUARIO_ID, new TelefonoWhatsapp("5491122334455"), "123456", AHORA);
    }

    private static PropuestaWhatsapp givenPropuestaDeTexto() {
        return PropuestaWhatsapp.pendiente(NEGOCIO_ID, "5491122334455", OrigenPropuestaWhatsapp.TEXTO, "gasté",
                BLOQUE_ID, List.of(), AHORA);
    }

    private static MensajeWhatsapp givenMensaje() {
        return MensajeWhatsapp.recibido(NEGOCIO_ID, new MensajeEntrante("5491122334455", "ID1",
                TipoMensajeWhatsapp.IMAGEN, "pie", "image/jpeg", null, AHORA));
    }

    private static LecturaCuaderno givenLectura() {
        return new LecturaCuaderno(TipoRegistroCuaderno.OTRO, new BloquePropuesto("Gastos", BLOQUE_ID,
                List.of(new CampoPropuesto("monto", "Monto", TipoCampo.MONEDA, List.of()))),
                List.of(new FilaLeida(1, 1, Map.of("monto", new DatoLeido("5000", 0.9)))), List.of("aviso"),
                new ConsumoIa("simulado", "demo", "v1", 1, 10, 20, 30));
    }
}
