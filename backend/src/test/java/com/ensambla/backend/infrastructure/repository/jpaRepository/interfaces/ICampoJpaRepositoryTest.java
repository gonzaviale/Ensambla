package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import com.ensambla.backend.infrastructure.repository.entity.RegistroEntity;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ICampoJpaRepositoryTest extends PostgresDatosTestSupport {

    @Autowired
    private EntityManager entityManager;

    private UUID negocioId;
    private UUID negocioA;
    private UUID negocioB;
    private UUID bloqueId;
    private UUID campoId;
    private UUID campoDeA;
    private UUID registroId;
    private UUID clientes;
    private UUID pedidos;
    private UUID clientesDeB;
    private UUID pedidosDeA;
    private UUID clienteConexion;
    private CampoEntity campoDeTexto;
    private Optional<CampoEntity> encontrado;
    private CampoEntity releido;
    private List<CampoEntity> conexiones;
    private Throwable rechazo;

    @Test
    @DisplayName("Encuentra el campo dentro de su negocio")
    void encuentraElCampoDentroDeSuNegocio() {
        dadoUnCampoPrecioDelNegocio();

        cuandoSeBuscaElCampoEnSuNegocio();

        entoncesSeEncuentraElCampoPrecio();
    }

    @Test
    @DisplayName("No devuelve un campo de otro negocio")
    void noDevuelveUnCampoDeOtroNegocio() {
        dadoUnCampoDelNegocioADeOtroNegocioB();

        cuandoSeBuscaElCampoEnElNegocioB();

        entoncesNoSeEncuentraNingunCampo();
    }

    @Test
    @DisplayName("Guarda y relee las opciones, el valor inicial y la obligatoriedad de un campo de opción")
    void guardaYReleeLasOpcionesElValorInicialYLaObligatoriedadDeUnCampoDeOpcion() {
        dadoUnCampoDeOpcionGuardado();

        cuandoSeReleeElCampoDesdeLaBase();

        entoncesSeConservaLaConfiguracionDelCampoDeOpcion();
    }

    @Test
    @DisplayName("Borrar un campo no borra los registros del bloque")
    void borrarUnCampoNoBorraLosRegistrosDelBloque() {
        dadoUnCampoConUnRegistroEnElBloque();

        cuandoSeBorraElCampo();

        entoncesElCampoSeBorraPeroElRegistroSeConserva();
    }

    @Test
    @DisplayName("Borrar con el negocio equivocado no toca el campo ajeno")
    void borrarConElNegocioEquivocadoNoTocaElCampoAjeno() {
        dadoUnCampoDelNegocioADeOtroNegocioB();

        cuandoSeBorraElCampoConElNegocioB();

        entoncesElCampoAjenoSigueExistiendo();
    }

    @Test
    @DisplayName("Encuentra los campos de otros bloques que se conectan con el bloque, sin contar la autorreferencia")
    void encuentraLosCamposDeOtrosBloquesQueSeConectanConElBloqueSinContarLaAutorreferencia() {
        dadoUnaConexionDePedidosAClientesYUnaAutorreferencia();

        cuandoSeBuscanLasConexionesHaciaClientes();

        entoncesSoloSeEncuentraLaConexionDesdePedidos();
    }

    @Test
    @DisplayName("No devuelve conexiones de otro negocio")
    void noDevuelveConexionesDeOtroNegocio() {
        dadoUnaConexionEnElNegocioA();

        cuandoSeBuscanLasConexionesHaciaClientesDesdeElNegocioB();

        entoncesNoSeEncuentraNingunaConexion();
    }

    @Test
    @DisplayName("Guarda y relee la tabla con la que se conecta un campo de conexión")
    void guardaYReleeLaTablaConLaQueSeConectaUnCampoDeConexion() {
        dadoUnCampoDeConexionGuardado();

        cuandoSeReleeElCampoDesdeLaBase();

        entoncesSeConservaLaTablaConectada();
    }

    @Test
    @DisplayName("La base rechaza un campo de conexión sin tabla")
    void laBaseRechazaUnCampoDeConexionSinTabla() {
        dadoUnBloquePedidos();

        cuandoSeIntentaGuardarUnaConexionSinTabla();

        entoncesLaBaseRechazaPorIntegridad();
    }

    @Test
    @DisplayName("La base rechaza una tabla conectada en un campo que no es de conexión")
    void laBaseRechazaUnaTablaConectadaEnUnCampoQueNoEsDeConexion() {
        dadoUnCampoDeTextoConTablaConectada();

        cuandoSeIntentaGuardarElCampoDeTexto();

        entoncesLaBaseRechazaPorIntegridad();
    }

    @Test
    @DisplayName("La base rechaza conectar con una tabla de otro negocio")
    void laBaseRechazaConectarConUnaTablaDeOtroNegocio() {
        dadoUnaTablaDeClientesDeOtroNegocio();

        cuandoSeIntentaConectarConLaTablaDeOtroNegocio();

        entoncesLaBaseRechazaPorIntegridad();
    }

    @Test
    @DisplayName("Borrar la tabla conectada borra en cascada el campo de conexión")
    void borrarLaTablaConectadaBorraEnCascadaElCampoDeConexion() {
        dadoUnCampoDeConexionGuardado();

        cuandoSeBorraLaTablaConectada();

        entoncesSeBorraElCampoDeConexion();
    }

    // --- dado ---
    private void dadoUnCampoPrecioDelNegocio() {
        negocioId = givenNegocio("a@example.com").getId();
        campoId = givenCampo(NegocioId.de(negocioId), BloqueId
                .de(givenBloque(NegocioId.de(negocioId), "Productos")), "Precio", TipoCampo.NUMERO, 0);
    }

    private void dadoUnCampoDelNegocioADeOtroNegocioB() {
        negocioA = givenNegocio("a@example.com").getId();
        negocioB = givenNegocio("b@example.com").getId();
        campoDeA = givenCampo(NegocioId.de(negocioA), BloqueId
                .de(givenBloque(NegocioId.de(negocioA), "Productos")), "Precio", TipoCampo.NUMERO, 0);
    }

    private void dadoUnCampoDeOpcionGuardado() {
        negocioId = givenNegocio("a@example.com").getId();
        bloqueId = givenBloque(NegocioId.de(negocioId), "Productos");
        campoId = campos.saveAndFlush(CampoEntity.builder()
                .negocioId(negocioId)
                .bloqueId(bloqueId)
                .nombre("Talle")
                .tipo(TipoCampo.OPCION)
                .obligatorio(true)
                .valorInicial("Chico")
                .opciones(List.of("Chico", "Grande"))
                .posicion((short) 0)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private void dadoUnCampoConUnRegistroEnElBloque() {
        negocioId = givenNegocio("a@example.com").getId();
        bloqueId = givenBloque(NegocioId.de(negocioId), "Productos");
        campoId = givenCampo(NegocioId
                .de(negocioId), BloqueId.de(bloqueId), "Nombre", TipoCampo.TEXTO, 0);
        registroId = givenRegistro(NegocioId.de(negocioId), BloqueId.de(bloqueId), Map
                .of(campoId.toString(), "Vela"), AHORA);
    }

    private void dadoUnaConexionDePedidosAClientesYUnaAutorreferencia() {
        negocioId = givenNegocio("a@example.com").getId();
        clientes = givenBloque(NegocioId.de(negocioId), "Clientes");
        pedidos = givenBloque(NegocioId.de(negocioId), "Pedidos");
        givenCampo(NegocioId.de(negocioId), BloqueId.de(pedidos), "Total", TipoCampo.NUMERO, 0);
        unaRelacion(negocioId, clientes, "Recomendado por", clientes);
        clienteConexion = unaRelacion(negocioId, pedidos, "Cliente", clientes);
    }

    private void dadoUnaConexionEnElNegocioA() {
        negocioA = givenNegocio("a@example.com").getId();
        negocioB = givenNegocio("b@example.com").getId();
        clientes = givenBloque(NegocioId.de(negocioA), "Clientes");
        unaRelacion(negocioA, givenBloque(NegocioId.de(negocioA), "Pedidos"), "Cliente", clientes);
    }

    private void dadoUnCampoDeConexionGuardado() {
        negocioId = givenNegocio("a@example.com").getId();
        clientes = givenBloque(NegocioId.de(negocioId), "Clientes");
        campoId = unaRelacion(negocioId, givenBloque(NegocioId.de(negocioId), "Pedidos"), "Cliente", clientes);
    }

    private void dadoUnBloquePedidos() {
        negocioId = givenNegocio("a@example.com").getId();
        pedidos = givenBloque(NegocioId.de(negocioId), "Pedidos");
    }

    private void dadoUnCampoDeTextoConTablaConectada() {
        negocioId = givenNegocio("a@example.com").getId();
        clientes = givenBloque(NegocioId.de(negocioId), "Clientes");
        campoDeTexto = unCampo(negocioId, givenBloque(NegocioId.de(negocioId), "Pedidos"), "Cliente",
                TipoCampo.TEXTO, clientes);
    }

    private void dadoUnaTablaDeClientesDeOtroNegocio() {
        negocioA = givenNegocio("a@example.com").getId();
        negocioB = givenNegocio("b@example.com").getId();
        clientesDeB = givenBloque(NegocioId.de(negocioB), "Clientes");
        pedidosDeA = givenBloque(NegocioId.de(negocioA), "Pedidos");
    }

    private UUID unaRelacion(UUID negocio, UUID bloque, String nombre, UUID relacionadoId) {
        return campos.saveAndFlush(unCampo(negocio, bloque, nombre, TipoCampo.RELACION, relacionadoId)).getId();
    }

    private CampoEntity unCampo(UUID negocio, UUID bloque, String nombre, TipoCampo tipo, UUID relacionadoId) {
        return CampoEntity.builder()
                .negocioId(negocio)
                .bloqueId(bloque)
                .nombre(nombre)
                .tipo(tipo)
                .obligatorio(false)
                .opciones(List.of())
                .bloqueRelacionadoId(relacionadoId)
                .posicion((short) 1)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build();
    }

    // --- cuando ---
    private void cuandoSeBuscaElCampoEnSuNegocio() {
        encontrado = campos.findByIdAndNegocioId(campoId, negocioId);
    }

    private void cuandoSeBuscaElCampoEnElNegocioB() {
        encontrado = campos.findByIdAndNegocioId(campoDeA, negocioB);
    }

    private void cuandoSeReleeElCampoDesdeLaBase() {
        entityManager.clear();
        releido = campos.findByIdAndNegocioId(campoId, negocioId).orElseThrow();
    }

    private void cuandoSeBorraElCampo() {
        campos.deleteByIdAndNegocioId(campoId, negocioId);
    }

    private void cuandoSeBorraElCampoConElNegocioB() {
        campos.deleteByIdAndNegocioId(campoDeA, negocioB);
    }

    private void cuandoSeBuscanLasConexionesHaciaClientes() {
        conexiones = campos.findRelacionesHacia(clientes, negocioId);
    }

    private void cuandoSeBuscanLasConexionesHaciaClientesDesdeElNegocioB() {
        conexiones = campos.findRelacionesHacia(clientes, negocioB);
    }

    private void cuandoSeIntentaGuardarUnaConexionSinTabla() {
        rechazo = catchThrowable(() -> unaRelacion(negocioId, pedidos, "Cliente", null));
    }

    private void cuandoSeIntentaGuardarElCampoDeTexto() {
        rechazo = catchThrowable(() -> campos.saveAndFlush(campoDeTexto));
    }

    private void cuandoSeIntentaConectarConLaTablaDeOtroNegocio() {
        rechazo = catchThrowable(() -> unaRelacion(negocioA, pedidosDeA, "Cliente", clientesDeB));
    }

    private void cuandoSeBorraLaTablaConectada() {
        bloques.deleteByIdAndNegocioId(clientes, negocioId);
    }

    // --- entonces ---
    private void entoncesSeEncuentraElCampoPrecio() {
        assertThat(encontrado).map(CampoEntity::getNombre).contains("Precio");
    }

    private void entoncesNoSeEncuentraNingunCampo() {
        assertThat(encontrado).isEmpty();
    }

    private void entoncesSeConservaLaConfiguracionDelCampoDeOpcion() {
        assertThat(releido.getTipo()).isEqualTo(TipoCampo.OPCION);
        assertThat(releido.getObligatorio()).isTrue();
        assertThat(releido.getValorInicial()).isEqualTo("Chico");
        assertThat(releido.getOpciones()).containsExactly("Chico", "Grande");
    }

    private void entoncesElCampoSeBorraPeroElRegistroSeConserva() {
        assertThat(campos.findById(campoId)).isEmpty();
        assertThat(registros.findById(registroId)).map(RegistroEntity::getValores)
                .contains(Map.of(campoId.toString(), "Vela"));
    }

    private void entoncesElCampoAjenoSigueExistiendo() {
        assertThat(campos.findByIdAndNegocioId(campoDeA, negocioA)).isPresent();
    }

    private void entoncesSoloSeEncuentraLaConexionDesdePedidos() {
        assertThat(conexiones).extracting(CampoEntity::getId).containsExactly(clienteConexion);
    }

    private void entoncesNoSeEncuentraNingunaConexion() {
        assertThat(conexiones).isEmpty();
    }

    private void entoncesSeConservaLaTablaConectada() {
        assertThat(releido.getTipo()).isEqualTo(TipoCampo.RELACION);
        assertThat(releido.getBloqueRelacionadoId()).isEqualTo(clientes);
    }

    private void entoncesLaBaseRechazaPorIntegridad() {
        assertThat(rechazo).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void entoncesSeBorraElCampoDeConexion() {
        assertThat(campos.findById(campoId)).isEmpty();
    }
}
