package com.ensambla.backend.domain.model.lote;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.LoteTestData.AHORA;
import static com.ensambla.backend.support.LoteTestData.BLOQUE_ID;
import static com.ensambla.backend.support.LoteTestData.FOTO_ID;
import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.LotesDePrueba.fotosJpeg;
import static com.ensambla.backend.support.LotesDePrueba.unLoteConUnaFoto;
import static com.ensambla.backend.support.LotesDePrueba.unLoteEn;
import static com.ensambla.backend.support.LotesDePrueba.unaFotoJpeg;
import static com.ensambla.backend.support.LotesDePrueba.unaFotoPng;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class LoteSubidaTest {

    private LoteSubida lote;
    private Optional<FotoCuaderno> foto;
    private Throwable rechazo;

    @Test
    @DisplayName("Un lote nuevo queda pendiente, sin intentos y con las fotos en el orden recibido")
    void unLoteNuevoQuedaPendienteSinIntentosYConLasFotosEnElOrdenRecibido() {
        cuandoSeArmaUnLoteConUnaFotoJpegYUnaPng(null);

        entoncesElLoteQuedaPendienteSinIntentos();
        entoncesLasFotosSiguenElOrdenRecibido();
    }

    @Test
    @DisplayName("Un lote nuevo recuerda el bloque destino que eligió la persona")
    void unLoteNuevoRecuerdaElBloqueDestinoQueEligioLaPersona() {
        cuandoSeArmaUnLoteConUnaFotoJpegYUnaPng(BLOQUE_ID);

        entoncesElLoteRecuerdaElBloqueDestino();
    }

    @Test
    @DisplayName("Un lote nuevo sin bloque destino deja que la IA proponga uno")
    void unLoteNuevoSinBloqueDestinoDejaQueLaIaProponga() {
        cuandoSeArmaUnLoteConUnaFotoJpegYUnaPng(null);

        entoncesElLoteNoTieneBloqueDestino();
    }

    @Test
    @DisplayName("Un lote sin fotos se rechaza")
    void unLoteSinFotosSeRechaza() {
        cuandoSeIntentaArmarUnLoteConFotos(0);

        entoncesSeRechazaPorque("Subí al menos una foto");
    }

    @Test
    @DisplayName("Un lote con más fotos de las que la IA lee de una vez se rechaza")
    void unLoteConMasFotosDeLasQueLaIaLeeDeUnaVezSeRechaza() {
        cuandoSeIntentaArmarUnLoteConFotos(LoteSubida.MAX_FOTOS + 1);

        entoncesSeRechazaPorque("Podés subir hasta 10 fotos por lote");
    }

    @Test
    @DisplayName("Un lote con exactamente el máximo de fotos se acepta")
    void unLoteConExactamenteElMaximoDeFotosSeAcepta() {
        cuandoSeArmaUnLoteConFotos(LoteSubida.MAX_FOTOS);

        entoncesElLoteTieneTodasLasFotos(LoteSubida.MAX_FOTOS);
    }

    @Test
    @DisplayName("Un lote con la lectura terminada se puede confirmar")
    void unLoteConLaLecturaTerminadaSePuedeConfirmar() {
        dadoUnLoteEn(EstadoLote.LISTO);

        cuandoSeIntentaConfirmarElLote();

        entoncesSePuedeConfirmar();
    }

    @Test
    @DisplayName("Un lote ya confirmado no se puede confirmar de nuevo")
    void unLoteYaConfirmadoNoSePuedeConfirmarDeNuevo() {
        dadoUnLoteEn(EstadoLote.CONFIRMADO);

        cuandoSeIntentaConfirmarElLote();

        entoncesSeRechazaPorque("Este cuaderno ya se guardó");
    }

    @Test
    @DisplayName("Un lote que todavía se está leyendo no se puede confirmar")
    void unLoteQueTodaviaSeEstaLeyendoNoSePuedeConfirmar() {
        dadoUnLoteEn(EstadoLote.PROCESANDO);

        cuandoSeIntentaConfirmarElLote();

        entoncesSeRechazaPorque("Este cuaderno todavía no terminó de leerse");
    }

    @Test
    @DisplayName("La página 1 es la foto de posición 0")
    void laPagina1EsLaFotoDePosicion0() {
        dadoUnLoteConUnaFoto();

        cuandoSeBuscaLaFotoDeLaPagina(1);

        entoncesSeEncuentraLaFotoDelLote();
    }

    @Test
    @DisplayName("Una página que el lote no tiene no devuelve foto")
    void unaPaginaQueElLoteNoTieneNoDevuelveFoto() {
        dadoUnLoteConUnaFoto();

        cuandoSeBuscaLaFotoDeLaPagina(2);

        entoncesNoSeEncuentraNingunaFoto();
    }

    @Test
    @DisplayName("Una foto se encuentra por su id")
    void unaFotoSeEncuentraPorSuId() {
        dadoUnLoteConUnaFoto();

        cuandoSeBuscaLaFotoConId(FOTO_ID);

        entoncesSeEncuentraLaFotoDelLote();
    }

    @Test
    @DisplayName("Un id que no es de ninguna foto del lote no devuelve foto")
    void unIdQueNoEsDeNingunaFotoDelLoteNoDevuelveFoto() {
        dadoUnLoteConUnaFoto();

        cuandoSeBuscaLaFotoConId(FotoId.nuevo());

        entoncesNoSeEncuentraNingunaFoto();
    }

    // --- dado ---
    private void dadoUnLoteConUnaFoto() {
        lote = unLoteConUnaFoto();
    }

    private void dadoUnLoteEn(EstadoLote estado) {
        lote = unLoteEn(estado);
    }

    // --- cuando ---
    private void cuandoSeArmaUnLoteConUnaFotoJpegYUnaPng(BloqueId destino) {
        lote = LoteSubida.nuevo(NEGOCIO_ID, List.of(unaFotoJpeg(), unaFotoPng()), destino, AHORA);
    }

    private void cuandoSeArmaUnLoteConFotos(int cantidad) {
        lote = LoteSubida.nuevo(NEGOCIO_ID, fotosJpeg(cantidad), null, AHORA);
    }

    private void cuandoSeIntentaArmarUnLoteConFotos(int cantidad) {
        rechazo = catchThrowable(() -> cuandoSeArmaUnLoteConFotos(cantidad));
    }

    private void cuandoSeIntentaConfirmarElLote() {
        rechazo = catchThrowable(lote::validarConfirmable);
    }

    private void cuandoSeBuscaLaFotoDeLaPagina(int pagina) {
        foto = lote.fotoDePagina(pagina);
    }

    private void cuandoSeBuscaLaFotoConId(FotoId fotoId) {
        foto = lote.fotoConId(fotoId);
    }

    // --- entonces ---
    private void entoncesElLoteQuedaPendienteSinIntentos() {
        assertThat(lote.getId()).isNull();
        assertThat(lote.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.PENDIENTE);
        assertThat(lote.getIntentos()).isZero();
        assertThat(lote.getCreadoEn()).isEqualTo(AHORA);
        assertThat(lote.getActualizadoEn()).isEqualTo(AHORA);
    }

    private void entoncesLasFotosSiguenElOrdenRecibido() {
        assertThat(lote.getFotos()).extracting(FotoCuaderno::getPosicion).containsExactly((short) 0, (short) 1);
        assertThat(lote.getFotos()).extracting(FotoCuaderno::getTipoContenido)
                .containsExactly("image/jpeg", "image/png");
    }

    private void entoncesElLoteRecuerdaElBloqueDestino() {
        assertThat(lote.getBloqueDestinoId()).isEqualTo(BLOQUE_ID);
    }

    private void entoncesElLoteNoTieneBloqueDestino() {
        assertThat(lote.getBloqueDestinoId()).isNull();
    }

    private void entoncesElLoteTieneTodasLasFotos(int cantidad) {
        assertThat(lote.getFotos()).hasSize(cantidad);
    }

    private void entoncesSePuedeConfirmar() {
        assertThat(rechazo).isNull();
    }

    private void entoncesSeRechazaPorque(String motivo) {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessage(motivo);
    }

    private void entoncesSeEncuentraLaFotoDelLote() {
        assertThat(foto).map(FotoCuaderno::getId).contains(FOTO_ID);
    }

    private void entoncesNoSeEncuentraNingunaFoto() {
        assertThat(foto).isEmpty();
    }
}
