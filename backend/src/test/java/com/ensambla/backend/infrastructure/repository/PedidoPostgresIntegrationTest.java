package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pedido.ClienteWhatsapp;
import com.ensambla.backend.domain.model.pedido.Comercio;
import com.ensambla.backend.domain.model.pedido.ConversacionCliente;
import com.ensambla.backend.domain.model.pedido.LineaDeCompra;
import com.ensambla.backend.domain.model.pedido.EstadoOrdenCompra;
import com.ensambla.backend.domain.model.pedido.MensajeCliente;
import com.ensambla.backend.domain.model.pedido.OrdenCompra;
import com.ensambla.backend.domain.model.pedido.ProductoEnStock;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.EstadoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.MensajeEntrante;
import com.ensambla.backend.domain.model.whatsapp.TipoMensajeWhatsapp;
import com.ensambla.backend.domain.repository.pedido.ClienteWhatsappRepository;
import com.ensambla.backend.domain.repository.pedido.ComercioRepository;
import com.ensambla.backend.domain.repository.pedido.ConversacionClienteRepository;
import com.ensambla.backend.domain.repository.pedido.MensajeClienteRepository;
import com.ensambla.backend.domain.repository.pedido.OrdenCompraRepository;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Lo que sólo se ve contra PostgreSQL: el esquema de V25 y V26, la cola con SKIP LOCKED y la consulta de comercios. */
class PedidoPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-11T12:00:00Z");
    private static final String TELEFONO = "5491199990001";
    private static final Duration VIGENCIA = Duration.ofMinutes(2);

    @Autowired
    private ClienteWhatsappRepository clientes;

    @Autowired
    private MensajeClienteRepository mensajes;

    @Autowired
    private ConversacionClienteRepository conversaciones;

    @Autowired
    private ComercioRepository comercios;

    @Autowired
    private OrdenCompraRepository ordenes;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UsuarioId usuarioId;
    private NegocioId negocioId;

    @BeforeEach
    void setUp() {
        usuarioId = UsuarioId.nuevo();
        negocioId = NegocioId.nuevo();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Prueba Pedidos')",
                usuarioId.valor(), usuarioId.valor() + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Almacén Prueba Pedidos', ?)",
                negocioId.valor(), usuarioId.valor());
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM cliente_whatsapp WHERE telefono = ?", TELEFONO);
        jdbcTemplate.update("DELETE FROM mensaje_cliente WHERE telefono = ?", TELEFONO);
        jdbcTemplate.update("DELETE FROM negocio WHERE id = ?", negocioId.valor());
        jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", usuarioId.valor());
    }

    @Test
    @DisplayName("Guarda al cliente con su último comercio y lo encuentra por teléfono")
    void clienteShouldBeSavedAndFoundByPhone() {
        ClienteWhatsapp guardado = clientes.save(ClienteWhatsapp.nuevo(TELEFONO, AHORA));
        guardado.setUltimoNegocioId(negocioId);
        clientes.save(guardado);

        Optional<ClienteWhatsapp> encontrado = clientes.findByTelefono(TELEFONO);

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getId()).isEqualTo(guardado.getId());
        assertThat(encontrado.get().getUltimoNegocioId()).isEqualTo(negocioId);
        assertThat(clientes.findByTelefono("5491100000000")).isEmpty();
    }

    @Test
    @DisplayName("La cola entrega los mensajes en orden, uno a uno, y no repite el id externo")
    void queueShouldDeliverMessagesInOrder() {
        String primero = "PEDIDO-" + UUID.randomUUID();
        String segundo = "PEDIDO-" + UUID.randomUUID();
        mensajes.save(MensajeCliente.recibido(entrante(segundo, "dos", AHORA.plusSeconds(5))));
        mensajes.save(MensajeCliente.recibido(entrante(primero, "uno", AHORA)));

        assertThat(mensajes.existsByIdExterno(primero)).isTrue();
        assertThat(mensajes.existsByIdExterno("PEDIDO-INEXISTENTE")).isFalse();
        Optional<MensajeCliente> reclamado = mensajes.reclamarSiguiente(AHORA, VIGENCIA);

        assertThat(reclamado).isPresent();
        assertThat(reclamado.get().getTexto()).isEqualTo("uno");
        assertThat(reclamado.get().getEstado()).isEqualTo(EstadoMensajeWhatsapp.ATENDIENDO);
        assertThat(reclamado.get().getIntentos()).isEqualTo(1);
        assertThat(reclamado.get().getReclamadoHasta().toInstant()).isEqualTo(AHORA.plus(VIGENCIA).toInstant());
        assertThat(mensajes.reclamarSiguiente(AHORA, VIGENCIA).map(MensajeCliente::getTexto)).contains("dos");
        assertThat(mensajes.reclamarSiguiente(AHORA, VIGENCIA)).isEmpty();
    }

    @Test
    @DisplayName("Un mensaje reclamado cuyo plazo pasó figura como vencido")
    void expiredClaimsShouldBeListed() {
        mensajes.save(MensajeCliente.recibido(entrante("PEDIDO-" + UUID.randomUUID(), "hola", AHORA)));
        MensajeCliente reclamado = mensajes.reclamarSiguiente(AHORA, VIGENCIA).orElseThrow();

        assertThat(mensajes.vencidos(AHORA.plusMinutes(1))).isEmpty();
        assertThat(mensajes.vencidos(AHORA.plusMinutes(3))).extracting(MensajeCliente::getId)
                .containsExactly(reclamado.getId());
    }

    @Test
    @DisplayName("Guarda la conversación con las opciones y la actualiza sin duplicarla para el mismo cliente")
    void conversationShouldBeSavedOncePerClient() {
        ClienteWhatsapp cliente = clientes.save(ClienteWhatsapp.nuevo(TELEFONO, AHORA));
        ConversacionCliente conversacion = ConversacionCliente.nueva(cliente.getId(), AHORA);
        conversacion.ofrecerOpciones(List.of(negocioId), AHORA);
        conversaciones.save(conversacion);

        ConversacionCliente guardada = conversaciones.findByClienteId(cliente.getId()).orElseThrow();
        assertThat(guardada.getOpciones()).containsExactly(negocioId);
        guardada.elegir(negocioId, AHORA.plusMinutes(1));
        ProductoEnStock yerba = new ProductoEnStock(RegistroId.nuevo(), "Yerba Playadito 1 kg",
                new BigDecimal("3500.50"), new BigDecimal("10"));
        guardada.armarLista(List.of(LineaDeCompra.de(yerba, new BigDecimal("2"), List.of(yerba))), AHORA.plusMinutes(2));
        guardada.ofrecerCambioDeMarca(1, AHORA.plusMinutes(2));
        conversaciones.save(guardada);

        ConversacionCliente actualizada = conversaciones.findByClienteId(cliente.getId()).orElseThrow();
        assertThat(actualizada.getId()).isEqualTo(guardada.getId());
        assertThat(actualizada.getNegocioId()).isEqualTo(negocioId);
        assertThat(actualizada.getOpciones()).isEmpty();
        assertThat(actualizada.getRenglonACambiar()).isEqualTo(1);
        assertThat(actualizada.getLista()).singleElement().satisfies(linea -> {
            assertThat(linea.producto()).isEqualTo(yerba);
            assertThat(linea.cantidad()).isEqualByComparingTo("2");
            assertThat(linea.subtotal()).isEqualByComparingTo("7001");
        });
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM conversacion_cliente WHERE cliente_id = ?",
                Integer.class, cliente.getId().valor())).isEqualTo(1);
    }

    @Test
    @DisplayName("Guarda la orden pendiente con sus ítems y total, y la base no acepta un rechazo con otro estado")
    void orderShouldBeSavedWithItsItems() {
        ClienteWhatsapp cliente = clientes.save(ClienteWhatsapp.nuevo(TELEFONO, AHORA));
        ProductoEnStock yerba = new ProductoEnStock(RegistroId.nuevo(), "Yerba Playadito 1 kg",
                new BigDecimal("3500"), new BigDecimal("10"));

        OrdenCompra guardada = ordenes.save(OrdenCompra.pendiente(negocioId, cliente.getId(),
                List.of(LineaDeCompra.de(yerba, new BigDecimal("2"), List.of())), AHORA));

        assertThat(guardada.getId()).isNotNull();
        assertThat(guardada.getEstado()).isEqualTo(EstadoOrdenCompra.PENDIENTE);
        assertThat(guardada.getTotal()).isEqualByComparingTo("7000");
        assertThat(guardada.getItems()).singleElement().satisfies(item -> {
            assertThat(item.productoId()).isEqualTo(yerba.id());
            assertThat(item.cantidad()).isEqualByComparingTo("2");
        });
        assertThat(jdbcTemplate.queryForObject("SELECT estado FROM orden_compra WHERE id = ?", String.class,
                guardada.getId().valor())).isEqualTo("PENDIENTE");
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE orden_compra SET motivo_rechazo = 'x' WHERE id = ?", guardada.getId().valor()))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbcTemplate.update("DELETE FROM orden_compra WHERE id = ?", guardada.getId().valor());
    }

    @Test
    @DisplayName("Sólo son comercios los negocios con un número de WhatsApp vinculado")
    void businessesShouldRequireALinkedWhatsapp() {
        assertThat(comercios.listarConWhatsapp()).extracting(Comercio::id).doesNotContain(negocioId);

        jdbcTemplate.update("INSERT INTO whatsapp_vinculo (usuario_id, negocio_id, telefono) VALUES (?, ?, ?)",
                usuarioId.valor(), negocioId.valor(), "5491188880001");

        assertThat(comercios.listarConWhatsapp()).contains(new Comercio(negocioId, "Almacén Prueba Pedidos"));
    }

    private static MensajeEntrante entrante(String idExterno, String texto, OffsetDateTime recibidoEn) {
        return new MensajeEntrante(TELEFONO, idExterno, TipoMensajeWhatsapp.TEXTO, texto, null, null, recibidoEn);
    }
}
