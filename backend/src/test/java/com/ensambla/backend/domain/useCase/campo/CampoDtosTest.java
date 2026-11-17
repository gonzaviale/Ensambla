package com.ensambla.backend.domain.useCase.campo;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.campo.crearCampo.CrearCampoRequest;
import com.ensambla.backend.domain.useCase.campo.editarCampo.EditarCampoRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CampoDtosTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final CampoId CAMPO_ID = CampoId.de("d0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime CREADO = OffsetDateTime.parse("2026-10-09T12:00:00Z");
    private static final OffsetDateTime ACTUALIZADO = OffsetDateTime.parse("2026-10-10T12:00:00Z");
    private static final BloqueId RELACIONADO_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final List<String> TALLES = List.of("Chico", "Grande");

    private CampoItem item;
    private ConfiguracionCampo configuracion;

    @Test
    @DisplayName("La respuesta tiene todos los datos del campo, con el tipo como texto")
    void laRespuestaTieneTodosLosDatosDelCampoConElTipoComoTexto() {
        cuandoSeArmaLaRespuestaDe(unTalleObligatorio(true));

        entoncesLaRespuestaTieneTodosLosDatosDelTalleObligatorio();
    }

    @Test
    @DisplayName("Una conexión pasa a la respuesta la tabla con la que se conecta")
    void unaConexionPasaALaRespuestaLaTablaConLaQueSeConecta() {
        cuandoSeArmaLaRespuestaDe(unaConexionACliente());

        entoncesLaRespuestaEsUnaConexionConLaTablaRelacionada();
    }

    @Test
    @DisplayName("Un campo sin la marca de obligatorio sale como no obligatorio")
    void unCampoSinLaMarcaDeObligatorioSaleComoNoObligatorio() {
        cuandoSeArmaLaRespuestaDe(unTalleObligatorio(null));

        entoncesLaRespuestaNoEsObligatoria();
    }

    @Test
    @DisplayName("Crear arma la configuración con todos los datos del pedido")
    void crearArmaLaConfiguracionConTodosLosDatosDelPedido() {
        cuandoSeArmaLaConfiguracionDe(unPedidoParaCrearUnTalle());

        entoncesLaConfiguracionEsLaDelTalleObligatorioConValorInicial();
    }

    @Test
    @DisplayName("Editar arma la configuración con todos los datos del pedido")
    void editarArmaLaConfiguracionConTodosLosDatosDelPedido() {
        cuandoSeArmaLaConfiguracionDe(unPedidoParaEditarUnTalle());

        entoncesLaConfiguracionEsLaDelTalleOpcionalSinValorInicial();
    }

    private void cuandoSeArmaLaRespuestaDe(Campo campo) {
        item = CampoItem.de(campo);
    }

    private void cuandoSeArmaLaConfiguracionDe(CrearCampoRequest pedido) {
        configuracion = pedido.aConfiguracionCampo();
    }

    private void cuandoSeArmaLaConfiguracionDe(EditarCampoRequest pedido) {
        configuracion = pedido.aConfiguracionCampo();
    }

    private void entoncesLaRespuestaTieneTodosLosDatosDelTalleObligatorio() {
        assertThat(item.id()).isEqualTo(CAMPO_ID);
        assertThat(item.bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(item.nombre()).isEqualTo("Talle");
        assertThat(item.tipo()).isEqualTo("OPCION");
        assertThat(item.obligatorio()).isTrue();
        assertThat(item.valorInicial()).isEqualTo("Chico");
        assertThat(item.opciones()).containsExactly("Chico", "Grande");
        assertThat(item.bloqueRelacionadoId()).isNull();
        assertThat(item.posicion()).isEqualTo((short) 1);
        assertThat(item.creadoEn()).isEqualTo(CREADO);
        assertThat(item.actualizadoEn()).isEqualTo(ACTUALIZADO);
    }

    private void entoncesLaRespuestaEsUnaConexionConLaTablaRelacionada() {
        assertThat(item.tipo()).isEqualTo("RELACION");
        assertThat(item.bloqueRelacionadoId()).isEqualTo(RELACIONADO_ID);
    }

    private void entoncesLaRespuestaNoEsObligatoria() {
        assertThat(item.obligatorio()).isFalse();
    }

    private void entoncesLaConfiguracionEsLaDelTalleObligatorioConValorInicial() {
        assertThat(configuracion)
                .isEqualTo(new ConfiguracionCampo("Talle", "OPCION", true, "Chico", TALLES, RELACIONADO_ID, null));
    }

    private void entoncesLaConfiguracionEsLaDelTalleOpcionalSinValorInicial() {
        assertThat(configuracion)
                .isEqualTo(new ConfiguracionCampo("Talle", "OPCION", false, null, TALLES, RELACIONADO_ID, null));
    }

    private CrearCampoRequest unPedidoParaCrearUnTalle() {
        return new CrearCampoRequest(
                USUARIO_ID, BLOQUE_ID, "Talle", "OPCION", true, "Chico", TALLES, RELACIONADO_ID, null);
    }

    private EditarCampoRequest unPedidoParaEditarUnTalle() {
        return new EditarCampoRequest(
                USUARIO_ID, CAMPO_ID, "Talle", "OPCION", false, null, TALLES, RELACIONADO_ID, null);
    }

    private Campo unTalleObligatorio(Boolean obligatorio) {
        return Campo.builder()
                .id(CAMPO_ID)
                .bloqueId(BLOQUE_ID)
                .nombre("Talle")
                .tipo(TipoCampo.OPCION)
                .obligatorio(obligatorio)
                .valorInicial("Chico")
                .opciones(TALLES)
                .posicion((short) 1)
                .creadoEn(CREADO)
                .actualizadoEn(ACTUALIZADO)
                .build();
    }

    private Campo unaConexionACliente() {
        return Campo.builder()
                .id(CAMPO_ID)
                .bloqueId(BLOQUE_ID)
                .nombre("Cliente")
                .tipo(TipoCampo.RELACION)
                .bloqueRelacionadoId(RELACIONADO_ID)
                .posicion((short) 1)
                .creadoEn(CREADO)
                .actualizadoEn(ACTUALIZADO)
                .build();
    }
}
