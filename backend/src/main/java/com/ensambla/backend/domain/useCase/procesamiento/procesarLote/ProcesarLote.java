package com.ensambla.backend.domain.useCase.procesamiento.procesarLote;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.BloquePropuesto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.PaginaCuaderno;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.FotoLectura;
import com.ensambla.backend.domain.model.procesamiento.LoteTrabajo;
import com.ensambla.backend.domain.model.procesamiento.PoliticaProcesamiento;
import com.ensambla.backend.domain.model.procesamiento.PropuestaLectura;
import com.ensambla.backend.domain.model.procesamiento.ResultadoIntento;
import com.ensambla.backend.domain.model.registro.Referencia;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Worker de la cola de cuadernos: libera los reclamos vencidos, toma un lote, lo lee con la IA y guarda el resultado.
 * El texto leído en columnas de conexión (RELACION) se convierte en la fila a la que apunta: se busca en la tabla
 * conectada una fila cuya etiqueta sea igual (sin importar tildes ni mayúsculas). Si no hay ninguna, o hay más de una,
 * la celda queda vacía con confianza 0 y se avisa, para que la persona la elija en la revisión.
 */
@RequiredArgsConstructor
public class ProcesarLote {

    public static final String ERROR_TEMPORAL = "No pudimos procesar el cuaderno. Probá nuevamente más tarde.";
    public static final String RECLAMO_VENCIDO = "Se agotó el tiempo de procesamiento del cuaderno.";

    static final double SIN_CONFIANZA = 0.0;

    private final ProcesamientoRepository procesamientoRepository;
    private final ArchivoCuadernoPort archivoCuadernoPort;
    private final LectorCuadernoPort lectorCuadernoPort;
    private final CampoRepository campoRepository;
    private final RegistroDatosService registroDatosService;
    private final ControlPlanService controlPlanService;
    private final PoliticaProcesamiento politicaProcesamiento;
    private final Clock clock;

    public void execute() {
        recuperar(OffsetDateTime.now(clock));
        reclamar(OffsetDateTime.now(clock)).ifPresent(lote ->
                finalizar(lote, leer(lote, OffsetDateTime.now(clock)), OffsetDateTime.now(clock)));
    }

    private Optional<LoteTrabajo> reclamar(OffsetDateTime ahora) {
        return procesamientoRepository.reclamar(ahora, ahora.plus(politicaProcesamiento.vigenciaReclamo()));
    }

    private void recuperar(OffsetDateTime ahora) {
        procesamientoRepository.vencidos(ahora).forEach(lote ->
                finalizar(lote, new ResultadoIntento(null, RECLAMO_VENCIDO, true), ahora));
    }

    private ResultadoIntento leer(LoteTrabajo lote, OffsetDateTime ahora) {
        try {
            List<FotoLectura> fotos = procesamientoRepository.fotos(lote);
            validarPaginas(fotos);
            List<PaginaCuaderno> paginas = paginas(lote, fotos);
            List<BloqueLectura> bloques = procesamientoRepository.bloques(lote.negocioId());
            if (!reservar(lote, paginas.size(), ahora)) {
                return new ResultadoIntento(null, PoliticaProcesamiento.LIMITE_DIARIO_AGOTADO, false);
            }
            List<BloqueConocido> contexto = bloques.stream()
                    .filter(bloque -> lote.bloqueDestinoId() == null || bloque.id().equals(lote.bloqueDestinoId()))
                    .map(bloque -> bloque.paraElLector(bloques))
                    .toList();
            LecturaCuaderno lectura = lectorCuadernoPort.leer(paginas, contexto, lote.bloqueDestinoId());
            PropuestaLectura propuesta = PropuestaLectura.proponer(lectura, bloques, lote.bloqueDestinoId());
            return new ResultadoIntento(resolverConexiones(lote.negocioId(), propuesta, bloques), null, false);
        } catch (BusinessRuleException e) {
            return new ResultadoIntento(null, e.getMessage(), false);
        } catch (LecturaCuadernoException e) {
            return new ResultadoIntento(null, e.getMessage(), e.esRecuperable());
        } catch (RuntimeException e) {
            return new ResultadoIntento(null, ERROR_TEMPORAL, true);
        }
    }

    /** Las cuotas mensual/de prueba y la reserva diaria comparten transacción y bloqueo por negocio. */
    private boolean reservar(LoteTrabajo lote, int paginas, OffsetDateTime ahora) {
        return controlPlanService.reservarPaginasIa(lote.negocioId(), paginas, ahora, () ->
                procesamientoRepository.reservar(lote, paginas, politicaProcesamiento.limiteDiario(), ahora));
    }

