package com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MedidorMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ProgresoObjetivo;
import com.ensambla.backend.domain.model.objetivo.ResumenSemanal;
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
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        Objetivo objetivo = objetivoRepository.obtener(negocioId, request.objetivoId());
        LocalDate hoy = ZonaNegocio.dia(OffsetDateTime.now(clock));
        List<ProgresoObjetivo> fotos = progresoObjetivoRepository.findAllByObjetivoId(objetivo.getId());
        List<MiniProgreso> minis = miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(objetivo.getId(), negocioId)
                .stream()
                .filter(mini -> mini.estaEnCurso() || mini.getEstado() == EstadoMiniObjetivo.LOGRADO)
                .map(mini -> MiniProgreso.de(mini, hoy, fotosDe(mini, fotos)))
                .toList();
        return ObtenerProgresoResponse.de(objetivo, serieDelObjetivo(negocioId, objetivo, hoy, fotos), minis);
    }

    private static List<ProgresoObjetivo> fotosDe(MiniObjetivo mini, List<ProgresoObjetivo> fotos) {
        return fotos.stream().filter(foto -> mini.getId().equals(foto.miniObjetivoId())).toList();
    }

    private List<PuntoSemanal> serieDelObjetivo(
            NegocioId negocioId, Objetivo objetivo, LocalDate hoy, List<ProgresoObjetivo> fotos) {
        List<PuntoSemanal> medidos = ProgresoObjetivo.ultimaDeCadaSemana(
                        fotos.stream().filter(ProgresoObjetivo::esDelObjetivo).toList()).stream()
                .map(PuntoSemanal::de)
                .toList();
        List<PuntoSemanal> serie = new ArrayList<>(estimadosAnterioresA(negocioId, hoy, medidos));
        serie.addAll(medidos);
        Map<LocalDate, BigDecimal> cierres = resumenSemanalRepository.findAllByObjetivoId(objetivo.getId()).stream()
                .collect(Collectors.toMap(ResumenSemanal::semanaInicio, ResumenSemanal::resultado));
        return serie.stream()
                .map(punto -> cierres.containsKey(punto.semana())
                        ? new PuntoSemanal(punto.semana(), cierres.get(punto.semana()), false)
                        : punto)
                .toList();
    }

    /** Las semanas de las que se sabe el resultado pero que el monitoreo no midió, antes de su primera foto. */
    private List<PuntoSemanal> estimadosAnterioresA(NegocioId negocioId, LocalDate hoy, List<PuntoSemanal> medidos) {
        MedidorMiniObjetivo medidor = propuestaMiniObjetivosService.medidor(negocioId, hoy);
        return medidor.resultadoPorSemana().entrySet().stream()
                .filter(semana -> medidos.isEmpty() || semana.getKey().isBefore(medidos.getFirst().semana()))
                .map(semana -> new PuntoSemanal(semana.getKey(), semana.getValue(), true))
                .toList();
    }
}
