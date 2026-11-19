package com.ensambla.backend.infrastructure.presentation.mapper.registro;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.lote.FotoId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.registro.RegistroItem;
import com.ensambla.backend.domain.useCase.registro.TotalesItem;
import com.ensambla.backend.domain.useCase.registro.crearRegistro.CrearRegistroResponse;
import com.ensambla.backend.domain.useCase.registro.editarRegistro.EditarRegistroResponse;
import com.ensambla.backend.domain.useCase.registro.listarReferencias.ListarReferenciasRequest;
import com.ensambla.backend.domain.useCase.registro.listarReferencias.ReferenciaItem;
import com.ensambla.backend.domain.useCase.registro.listarRegistros.ListarRegistrosRequest;
import com.ensambla.backend.domain.useCase.registro.listarRegistros.ListarRegistrosResponse;
import com.ensambla.backend.domain.useCase.registro.obtenerRegistro.ObtenerRegistroResponse;
import com.ensambla.backend.infrastructure.presentation.dto.registro.PaginaRegistrosResponse;
import com.ensambla.backend.infrastructure.presentation.dto.registro.ReferenciaResponse;
import com.ensambla.backend.infrastructure.presentation.dto.registro.RegistroRequest;
import com.ensambla.backend.infrastructure.presentation.dto.registro.RegistroResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class RegistroPresentationMapperTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final RegistroId OTRO_REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");
    private static final FotoId FOTO_ID = FotoId.de("f0000000-0000-4000-8000-000000000001");
    private static final CampoId CAMPO_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final Map<String, Object> VALORES = Map.of("campo", "dato");
    private static final OffsetDateTime CREADO = OffsetDateTime.parse("2026-10-09T12:00:00Z");
    private static final OffsetDateTime ACTUALIZADO = OffsetDateTime.parse("2026-10-10T12:00:00Z");
    private static final RegistroItem REGISTRO = new RegistroItem(REGISTRO_ID, BLOQUE_ID, VALORES, FOTO_ID, CREADO, ACTUALIZADO);
    private static final RegistroResponse REGISTRO_WEB =
            new RegistroResponse(REGISTRO_ID, BLOQUE_ID, VALORES, FOTO_ID, CREADO, ACTUALIZADO);

    private final RegistroPresentationMapper mapper = new RegistroPresentationMapper();
    private PaginaRegistrosResponse pagina;

    @Test
    @DisplayName("Arma el pedido de referencias con el texto y los ids pedidos")
    void armaElPedidoDeReferenciasConElTextoYLosIds() {
        entoncesElPedidoEs(mapper.toReferenciasRequest(USUARIO_ID, BLOQUE_ID, "ana", List.of(REGISTRO_ID)),
                new ListarReferenciasRequest(USUARIO_ID, BLOQUE_ID, "ana", List.of(REGISTRO_ID)));
    }

    @Test
    @DisplayName("Sin el parámetro ids el pedido de referencias lleva una lista vacía")
    void sinIdsElPedidoDeReferenciasLlevaUnaListaVacia() {
        entoncesElPedidoNoPideIds(mapper.toReferenciasRequest(USUARIO_ID, BLOQUE_ID, null, null));
    }

    @Test
    @DisplayName("Pasa el id y la etiqueta de cada referencia, en orden")
    void pasaElIdYLaEtiquetaDeCadaReferencia() {
        entoncesLasReferenciasSon(mapper.toResponse(
                        List.of(new ReferenciaItem(REGISTRO_ID, "Ana"), new ReferenciaItem(OTRO_REGISTRO_ID, "Beto"))),
                new ReferenciaResponse(REGISTRO_ID, "Ana"), new ReferenciaResponse(OTRO_REGISTRO_ID, "Beto"));
    }

    @Test
    @DisplayName("Arma el pedido de crear con el usuario de la sesión, el bloque y los valores")
    void armaElPedidoDeCrear() {
        var pedido = mapper.toCrearRequest(USUARIO_ID, BLOQUE_ID, new RegistroRequest(VALORES));

        entoncesElPedidoLleva(pedido.usuarioId(), pedido.bloqueId(), pedido.valores(), BLOQUE_ID);
    }

    @Test
    @DisplayName("Arma el pedido de listar con todos los parámetros de la consulta")
    void armaElPedidoDeListarConTodosLosParametros() {
        entoncesElListadoPide(mapper.toListarRequest(USUARIO_ID, BLOQUE_ID, "harina", "creadoEn", "asc", 2, 10));
    }

    @Test
    @DisplayName("Arma el pedido de obtener con el usuario y el registro")
    void armaElPedidoDeObtener() {
        var pedido = mapper.toObtenerRequest(USUARIO_ID, REGISTRO_ID);

        entoncesEsDelUsuarioYElRegistro(pedido.usuarioId(), pedido.registroId());
    }

    @Test
    @DisplayName("Arma el pedido de editar con el usuario, el registro y los cambios")
    void armaElPedidoDeEditar() {
        var pedido = mapper.toEditarRequest(USUARIO_ID, REGISTRO_ID, new RegistroRequest(VALORES));

        entoncesElPedidoLleva(pedido.usuarioId(), pedido.registroId(), pedido.valores(), REGISTRO_ID);
    }

    @Test
    @DisplayName("Arma el pedido de la foto de origen con el usuario y el registro")
    void armaElPedidoDeLaFotoDeOrigen() {
        var pedido = mapper.toFotoOrigenRequest(USUARIO_ID, REGISTRO_ID);

        entoncesEsDelUsuarioYElRegistro(pedido.usuarioId(), pedido.registroId());
    }

    @Test
    @DisplayName("Arma el pedido de eliminar con el usuario y el registro")
    void armaElPedidoDeEliminar() {
        var pedido = mapper.toEliminarRequest(USUARIO_ID, REGISTRO_ID);

        entoncesEsDelUsuarioYElRegistro(pedido.usuarioId(), pedido.registroId());
    }

    @Test
    @DisplayName("Convierte la respuesta de crear en el registro web")
    void convierteLaRespuestaDeCrear() {
        entoncesElRegistroWebEs(mapper.toResponse(new CrearRegistroResponse(REGISTRO)));
    }

    @Test
    @DisplayName("Convierte la respuesta de obtener en el mismo registro web")
    void convierteLaRespuestaDeObtener() {
        entoncesElRegistroWebEs(mapper.toResponse(new ObtenerRegistroResponse(REGISTRO)));
    }

    @Test
    @DisplayName("Convierte la respuesta de editar en el mismo registro web")
    void convierteLaRespuestaDeEditar() {
        entoncesElRegistroWebEs(mapper.toResponse(new EditarRegistroResponse(REGISTRO)));
    }

    @Test
    @DisplayName("Marca la primera página y no la última cuando hay más páginas")
    void marcaLaPrimeraPaginaCuandoHayMas() {
        cuandoSeRespondeLaPagina(0, 3);

        entoncesLaPaginaEs(0, 20, 45, 3);
        entoncesEsPrimeraYUltima(true, false);
        entoncesLaPaginaTieneRegistros(1);
    }

    @Test
    @DisplayName("Marca la última página y no la primera")
    void marcaLaUltimaPagina() {
        cuandoSeRespondeLaPagina(2, 3);

        entoncesEsPrimeraYUltima(false, true);
    }

    @Test
    @DisplayName("Pasa los totales de la búsqueda a la respuesta web")
    void pasaLosTotalesDeLaBusqueda() {
        cuandoSeRespondeLaPagina(0, 3);

        entoncesLosTotalesSon(45, CAMPO_ID, new BigDecimal("1500.50"));
    }

    private void cuandoSeRespondeLaPagina(int numero, int totalPaginas) {
        pagina = mapper.toResponse(new ListarRegistrosResponse(List.of(REGISTRO), numero, 20, 45, totalPaginas,
                new TotalesItem(45, Map.of(CAMPO_ID, new BigDecimal("1500.50")))));
    }

    private static void entoncesElPedidoEs(Object armado, Object esperado) {
        assertThat(armado).isEqualTo(esperado);
    }

    private static void entoncesElPedidoNoPideIds(ListarReferenciasRequest pedido) {
        assertThat(pedido.ids()).isEmpty();
    }

    private static void entoncesLasReferenciasSon(List<ReferenciaResponse> referencias, ReferenciaResponse... esperadas) {
        assertThat(referencias).containsExactly(esperadas);
    }

    private static void entoncesElPedidoLleva(UsuarioId usuario, Object destino, Map<String, Object> valores,
                                              Object destinoEsperado) {
        assertThat(usuario).isEqualTo(USUARIO_ID);
        assertThat(destino).isEqualTo(destinoEsperado);
        assertThat(valores).isEqualTo(VALORES);
    }

    private static void entoncesElListadoPide(ListarRegistrosRequest pedido) {
        assertThat(pedido.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(pedido.bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(pedido.texto()).isEqualTo("harina");
        assertThat(pedido.orden()).isEqualTo("creadoEn");
        assertThat(pedido.direccion()).isEqualTo("asc");
        assertThat(pedido.pagina()).isEqualTo(2);
        assertThat(pedido.tamanio()).isEqualTo(10);
    }

    private static void entoncesEsDelUsuarioYElRegistro(UsuarioId usuario, RegistroId registro) {
        assertThat(usuario).isEqualTo(USUARIO_ID);
        assertThat(registro).isEqualTo(REGISTRO_ID);
    }

    private static void entoncesElRegistroWebEs(RegistroResponse registro) {
        assertThat(registro).isEqualTo(REGISTRO_WEB);
    }

    private void entoncesLaPaginaEs(int numero, int tamanio, long total, int paginas) {
        assertThat(pagina.pageNumber()).isEqualTo(numero);
        assertThat(pagina.pageSize()).isEqualTo(tamanio);
        assertThat(pagina.totalElements()).isEqualTo(total);
        assertThat(pagina.totalPages()).isEqualTo(paginas);
    }

    private void entoncesEsPrimeraYUltima(boolean primera, boolean ultima) {
        assertThat(pagina.first()).isEqualTo(primera);
        assertThat(pagina.last()).isEqualTo(ultima);
    }

    private void entoncesLaPaginaTieneRegistros(int cantidad) {
        assertThat(pagina.content()).hasSize(cantidad);
    }

    private void entoncesLosTotalesSon(long cantidad, CampoId campo, BigDecimal suma) {
        assertThat(pagina.totales().cantidad()).isEqualTo(cantidad);
        assertThat(pagina.totales().sumas()).containsOnly(entry(campo, suma));
    }
}