    private void finalizar(LoteTrabajo lote, ResultadoIntento resultado, OffsetDateTime ahora) {
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

    private PropuestaLectura resolverConexiones(NegocioId negocioId, PropuestaLectura propuesta,
                                                List<BloqueLectura> bloques) {
        Map<String, BloqueLectura> conectadas = tablasConectadas(propuesta, bloques);
        if (conectadas.isEmpty()) {
            return propuesta;
        }
        Resolucion resolucion = new Resolucion(negocioId);
        List<FilaLeida> filas = propuesta.lectura().filas().stream()
                .map(fila -> resolverFila(fila, conectadas, resolucion))
                .toList();
        List<String> advertencias = new ArrayList<>(propuesta.lectura().advertencias());
        advertencias.addAll(resolucion.advertencias);
        LecturaCuaderno lectura = propuesta.lectura();
        return new PropuestaLectura(new LecturaCuaderno(lectura.tipoRegistro(), lectura.bloque(), filas, advertencias,
                lectura.consumo()), propuesta.mapeoCampos(), propuesta.camposNuevos());
    }

    /** Clave de cada columna de conexión del bloque destino → tabla con la que se conecta. */
    private Map<String, BloqueLectura> tablasConectadas(PropuestaLectura propuesta, List<BloqueLectura> bloques) {
        BloquePropuesto propuesto = propuesta.lectura().bloque();
        Optional<BloqueLectura> destino = propuesto.bloqueExistente()
                .flatMap(id -> bloques.stream().filter(bloque -> bloque.id().equals(id)).findFirst());
        Map<String, BloqueLectura> conectadas = new LinkedHashMap<>();
        for (CampoPropuesto campo : propuesto.campos()) {
            if (campo.tipo() != TipoCampo.RELACION) {
                continue;
            }
            destino.flatMap(bloque -> conocido(bloque, propuesta.mapeoCampos().get(campo.clave())))
                    .map(CampoConocido::bloqueRelacionadoId)
                    .flatMap(id -> bloques.stream().filter(bloque -> bloque.id().equals(id)).findFirst())
                    .ifPresent(tabla -> conectadas.put(campo.clave(), tabla));
        }
        return conectadas;
    }

    private Optional<CampoConocido> conocido(BloqueLectura bloque, CampoId campoId) {
        return bloque.campos().stream().filter(campo -> campo.id().equals(campoId)).findFirst();
    }

    private FilaLeida resolverFila(FilaLeida fila, Map<String, BloqueLectura> conectadas, Resolucion resolucion) {
        Map<String, DatoLeido> datos = new LinkedHashMap<>(fila.datos());
        conectadas.forEach((clave, tabla) -> {
            DatoLeido dato = datos.get(clave);
            if (dato != null && dato.valor() != null && !dato.valor().isBlank()) {
                datos.put(clave, resolucion.resolver(tabla, dato));
            }
        });
        return new FilaLeida(fila.pagina(), fila.renglon(), datos);
    }

    /** Una pasada por lectura: cada tabla conectada se consulta una vez por texto distinto. */
    private final class Resolucion {

        private final NegocioId negocioId;
        private final Map<BloqueId, List<Campo>> camposPorTabla = new HashMap<>();
        private final Map<String, Optional<RegistroId>> encontradas = new HashMap<>();
        private final Set<String> avisadas = new HashSet<>();
        private final Set<String> advertencias = new LinkedHashSet<>();

        private Resolucion(NegocioId negocioId) {
            this.negocioId = negocioId;
        }

        private DatoLeido resolver(BloqueLectura tabla, DatoLeido dato) {
            String clave = tabla.id() + ":" + TextoNormalizado.de(dato.valor());
            Optional<RegistroId> fila = encontradas
                    .computeIfAbsent(clave, ignorada -> buscar(tabla.id(), dato.valor()));
            if (fila.isPresent()) {
                return new DatoLeido(fila.get().toString(), dato.confianza());
            }
            if (avisadas.add(clave)) {
                advertencias.add("No encontramos «%s» en «%s», o hay más de una igual: elegila vos."
                        .formatted(dato.valor().strip(), tabla.nombre()));
            }
            return new DatoLeido(null, SIN_CONFIANZA);
        }

        private Optional<RegistroId> buscar(BloqueId tablaId, String texto) {
            List<Campo> campos = camposPorTabla.computeIfAbsent(tablaId,
                    id -> campoRepository.findByBloqueIdAndNegocioId(id, negocioId));
            var etiquetar = Referencia.etiquetador(campos);
            List<Referencia> iguales = registroDatosService
                    .buscarReferenciables(negocioId, tablaId, campos, texto, List.of()).stream()
                    .map(etiquetar)
                    .filter(referencia ->
                            TextoNormalizado.de(referencia.etiqueta()).equals(TextoNormalizado.de(texto)))
                    .toList();
            return iguales.size() == 1 ? Optional.of(iguales.getFirst().id()) : Optional.empty();
        }
    }
}
