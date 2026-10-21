package com.ensambla.backend.domain.service.procesamiento;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.CampoContexto;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.PaginaCuaderno;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.ConsumoDiario;
import com.ensambla.backend.domain.model.procesamiento.EstadoLectura;
import com.ensambla.backend.domain.model.procesamiento.FotoLectura;
import com.ensambla.backend.domain.model.procesamiento.LoteTrabajo;
import com.ensambla.backend.domain.model.procesamiento.PoliticaProcesamiento;
import com.ensambla.backend.domain.model.procesamiento.PropuestaLectura;
import com.ensambla.backend.domain.model.procesamiento.ResultadoIntento;
import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class ProcesamientoService {

    public static final String LIMITE_AGOTADO = "Se alcanzó el límite diario de páginas de IA. Probá mañana.";
    public static final String ERROR_TEMPORAL = "No pudimos procesar el cuaderno. Probá nuevamente más tarde.";
    public static final String RECLAMO_VENCIDO = "Se agotó el tiempo de procesamiento del cuaderno.";

    private final ProcesamientoRepository procesamientoRepository;
    private final ArchivoCuadernoPort archivoCuadernoPort;
    private final LectorCuadernoPort lectorCuadernoPort;
    private final PropuestaLecturaService propuestaLecturaService;
    private final ConexionesLecturaService conexionesLecturaService;
    private final PoliticaProcesamiento politicaProcesamiento;

    private final ReservaIaService reservaIaService;

    public Optional<LoteTrabajo> reclamar(OffsetDateTime ahora) {
        return procesamientoRepository.reclamar(ahora, ahora.plus(politicaProcesamiento.vigenciaReclamo()));
    }

    public void recuperar(OffsetDateTime ahora) {
        procesamientoRepository.vencidos(ahora).forEach(lote ->
                finalizar(lote, new ResultadoIntento(null, RECLAMO_VENCIDO, true), ahora));
    }

    public ResultadoIntento leer(LoteTrabajo lote, OffsetDateTime ahora) {
        try {
            List<FotoLectura> fotos = procesamientoRepository.fotos(lote);
            validarPaginas(fotos);
            List<PaginaCuaderno> paginas = paginas(lote, fotos);
            List<BloqueLectura> bloques = procesamientoRepository.bloques(lote.negocioId());
            if (!reservaIaService.reservar(lote, paginas.size(), politicaProcesamiento.limiteDiario(), ahora)) {
                return new ResultadoIntento(null, LIMITE_AGOTADO, false);
            }
            List<BloqueConocido> contexto = bloques.stream()
                    .filter(bloque -> lote.bloqueDestinoId() == null || bloque.id().equals(lote.bloqueDestinoId()))
                    .map(bloque -> conocido(bloque, bloques))
                    .toList();
            LecturaCuaderno lectura = lectorCuadernoPort.leer(paginas, contexto, lote.bloqueDestinoId());
            PropuestaLectura propuesta = propuestaLecturaService.proponer(lectura, bloques, lote.bloqueDestinoId());
            return new ResultadoIntento(conexionesLecturaService.resolver(lote.negocioId(), propuesta, bloques),
                    null, false);
        } catch (BusinessRuleException e) {
            return new ResultadoIntento(null, e.getMessage(), false);
        } catch (LecturaCuadernoException e) {
            return new ResultadoIntento(null, e.getMessage(), e.esRecuperable());
        } catch (RuntimeException e) {
            return new ResultadoIntento(null, ERROR_TEMPORAL, true);
        }
    }

    public void finalizar(LoteTrabajo lote, ResultadoIntento resultado, OffsetDateTime ahora) {
        OffsetDateTime reintentarEn = null;
        if (resultado.recuperable() && lote.intentos() < politicaProcesamiento.maxIntentos()) {
            reintentarEn = ahora.plus(politicaProcesamiento.demoraReintento());
        }
        BigDecimal costo = null;
        if (resultado.propuesta() != null) {
            costo = politicaProcesamiento.costo(resultado.propuesta().lectura().consumo());
        }
        procesamientoRepository.finalizar(lote, resultado, costo, ahora, reintentarEn);
    }

    public EstadoLectura obtener(UUID negocioId, UUID loteId) {
        return procesamientoRepository.obtener(negocioId, loteId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado"));
    }

    public ConsumoDiario consumo(UUID negocioId, OffsetDateTime ahora) {
        return procesamientoRepository.consumo(negocioId, ahora.toLocalDate());
    }

    public void reintentar(UUID negocioId, UUID loteId, OffsetDateTime ahora) {
        obtener(negocioId, loteId);
        reservaIaService.validarDisponibilidad(negocioId, ahora);
        ConsumoDiario consumo = consumo(negocioId, ahora);
        if (consumo.paginas() + consumo.reservadas() >= politicaProcesamiento.limiteDiario()) {
            throw new BusinessRuleException(LIMITE_AGOTADO);
        }
        if (!procesamientoRepository.reencolar(negocioId, loteId, ahora)) {
            throw new BusinessRuleException("Sólo se puede reintentar un lote en estado ERROR");
        }
    }

    private BloqueConocido conocido(BloqueLectura bloque, List<BloqueLectura> bloques) {
        return new BloqueConocido(bloque.id(), bloque.nombre(), bloque.campos().stream()
                .map(campo -> new CampoContexto(campo.nombre(), campo.tipo(), campo.opciones(),
                        nombreConectado(campo, bloques)))
                .toList());
    }

    private String nombreConectado(CampoConocido campo, List<BloqueLectura> bloques) {
        return bloques.stream()
                .filter(bloque -> bloque.id().equals(campo.bloqueRelacionadoId()))
                .map(BloqueLectura::nombre)
                .findFirst()
                .orElse(null);
    }

    private void validarPaginas(List<FotoLectura> fotos) {
        if (fotos.isEmpty() || fotos.size() > politicaProcesamiento.maxPaginas()) {
            throw new BusinessRuleException(
                    "El lote debe tener entre 1 y " + politicaProcesamiento.maxPaginas() + " páginas");
        }
    }

    private List<PaginaCuaderno> paginas(LoteTrabajo lote, List<FotoLectura> fotos) {
        List<PaginaCuaderno> paginas = new ArrayList<>();
        for (FotoLectura foto : fotos) {
            paginas.add(new PaginaCuaderno(paginas.size() + 1,
                    archivoCuadernoPort.leer(lote.negocioId(), foto.clave()), foto.tipoContenido()));
        }
        return paginas;
    }
}
