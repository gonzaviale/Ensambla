package com.ensambla.backend.infrastructure.presentation.mapper.aviso;

import com.ensambla.backend.domain.model.aviso.AccionAbrirRegistro;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.GrupoAviso;
import com.ensambla.backend.domain.model.aviso.ImpactoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.aviso.AvisoItem;
import com.ensambla.backend.domain.useCase.aviso.ResolverAvisoRequest;
import com.ensambla.backend.domain.useCase.aviso.contarAvisos.ContadorAvisos;
import com.ensambla.backend.domain.useCase.aviso.listarAvisos.ListarAvisosRequest;
import com.ensambla.backend.domain.useCase.aviso.obtenerImpactoAvisos.ImpactoAvisos;
import com.ensambla.backend.domain.useCase.aviso.obtenerImpactoAvisos.ImpactoDeAviso;
import com.ensambla.backend.infrastructure.presentation.dto.aviso.AvisoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.aviso.ContadorAvisosResponse;
import com.ensambla.backend.infrastructure.presentation.dto.aviso.ImpactoAvisosResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AvisoPresentationMapperTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final AvisoId AVISO_ID = AvisoId.de("e0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");
    private static final AvisoItem AVISO_DE_STOCK = new AvisoItem(AVISO_ID, null, TipoAviso.STOCK, GrupoAviso.STOCK,
            (short) 3, EstadoAviso.PENDIENTE, "registro:1", "titulo", "detalle", List.of("motivo"),
            List.of(new AccionAbrirRegistro("Ver producto", null, null, null)), Map.of("cantidad", 0), AHORA, null);

    private final AvisoPresentationMapper mapper = new AvisoPresentationMapper();

    @Test
    @DisplayName("Arma el pedido de listado con el usuario y el estado opcional")
    void armaElPedidoDeListadoConElUsuarioYElEstado() {
        entoncesElPedidoDeListadoEs(new ListarAvisosRequest(USUARIO_ID, EstadoAviso.PENDIENTE));
    }

    @Test
    @DisplayName("Arma el pedido de aplicar o descartar con el usuario y el aviso")
    void armaElPedidoDeResolverConElUsuarioYElAviso() {
        entoncesElPedidoDeResolverEs(new ResolverAvisoRequest(USUARIO_ID, AVISO_ID));
    }

    @Test
    @DisplayName("Pasa el aviso a la respuesta con todos sus datos")
    void pasaElAvisoALaRespuestaConTodosSusDatos() {
        entoncesElAvisoSeRespondeComo(new AvisoResponse(AVISO_ID, null, TipoAviso.STOCK, GrupoAviso.STOCK,
                (short) 3, EstadoAviso.PENDIENTE, "registro:1", "titulo", "detalle", List.of("motivo"),
                List.of(Map.of("tipo", "ABRIR", "texto", "Ver producto")), Map.of("cantidad", 0), AHORA, null));
    }

    @Test
    @DisplayName("Pasa una lista de avisos conservando el orden")
    void pasaUnaListaDeAvisosConservandoElOrden() {
        entoncesLaListaTieneLosAvisos(AVISO_ID);
    }

    @Test
    @DisplayName("Pasa el contador a la respuesta")
    void pasaElContadorALaRespuesta() {
        entoncesElContadorSeRespondeComo(new ContadorAvisos(5, 2), new ContadorAvisosResponse(5, 2));
    }

    @Test
    @DisplayName("Pasa el impacto a la respuesta con el cambio de ingreso semanal de cada aviso medido")
    void pasaElImpactoALaRespuesta() {
        entoncesElImpactoSeRespondeComo(new ImpactoAvisosResponse(3, 1, new BigDecimal("70.00"), List.of(
                new ImpactoAvisosResponse.Medido(AVISO_ID, TipoAviso.ESTANCADO, "Mermelada cara", AHORA, 14,
                        new BigDecimal("1.00"), new BigDecimal("2.00"), new BigDecimal("10.00"),
                        new BigDecimal("20.00"), new BigDecimal("70.00")))));
    }

    private static ImpactoAvisos unImpactoMedido() {
        ImpactoAviso impacto = new ImpactoAviso(14, new BigDecimal("1.00"), new BigDecimal("2.00"),
                new BigDecimal("10.00"), new BigDecimal("20.00"));
        ImpactoDeAviso medido = new ImpactoDeAviso(AVISO_ID, TipoAviso.ESTANCADO, "Mermelada cara", AHORA, impacto);
        return new ImpactoAvisos(3, 1, new BigDecimal("70.00"), List.of(medido));
    }

    private void entoncesElPedidoDeListadoEs(ListarAvisosRequest esperado) {
        assertThat(mapper.toListarRequest(USUARIO_ID, EstadoAviso.PENDIENTE)).isEqualTo(esperado);
    }

    private void entoncesElPedidoDeResolverEs(ResolverAvisoRequest esperado) {
        assertThat(mapper.toResolverRequest(USUARIO_ID, AVISO_ID)).isEqualTo(esperado);
    }

    private void entoncesElAvisoSeRespondeComo(AvisoResponse esperada) {
        assertThat(mapper.toResponse(AVISO_DE_STOCK)).isEqualTo(esperada);
    }

    private void entoncesLaListaTieneLosAvisos(AvisoId... ids) {
        assertThat(mapper.toResponse(List.of(AVISO_DE_STOCK))).extracting(AvisoResponse::id).containsExactly(ids);
    }

    private void entoncesElContadorSeRespondeComo(ContadorAvisos contador, ContadorAvisosResponse esperado) {
        assertThat(mapper.toResponse(contador)).isEqualTo(esperado);
    }

    private void entoncesElImpactoSeRespondeComo(ImpactoAvisosResponse esperado) {
        assertThat(mapper.toResponse(unImpactoMedido())).isEqualTo(esperado);
    }
}