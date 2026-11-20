package com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MedidorMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ProgresoObjetivo;
import com.ensambla.backend.domain.model.objetivo.ResumenSemanal;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ProgresoObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ResumenSemanalRepository;
import com.ensambla.backend.domain.service.objetivo.PropuestaMiniObjetivosService;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgresoResponse.MiniProgreso;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgresoResponse.PuntoSemanal;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lo que el monitoreo midió de un objetivo del negocio de la sesión, semana por semana. Las semanas anteriores a la
 * primera foto del objetivo se completan con el resultado que se calcula con los cuadernos, marcadas como estimadas. Una
 * semana ya cerrada muestra lo que dio su cierre del domingo: el mismo número que su resumen.
 * Un objetivo ajeno responde como inexistente.
 */
@RequiredArgsConstructor
public class ObtenerProgreso {

    private final NegocioRepository negocioRepository;

    private final ObjetivoRepository objetivoRepository;

    private final MiniObjetivoRepository miniObjetivoRepository;

    private final ProgresoObjetivoRepository progresoObjetivoRepository;

    private final PropuestaMiniObjetivosService propuestaMiniObjetivosService;

    private final ResumenSemanalRepository resumenSemanalRepository;

    private final Clock clock;

    public ObtenerProgresoResponse execute(ObtenerProgresoRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Objetivo objetivo = obtenerObjetivo(negocioId, request);
        LocalDate hoy = ZonaNegocio.dia(OffsetDateTime.now(clock));
        List<ProgresoObjetivo> fotos = obtenerFotos(objetivo);
        List<MiniProgreso> minis = progresoDeLosMinis(negocioId, objetivo, hoy, fotos);
        return ObtenerProgresoResponse.de(objetivo, serieDelObjetivo(negocioId, objetivo, hoy, fotos), minis);
    }

    private List<MiniProgreso> progresoDeLosMinis(
            NegocioId negocioId, Objetivo objetivo, LocalDate hoy, List<ProgresoObjetivo> fotos) {
        return obtenerMinis(negocioId, objetivo).stream()
                .filter(MiniObjetivo::esMedido)
                .filter(mini -> mini.estaEnCurso() || mini.getEstado() == EstadoMiniObjetivo.LOGRADO)
                .map(mini -> MiniProgreso.de(mini, hoy, fotosDe(mini, fotos)))
                .toList();
    }

    private static List<ProgresoObjetivo> fotosDe(MiniObjetivo mini, List<ProgresoObjetivo> fotos) {
        return fotos.stream().filter(foto -> mini.getId().equals(foto.miniObjetivoId())).toList();
    }

    private List<PuntoSemanal> serieDelObjetivo(
            NegocioId negocioId, Objetivo objetivo, LocalDate hoy, List<ProgresoObjetivo> fotos) {
        List<PuntoSemanal> medidos = puntosMedidos(fotos);
        List<PuntoSemanal> serie = new ArrayList<>(estimadosAnterioresA(negocioId, hoy, medidos));
        serie.addAll(medidos);
        return conElResultadoDelCierre(serie, resultadosCerrados(objetivo));
    }

    private static List<PuntoSemanal> puntosMedidos(List<ProgresoObjetivo> fotos) {
        return ProgresoObjetivo.ultimaDeCadaSemana(fotos.stream().filter(ProgresoObjetivo::esDelObjetivo).toList())
                .stream()
                .map(PuntoSemanal::de)
                .toList();
    }

    private Map<LocalDate, BigDecimal> resultadosCerrados(Objetivo objetivo) {
        return obtenerResumenes(objetivo).stream()
                .collect(Collectors.toMap(ResumenSemanal::semanaInicio, ResumenSemanal::resultado));
    }

    private static List<PuntoSemanal> conElResultadoDelCierre(
            List<PuntoSemanal> serie, Map<LocalDate, BigDecimal> cierres) {
        return serie.stream().map(punto -> conElResultadoDelCierre(punto, cierres)).toList();
    }

    private static PuntoSemanal conElResultadoDelCierre(PuntoSemanal punto, Map<LocalDate, BigDecimal> cierres) {
        if (!cierres.containsKey(punto.semana())) {
            return punto;
        }
        return new PuntoSemanal(punto.semana(), cierres.get(punto.semana()), false);
    }

    /** Las semanas de las que se sabe el resultado pero que el monitoreo no midió, antes de su primera foto. */
    private List<PuntoSemanal> estimadosAnterioresA(NegocioId negocioId, LocalDate hoy, List<PuntoSemanal> medidos) {
        return medirElNegocio(negocioId, hoy).resultadoPorSemana().entrySet().stream()
                .filter(semana -> medidos.isEmpty() || semana.getKey().isBefore(medidos.getFirst().semana()))
                .map(semana -> new PuntoSemanal(semana.getKey(), semana.getValue(), true))
                .toList();
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Objetivo obtenerObjetivo(NegocioId negocioId, ObtenerProgresoRequest request) {
        return objetivoRepository.obtener(negocioId, request.objetivoId());
    }

    private List<ProgresoObjetivo> obtenerFotos(Objetivo objetivo) {
        return progresoObjetivoRepository.findAllByObjetivoId(objetivo.getId());
    }

    private List<MiniObjetivo> obtenerMinis(NegocioId negocioId, Objetivo objetivo) {
        return miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(objetivo.getId(), negocioId);
    }

    private List<ResumenSemanal> obtenerResumenes(Objetivo objetivo) {
        return resumenSemanalRepository.findAllByObjetivoId(objetivo.getId());
    }

    private MedidorMiniObjetivo medirElNegocio(NegocioId negocioId, LocalDate hoy) {
        return propuestaMiniObjetivosService.medidor(negocioId, hoy);
    }
}
