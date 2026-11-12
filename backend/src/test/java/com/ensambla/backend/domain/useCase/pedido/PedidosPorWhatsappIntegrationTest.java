package com.ensambla.backend.domain.useCase.pedido;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pedido.EstadoOrdenCompra;
import com.ensambla.backend.domain.model.pedido.OrdenCompraId;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.MensajeEntrante;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TipoMensajeWhatsapp;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.useCase.orden.OrdenItem;
import com.ensambla.backend.domain.useCase.orden.cambiarEstadoOrden.CambiarEstadoOrden;
import com.ensambla.backend.domain.useCase.orden.cambiarEstadoOrden.CambiarEstadoOrdenRequest;
import com.ensambla.backend.domain.useCase.orden.listarOrdenes.ListarOrdenes;
import com.ensambla.backend.domain.useCase.orden.listarOrdenes.ListarOrdenesRequest;
import com.ensambla.backend.domain.useCase.pedido.atenderMensajeCliente.AtenderMensajeCliente;
import com.ensambla.backend.domain.useCase.whatsapp.procesarMensajeEntrante.ProcesarMensajeEntrante;
import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import com.ensambla.backend.infrastructure.repository.entity.ConversacionClienteEntity;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.ProyectoEntity;
import com.ensambla.backend.infrastructure.repository.entity.RegistroEntity;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.entity.VinculoWhatsappEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IBloqueJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICampoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IConversacionClienteJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IProyectoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IRegistroJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IVinculoWhatsappJpaRepository;
import com.ensambla.backend.infrastructure.whatsapp.MensajeriaWhatsappSimulada;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El pedido por WhatsApp de punta a punta, con las piezas reales y PostgreSQL: un cliente de la lista de prueba le escribe
 * a Sami, elige el comercio por nombre, pide en texto libre, cambia una marca y confirma; el dueño acepta, prepara y deja
 * lista la orden, y Sami le avisa al cliente. Los proveedores de WhatsApp y de IA son los simulados de los tests.
 */
@Transactional
@TestPropertySource(properties = "ensambla.whatsapp.pedidos.clientes-permitidos=+54 9 11 5555-0001")
class PedidosPorWhatsappIntegrationTest extends PostgresRepositoryTestSupport {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-11T12:00:00Z");
    private static final String CLIENTE = "5491155550001";
    private static final String DESCONOCIDO = "5491100009999";

    @Autowired
    private ProcesarMensajeEntrante procesarMensajeEntrante;

    @Autowired
    private AtenderMensajeCliente atenderMensajeCliente;

    @Autowired
    private ListarOrdenes listarOrdenes;

    @Autowired
    private CambiarEstadoOrden cambiarEstadoOrden;

    @Autowired
    private MensajeriaWhatsappPort mensajeria;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private IUsuarioJpaRepository usuarios;

    @Autowired
    private INegocioJpaRepository negocios;

    @Autowired
    private IProyectoJpaRepository proyectos;

    @Autowired
    private IBloqueJpaRepository bloques;

    @Autowired
    private ICampoJpaRepository campos;

    @Autowired
    private IRegistroJpaRepository registros;

    @Autowired
    private IVinculoWhatsappJpaRepository vinculos;

    @Autowired
    private IConversacionClienteJpaRepository conversaciones;

    private UsuarioId duenioDePepe;
    private UUID campoStockId;
    private UUID yerbaId;
    private UUID cocineroId;
    private UUID naturaId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM cliente_whatsapp WHERE telefono = ?", CLIENTE);
        duenioDePepe = UsuarioId.de(givenUsuario());
        NegocioId pepe = givenNegocio("Almacén Don Pepe", duenioDePepe, "5491100000011");
        BloqueId inventario = givenInventarioVacio(pepe);
        yerbaId = givenProducto(pepe, inventario, "Yerba Playadito 1 kg", 3500, 10);
        cocineroId = givenProducto(pepe, inventario, "Aceite Cocinero 900 ml", 2000, 3);
        naturaId = givenProducto(pepe, inventario, "Aceite Natura 900 ml", 2400, 8);

        UsuarioId duenioDeRosa = UsuarioId.de(givenUsuario());
        NegocioId rosa = givenNegocio("Almacén Doña Rosa", duenioDeRosa, "5491100000012");
        givenProducto(rosa, givenInventarioVacio(rosa), "Fideos Matarazzo 500 g", 1200, 20);

