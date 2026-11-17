package com.ensambla.backend.domain.model.common;

import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.inventario.MovimientoStockId;
import com.ensambla.backend.domain.model.lote.FotoId;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivoId;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ProgresoObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ReajusteMiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.RespuestaMiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ResumenSemanalId;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.pago.RenovacionId;
import com.ensambla.backend.domain.model.plan.PlanId;
import com.ensambla.backend.domain.model.plan.SuscripcionId;
import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.model.procesamiento.ReclamoId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.receta.RecetaId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.seccion.SeccionId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.CodigoVinculacionId;
import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsappId;
import com.ensambla.backend.domain.model.whatsapp.VerificacionWhatsappId;
import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsappId;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsappId;
import com.ensambla.backend.domain.model.zona.ZonaId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class IdentificadorTest {

    private static final UUID UUID_FIJO = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID OTRO_UUID = UUID.fromString("a0000000-0000-4000-8000-000000000002");

    private Identificador desdeUuid;
    private Identificador desdeTexto;
    private Object uno;
    private Object otro;
    private Throwable error;
    private UUID valor;
    private BloqueId idOpcional;
    private List<UUID> valores;
    private boolean sonIguales;

    static Stream<Class<?>> tipos() {
        return Stream.of(AvisoId.class, BloqueId.class, CampoId.class, MovimientoStockId.class, FotoId.class,
                LoteId.class, NegocioId.class, RenovacionId.class, PlanId.class, SuscripcionId.class,
                PlantillaId.class, ReclamoId.class, ProyectoId.class, RecetaId.class, RegistroId.class,
                SeccionId.class, UsuarioId.class, ZonaId.class, VinculoWhatsappId.class, CodigoVinculacionId.class, PropuestaWhatsappId.class,
                VerificacionWhatsappId.class,
                MensajeWhatsappId.class, ObjetivoId.class, MiniObjetivoId.class, ProgresoObjetivoId.class,
                DecisionObjetivoId.class, ResumenSemanalId.class, ReajusteMiniObjetivoId.class,
                RespuestaMiniObjetivoId.class);
    }

    @ParameterizedTest
    @MethodSource("tipos")
    @DisplayName("Cada id envuelve su UUID, se arma desde UUID o texto y se lee como el UUID")
    void idShouldWrapUuid(Class<?> tipo) throws Exception {
        whenSeArmaDesdeUuidYTexto(tipo);

        thenElIdEnvuelveElUuid();
    }

    @ParameterizedTest
    @MethodSource("tipos")
    @DisplayName("Un id nuevo es distinto cada vez")
    void nuevoShouldCreateDifferentIds(Class<?> tipo) throws Exception {
        whenSeCreanDosIdsNuevos(tipo);

        thenLosIdsSonDistintos();
    }

    @ParameterizedTest
    @MethodSource("tipos")
    @DisplayName("Un id sin UUID no se puede armar")
    void idShouldRejectNull(Class<?> tipo) {
        whenSeArmaSinUuid(tipo);

        thenSeRechazaPorObligatorio();
    }

    @Test
    @DisplayName("Dos ids de tipo distinto con el mismo UUID no son iguales")
    void idsOfDifferentTypesShouldNotBeEqual() {
        whenSeComparanIdsDeDistintoTipo();

        thenNoSonIguales();
    }

    @Test
    @DisplayName("El valor de un id que falta es nulo")
    void valorDeShouldBeNullForMissingId() {
        whenSeLeElValorDe(null);

        thenElValorEs(null);
    }

    @Test
    @DisplayName("El valor de un id idOpcional es su UUID")
    void valorDeShouldBeTheUuidForAPresentId() {
        whenSeLeElValorDe(BloqueId.de(UUID_FIJO));

        thenElValorEs(UUID_FIJO);
    }

    @Test
    @DisplayName("Un UUID que puede faltar da un id si está")
    void opcionalShouldWrapAPresentUuid() {
        whenSeArmaOpcional(UUID_FIJO);

        thenElIdOpcionalEs(BloqueId.de(UUID_FIJO));
    }

    @Test
    @DisplayName("Un UUID que puede faltar no da id si falta")
    void opcionalShouldBeNullForAMissingUuid() {
        whenSeArmaOpcional(null);

        thenElIdOpcionalEs(null);
    }

    @Test
    @DisplayName("Los UUID de varios ids conservan el orden")
    void valoresShouldKeepOrder() {
        whenSeLeenLosValores(List.of(BloqueId.de(OTRO_UUID), BloqueId.de(UUID_FIJO)));

        thenLosValoresSon(OTRO_UUID, UUID_FIJO);
    }

    // --- act ---
    private void whenSeArmaDesdeUuidYTexto(Class<?> tipo) throws Exception {
        desdeUuid = (Identificador) tipo.getMethod("de", UUID.class).invoke(null, UUID_FIJO);
        desdeTexto = (Identificador) tipo.getMethod("de", String.class).invoke(null, UUID_FIJO.toString());
    }

    private void whenSeCreanDosIdsNuevos(Class<?> tipo) throws Exception {
        uno = tipo.getMethod("nuevo").invoke(null);
        otro = tipo.getMethod("nuevo").invoke(null);
    }

    private void whenSeArmaSinUuid(Class<?> tipo) {
        error = catchThrowable(() -> tipo.getMethod("de", UUID.class).invoke(null, (Object) null));
    }

    private void whenSeComparanIdsDeDistintoTipo() {
        sonIguales = BloqueId.de(UUID_FIJO).equals(CampoId.de(UUID_FIJO));
    }

    private void whenSeLeElValorDe(Identificador id) {
        valor = Identificador.valorDe(id);
    }

    private void whenSeArmaOpcional(UUID uuid) {
        idOpcional = Identificador.opcional(uuid, BloqueId::de);
    }

    private void whenSeLeenLosValores(List<BloqueId> ids) {
        valores = Identificador.valores(ids);
    }

    // --- assert ---
    private void thenElIdEnvuelveElUuid() {
        assertThat(desdeUuid.valor()).isEqualTo(UUID_FIJO);
        assertThat(desdeTexto).isEqualTo(desdeUuid);
        assertThat(desdeUuid).hasToString(UUID_FIJO.toString());
    }

    private void thenLosIdsSonDistintos() {
        assertThat(uno).isNotEqualTo(otro);
    }

    private void thenSeRechazaPorObligatorio() {
        assertThat(error).isInstanceOf(InvocationTargetException.class)
                .cause().isInstanceOf(NullPointerException.class).hasMessageContaining("obligatorio");
    }

    private void thenNoSonIguales() {
        assertThat(sonIguales).isFalse();
    }

    private void thenElValorEs(UUID esperado) {
        assertThat(valor).isEqualTo(esperado);
    }

    private void thenElIdOpcionalEs(BloqueId esperado) {
        assertThat(idOpcional).isEqualTo(esperado);
    }

    private void thenLosValoresSon(UUID... esperados) {
        assertThat(valores).containsExactly(esperados);
    }
}
