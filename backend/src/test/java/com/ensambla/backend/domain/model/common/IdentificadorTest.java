package com.ensambla.backend.domain.model.common;

import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.caja.VentaCajaId;
import com.ensambla.backend.domain.model.caja.CierreCajaId;
import com.ensambla.backend.domain.model.caja.CobroCajaId;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.inventario.MovimientoStockId;
import com.ensambla.backend.domain.model.lote.FotoId;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.metrica.MetricaId;
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
    private Object nuevo;
    private Object otroNuevo;
    private Throwable rechazo;
    private UUID valor;
    private BloqueId opcionalPresente;
    private BloqueId opcionalAusente;
    private List<UUID> valores;

    static Stream<Class<?>> tipos() {
        return Stream.of(CobroCajaId.class, CierreCajaId.class, VentaCajaId.class, AvisoId.class, BloqueId.class,
                CampoId.class, MovimientoStockId.class, FotoId.class, LoteId.class, NegocioId.class,
                RenovacionId.class, PlanId.class, SuscripcionId.class, PlantillaId.class, ReclamoId.class,
                ProyectoId.class, RecetaId.class, RegistroId.class, SeccionId.class, UsuarioId.class, ZonaId.class,
                VinculoWhatsappId.class, CodigoVinculacionId.class, PropuestaWhatsappId.class,
                VerificacionWhatsappId.class, MensajeWhatsappId.class, ObjetivoId.class, MiniObjetivoId.class,
                ProgresoObjetivoId.class, DecisionObjetivoId.class, ResumenSemanalId.class,
                ReajusteMiniObjetivoId.class, RespuestaMiniObjetivoId.class, MetricaId.class);
    }

    @ParameterizedTest
    @MethodSource("tipos")
    @DisplayName("Cada id envuelve su UUID, se arma desde UUID o texto y se lee como el UUID")
    void cadaIdEnvuelveSuUuidSeArmaDesdeUuidOTextoYSeLeeComoElUuid(Class<?> tipo) throws Exception {
        cuandoSeArmaElIdDesdeUuidYDesdeTexto(tipo);

        entoncesAmbosIdsEnvuelvenElUuidYSeLeenComoEl();
    }

    @ParameterizedTest
    @MethodSource("tipos")
    @DisplayName("Un id nuevo es distinto cada vez")
    void unIdNuevoEsDistintoCadaVez(Class<?> tipo) throws Exception {
        cuandoSeCreanDosIdsNuevos(tipo);

        entoncesLosIdsNuevosSonDistintos();
    }

    @ParameterizedTest
    @MethodSource("tipos")
    @DisplayName("Un id sin UUID no se puede armar")
    void unIdSinUuidNoSePuedeArmar(Class<?> tipo) {
        cuandoSeIntentaArmarElIdSinUuid(tipo);

        entoncesSeRechazaPorqueElUuidEsObligatorio();
    }

    @Test
    @DisplayName("Dos ids de tipo distinto con el mismo UUID no son iguales")
    void dosIdsDeTipoDistintoConElMismoUuidNoSonIguales() {
        cuandoSeArmanUnBloqueIdYUnCampoIdConElMismoUuid();

        entoncesLosIdsDeDistintoTipoNoSonIguales();
    }

    @Test
    @DisplayName("El valor de un id que falta es nulo")
    void elValorDeUnIdQueFaltaEsNulo() {
        cuandoSePideElValorDeUnIdAusente();

        entoncesElValorEsNulo();
    }

    @Test
    @DisplayName("El valor de un id presente es su UUID")
    void elValorDeUnIdPresenteEsSuUuid() {
        cuandoSePideElValorDeUnIdPresente();

        entoncesElValorEsElUuid();
    }

    @Test
    @DisplayName("Un UUID que puede faltar da un id sólo si está")
    void unUuidQuePuedeFaltarDaUnIdSoloSiEsta() {
        cuandoSeEnvuelvenUnUuidPresenteYUnoAusente();

        entoncesSoloElPresenteDaUnId();
    }

    @Test
    @DisplayName("Los UUID de varios ids conservan el orden")
    void losUuidDeVariosIdsConservanElOrden() {
        cuandoSePidenLosValoresDeDosIds();

        entoncesLosValoresConservanElOrden();
    }

    // --- cuando ---
    private void cuandoSeArmaElIdDesdeUuidYDesdeTexto(Class<?> tipo) throws Exception {
        desdeUuid = (Identificador) tipo.getMethod("de", UUID.class).invoke(null, UUID_FIJO);
        desdeTexto = (Identificador) tipo.getMethod("de", String.class).invoke(null, UUID_FIJO.toString());
    }

    private void cuandoSeCreanDosIdsNuevos(Class<?> tipo) throws Exception {
        nuevo = tipo.getMethod("nuevo").invoke(null);
        otroNuevo = tipo.getMethod("nuevo").invoke(null);
    }

    private void cuandoSeIntentaArmarElIdSinUuid(Class<?> tipo) {
        rechazo = catchThrowable(() -> tipo.getMethod("de", UUID.class).invoke(null, (Object) null));
    }

    private void cuandoSeArmanUnBloqueIdYUnCampoIdConElMismoUuid() {
        desdeUuid = BloqueId.de(UUID_FIJO);
        desdeTexto = CampoId.de(UUID_FIJO);
    }

    private void cuandoSePideElValorDeUnIdAusente() {
        valor = Identificador.valorDe(null);
    }

    private void cuandoSePideElValorDeUnIdPresente() {
        valor = Identificador.valorDe(BloqueId.de(UUID_FIJO));
    }

    private void cuandoSeEnvuelvenUnUuidPresenteYUnoAusente() {
        opcionalPresente = Identificador.opcional(UUID_FIJO, BloqueId::de);
        opcionalAusente = Identificador.opcional(null, BloqueId::de);
    }

    private void cuandoSePidenLosValoresDeDosIds() {
        valores = Identificador.valores(List.of(BloqueId.de(OTRO_UUID), BloqueId.de(UUID_FIJO)));
    }

    // --- entonces ---
    private void entoncesAmbosIdsEnvuelvenElUuidYSeLeenComoEl() {
        assertThat(desdeUuid.valor()).isEqualTo(UUID_FIJO);
        assertThat(desdeTexto).isEqualTo(desdeUuid);
        assertThat(desdeUuid).hasToString(UUID_FIJO.toString());
    }

    private void entoncesLosIdsNuevosSonDistintos() {
        assertThat(nuevo).isNotEqualTo(otroNuevo);
    }

    private void entoncesSeRechazaPorqueElUuidEsObligatorio() {
        assertThat(rechazo)
                .isInstanceOf(InvocationTargetException.class)
                .cause().isInstanceOf(NullPointerException.class).hasMessageContaining("obligatorio");
    }

    private void entoncesLosIdsDeDistintoTipoNoSonIguales() {
        assertThat((Object) desdeUuid).isNotEqualTo(desdeTexto);
    }

    private void entoncesElValorEsNulo() {
        assertThat(valor).isNull();
    }

    private void entoncesElValorEsElUuid() {
        assertThat(valor).isEqualTo(UUID_FIJO);
    }

    private void entoncesSoloElPresenteDaUnId() {
        assertThat(opcionalPresente).isEqualTo(BloqueId.de(UUID_FIJO));
        assertThat(opcionalAusente).isNull();
    }

    private void entoncesLosValoresConservanElOrden() {
        assertThat(valores).containsExactly(OTRO_UUID, UUID_FIJO);
    }
}