        // Un negocio sin WhatsApp vinculado no recibe pedidos: no se ofrece.
        negocios.saveAndFlush(NegocioEntity.builder().nombre("Almacén Sin WhatsApp")
                .duenioId(givenUsuario()).creadoEn(AHORA).actualizadoEn(AHORA).build());

        // Todo el test corre en una transacción: sin esto los bloques seguirían en memoria con sus campos vacíos, tal
        // como los armó el test, y la conversación no leería lo que hay en la base como lo haría en producción.
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("El cliente pide, cambia una marca y confirma; el dueño acepta, prepara y deja lista la orden, y Sami le avisa")
    void shouldCompleteTheWholeOrderFlow() {
        assertThat(escribe(CLIENTE, "hola")).contains("¿A qué comercio querés pedirle?");

        String opciones = escribe(CLIENTE, "almacen");
        assertThat(opciones).contains("Almacén Don Pepe", "Almacén Doña Rosa").doesNotContain("Sin WhatsApp");
        assertThat(escribe(CLIENTE, numeroDe(opciones, "Almacén Don Pepe")))
                .contains("te atiende *Almacén Don Pepe*");

        String lista = escribe(CLIENTE, "dame 2 yerba playadito, aceite y pañales");
        assertThat(lista).contains("Tu pedido en *Almacén Don Pepe*:",
                "1) Yerba Playadito 1 kg x2 — $7.000", "2) Aceite Cocinero 900 ml x1 — $2.000",
                "También tengo: Aceite Natura 900 ml", "Total: $9.000", "No tengo «panales».");

        assertThat(escribe(CLIENTE, "cambiar 2")).contains("1) Aceite Natura 900 ml — $2.400");
        assertThat(escribe(CLIENTE, "1")).contains("2) Aceite Natura 900 ml x1 — $2.400", "Total: $9.400");

        assertThat(escribe(CLIENTE, "sí")).contains("Mandé tu pedido a *Almacén Don Pepe* (total $9.400)");

        List<OrdenItem> ordenes = listarOrdenes.execute(new ListarOrdenesRequest(duenioDePepe, null));
        assertThat(ordenes).singleElement().satisfies(orden -> {
            assertThat(orden.estado()).isEqualTo(EstadoOrdenCompra.PENDIENTE);
            assertThat(orden.clienteTelefono()).isEqualTo(CLIENTE);
            assertThat(orden.total()).isEqualByComparingTo("9400");
            assertThat(orden.items()).extracting("nombre")
                    .containsExactly("Yerba Playadito 1 kg", "Aceite Natura 900 ml");
        });
        assertThat(stockDe(yerbaId)).isEqualByComparingTo("10");
        OrdenCompraId orden = ordenes.getFirst().id();

        assertThat(cambia(orden, EstadoOrdenCompra.ACEPTADA).clienteAvisado()).isTrue();
        assertThat(ultimoMensajeA(CLIENTE)).contains("*Almacén Don Pepe* aceptó tu pedido");
        assertThat(stockDe(yerbaId)).isEqualByComparingTo("8");
        assertThat(stockDe(naturaId)).isEqualByComparingTo("7");
        assertThat(stockDe(cocineroId)).isEqualByComparingTo("3");

        int mensajes = enviadosA(CLIENTE).size();
        assertThat(cambia(orden, EstadoOrdenCompra.EN_PREPARACION).clienteAvisado()).isNull();
        assertThat(enviadosA(CLIENTE)).hasSize(mensajes);

        assertThat(cambia(orden, EstadoOrdenCompra.LISTA).clienteAvisado()).isTrue();
        assertThat(ultimoMensajeA(CLIENTE))
                .isEqualTo("¡Tu pedido en *Almacén Don Pepe* está listo! Ya podés pasar a retirarlo. Total: $9.400.");
    }

