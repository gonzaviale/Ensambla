package com.ensambla.backend.domain.model.common;

import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.inventario.MovimientoStockId;
import com.ensambla.backend.domain.model.lote.FotoId;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentificadorTest {

    private static final UUID UUID_FIJO = UUID.fromString("a0000000-0000-4000-8000-000000000001");

    static Stream<Class<?>> tipos() {
        return Stream.of(AvisoId.class, BloqueId.class, CampoId.class, MovimientoStockId.class, FotoId.class,
                LoteId.class, NegocioId.class, RenovacionId.class, PlanId.class, SuscripcionId.class,
                PlantillaId.class, ReclamoId.class, ProyectoId.class, RecetaId.class, RegistroId.class,
                SeccionId.class, UsuarioId.class, ZonaId.class, VinculoWhatsappId.class, CodigoVinculacionId.class,
                MensajeWhatsappId.class);
    }

    @ParameterizedTest
    @MethodSource("tipos")
    @DisplayName("Cada id envuelve su UUID, se arma desde UUID o texto y se lee como el UUID")
    void idShouldWrapUuid(Class<?> tipo) throws Exception {
        Identificador desdeUuid = (Identificador) tipo.getMethod("de", UUID.class).invoke(null, UUID_FIJO);
        Identificador desdeTexto = (Identificador) tipo.getMethod("de", String.class).invoke(null, UUID_FIJO.toString());

        assertThat(desdeUuid.valor()).isEqualTo(UUID_FIJO);
        assertThat(desdeTexto).isEqualTo(desdeUuid);
        assertThat(desdeUuid).hasToString(UUID_FIJO.toString());
    }

    @ParameterizedTest
    @MethodSource("tipos")
    @DisplayName("Un id nuevo es distinto cada vez")
    void nuevoShouldCreateDifferentIds(Class<?> tipo) throws Exception {
        Object uno = tipo.getMethod("nuevo").invoke(null);
        Object otro = tipo.getMethod("nuevo").invoke(null);

        assertThat(uno).isNotEqualTo(otro);
    }

    @ParameterizedTest
    @MethodSource("tipos")
    @DisplayName("Un id sin UUID no se puede armar")
    void idShouldRejectNull(Class<?> tipo) {
        assertThatThrownBy(() -> tipo.getMethod("de", UUID.class).invoke(null, (Object) null))
                .isInstanceOf(InvocationTargetException.class)
                .cause().isInstanceOf(NullPointerException.class).hasMessageContaining("obligatorio");
    }

    @Test
    @DisplayName("Dos ids de tipo distinto con el mismo UUID no son iguales")
    void idsOfDifferentTypesShouldNotBeEqual() {
        assertThat((Object) BloqueId.de(UUID_FIJO)).isNotEqualTo(CampoId.de(UUID_FIJO));
    }

    @Test
    @DisplayName("El valor de un id que puede faltar es nulo si falta")
    void valorDeShouldBeNullForMissingId() {
        assertThat(Identificador.valorDe(null)).isNull();
        assertThat(Identificador.valorDe(BloqueId.de(UUID_FIJO))).isEqualTo(UUID_FIJO);
    }

    @Test
    @DisplayName("Un UUID que puede faltar da un id sólo si está")
    void opcionalShouldWrapOnlyPresentUuid() {
        BloqueId presente = Identificador.opcional(UUID_FIJO, BloqueId::de);
        BloqueId ausente = Identificador.opcional(null, BloqueId::de);

        assertThat(presente).isEqualTo(BloqueId.de(UUID_FIJO));
        assertThat(ausente).isNull();
    }

    @Test
    @DisplayName("Los UUID de varios ids conservan el orden")
    void valoresShouldKeepOrder() {
        UUID otro = UUID.fromString("a0000000-0000-4000-8000-000000000002");

        List<UUID> result = Identificador.valores(List.of(BloqueId.de(otro), BloqueId.de(UUID_FIJO)));

        assertThat(result).containsExactly(otro, UUID_FIJO);
    }
}