    @Test
    @DisplayName("Si el dueño rechaza la orden, el cliente se entera con el motivo y el inventario no cambia")
    void shouldTellTheClientWhenTheOrderIsRejected() {
        confirmaUnPedidoDeYerba();
        OrdenCompraId orden = listarOrdenes.execute(new ListarOrdenesRequest(duenioDePepe, null)).getFirst().id();

        cambiarEstadoOrden.execute(new CambiarEstadoOrdenRequest(duenioDePepe, orden, EstadoOrdenCompra.RECHAZADA,
                "Hoy no me queda"));

        assertThat(ultimoMensajeA(CLIENTE)).contains("*Almacén Don Pepe* no pudo aceptar tu pedido.",
                "Motivo: Hoy no me queda.");
        assertThat(stockDe(yerbaId)).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("Si el stock se acabó entre que el cliente confirmó y el dueño aceptó, no se acepta y el cliente no recibe aviso")
    void shouldNotAcceptWhenTheStockRanOut() {
        confirmaUnPedidoDeYerba();
        OrdenCompraId orden = listarOrdenes.execute(new ListarOrdenesRequest(duenioDePepe, null)).getFirst().id();
        RegistroEntity yerba = registros.findById(yerbaId).orElseThrow();
        Map<String, Object> valores = new LinkedHashMap<>(yerba.getValores());
        valores.put(campoStockId.toString(), 1);
        yerba.setValores(valores);
        registros.saveAndFlush(yerba);
        int mensajes = enviadosA(CLIENTE).size();

        assertThatThrownBy(() -> cambia(orden, EstadoOrdenCompra.ACEPTADA))
                .hasMessageContaining("No alcanza el stock de Yerba Playadito 1 kg");

        assertThat(enviadosA(CLIENTE)).hasSize(mensajes);
        assertThat(listarOrdenes.execute(new ListarOrdenesRequest(duenioDePepe, null)).getFirst().estado())
                .isEqualTo(EstadoOrdenCompra.PENDIENTE);
    }

    @Test
    @DisplayName("Un cliente que vuelve después de un rato recibe la oferta de seguir con el último comercio")
    void shouldOfferTheLastBusinessWhenTheClientComesBack() {
        confirmaUnPedidoDeYerba();
        ConversacionClienteEntity conversacion = conversaciones.findAll().getFirst();
        conversacion.setActualizadoEn(AHORA.minusHours(2));
        conversaciones.saveAndFlush(conversacion);

        assertThat(escribe(CLIENTE, "hola")).contains("¿Seguimos con *Almacén Don Pepe*?");
        assertThat(escribe(CLIENTE, "sí")).contains("te atiende *Almacén Don Pepe*");
    }

    @Test
    @DisplayName("Un número que no está en la lista recibe la respuesta fija y no genera clientes, mensajes ni pedidos")
    void shouldIgnoreNumbersOutsideTheList() {
        assertThat(escribe(DESCONOCIDO, "dame 2 yerba")).isEqualTo(TextosWhatsapp.NO_VINCULADO);
        assertThat(escribe(DESCONOCIDO, "hola")).isEqualTo(TextosWhatsapp.NO_VINCULADO);

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM cliente_whatsapp WHERE telefono = ?",
                Integer.class, DESCONOCIDO)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM mensaje_cliente WHERE telefono = ?",
                Integer.class, DESCONOCIDO)).isZero();
        assertThat(listarOrdenes.execute(new ListarOrdenesRequest(duenioDePepe, null))).isEmpty();
    }

    // --- el cliente y el dueño ---

    private void confirmaUnPedidoDeYerba() {
        escribe(CLIENTE, "hola");
        escribe(CLIENTE, "don pepe");
        assertThat(escribe(CLIENTE, "2 yerba playadito")).contains("Total: $7.000");
        assertThat(escribe(CLIENTE, "ok")).contains("Mandé tu pedido");
    }

    /** El cliente manda un texto; el worker (apagado en los tests) lo atendería enseguida: acá se atiende a mano. */
    private String escribe(String telefono, String texto) {
        int antes = enviadosA(telefono).size();
        procesarMensajeEntrante.execute(new MensajeEntrante(telefono, "ID-" + UUID.randomUUID(),
                TipoMensajeWhatsapp.TEXTO, texto, null, null, AHORA));
        while (atenderMensajeCliente.execute()) {
            // atiende todo lo que haya en la cola de clientes
        }
        List<String> enviados = enviadosA(telefono);
        assertThat(enviados).as("Sami tiene que contestarle a %s", telefono).hasSizeGreaterThan(antes);
        return enviados.getLast();
    }

    private OrdenItem cambia(OrdenCompraId orden, EstadoOrdenCompra estado) {
        return cambiarEstadoOrden.execute(new CambiarEstadoOrdenRequest(duenioDePepe, orden, estado, null));
    }

    private List<String> enviadosA(String telefono) {
        return ((MensajeriaWhatsappSimulada) mensajeria).enviados().stream()
                .filter(enviado -> enviado.telefono().equals(telefono))
                .map(MensajeriaWhatsappSimulada.Enviado::texto)
                .toList();
    }

    private String ultimoMensajeA(String telefono) {
        return enviadosA(telefono).getLast();
    }

    /** El número con el que se ofrece un comercio en la lista numerada, para no depender del orden en que salen. */
    private static String numeroDe(String listaNumerada, String comercio) {
        Matcher coincidencia = Pattern.compile("(\\d)\\) " + Pattern.quote(comercio)).matcher(listaNumerada);
        assertThat(coincidencia.find()).as("%s figura en la lista", comercio).isTrue();
        return coincidencia.group(1);
    }

    private BigDecimal stockDe(UUID registro) {
        Object valor = registros.findById(registro).orElseThrow().getValores().get(campoStockId.toString());
        return new BigDecimal(valor.toString());
    }

    // --- datos de prueba ---

    private UUID givenUsuario() {
        return usuarios.saveAndFlush(UsuarioEntity.builder().nombre("Persona")
                .email(UUID.randomUUID() + "@example.com").estado(EstadoUsuario.ACTIVO)
                .creadoEn(AHORA).actualizadoEn(AHORA).build()).getId();
    }

    private NegocioId givenNegocio(String nombre, UsuarioId duenio, String telefonoDelDuenio) {
        NegocioId negocio = NegocioId.de(negocios.saveAndFlush(NegocioEntity.builder().nombre(nombre)
                .duenioId(duenio.valor()).creadoEn(AHORA).actualizadoEn(AHORA).build()).getId());
        vinculos.saveAndFlush(VinculoWhatsappEntity.builder().usuarioId(duenio.valor()).negocioId(negocio.valor())
                .telefono(telefonoDelDuenio).vinculadoEn(AHORA).build());
        return negocio;
    }

    /** Un sistema con una tabla de inventario con nombre, precio y cantidad (las tres columnas que lee Sami). */
    private BloqueId givenInventarioVacio(NegocioId negocio) {
        UUID proyecto = proyectos.saveAndFlush(ProyectoEntity.builder().negocioId(negocio.valor()).nombre("Almacén")
                .creadoEn(AHORA).actualizadoEn(AHORA).build()).getId();
        BloqueId bloque = BloqueId.de(bloques.saveAndFlush(BloqueEntity.builder().negocioId(negocio.valor())
                .proyectoId(proyecto).nombre("Inventario").tipo(TipoBloque.INVENTARIO).posicion((short) 0)
                .creadoEn(AHORA).actualizadoEn(AHORA).build()).getId());
        givenCampo(negocio, bloque, "Nombre", TipoCampo.TEXTO, "name", 0);
        givenCampo(negocio, bloque, "Precio", TipoCampo.MONEDA, "price", 1);
        UUID stock = givenCampo(negocio, bloque, "Cantidad", TipoCampo.NUMERO, "quantity", 2);
        if (negocio.equals(negocioDePepe())) {
            campoStockId = stock;
        }
        return bloque;
    }

    private NegocioId negocioDePepe() {
        return NegocioId.de(negocios.findFirstByDuenioIdOrderByCreadoEnAscIdAsc(duenioDePepe.valor())
                .orElseThrow().getId());
    }

    private UUID givenCampo(NegocioId negocio, BloqueId bloque, String nombre, TipoCampo tipo, String clave,
                            int posicion) {
        return campos.saveAndFlush(CampoEntity.builder().negocioId(negocio.valor()).bloqueId(bloque.valor())
                .nombre(nombre).tipo(tipo).clave(clave).obligatorio(false).opciones(List.of())
                .posicion((short) posicion).creadoEn(AHORA).actualizadoEn(AHORA).build()).getId();
    }

    private UUID givenProducto(NegocioId negocio, BloqueId bloque, String nombre, int precio, int stock) {
        Map<String, Object> valores = new LinkedHashMap<>();
        campos.findAll().stream().filter(campo -> campo.getBloqueId().equals(bloque.valor())).forEach(campo ->
                valores.put(campo.getId().toString(), switch (campo.getClave()) {
                    case "name" -> nombre;
                    case "price" -> precio;
                    default -> stock;
                }));
        return registros.saveAndFlush(RegistroEntity.builder().negocioId(negocio.valor()).bloqueId(bloque.valor())
                .valores(valores).creadoEn(AHORA).actualizadoEn(AHORA).build()).getId();
    }
}
