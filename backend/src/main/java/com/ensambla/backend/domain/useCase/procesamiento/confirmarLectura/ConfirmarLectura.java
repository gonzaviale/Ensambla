package com.ensambla.backend.domain.useCase.procesamiento.confirmarLectura;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.FotoId;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionGuardada;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura.CampoConfirmado;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura.FilaConfirmada;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Guarda de una sola vez lo que la persona revisó de una lectura: el bloque y sus campos (o los de un bloque existente)
 * y los registros, cada uno vinculado a su foto. Corre en una transacción: si algo falla no queda nada a medias.
 */
@RequiredArgsConstructor
public class ConfirmarLectura {

    private final NegocioRepository negocioRepository;
    private final ProyectoRepository proyectoRepository;
    private final BloqueRepository bloqueRepository;
    private final CampoRepository campoRepository;
    private final RegistroDatosService registroDatosService;
    private final ControlPlanService controlPlanService;
    private final RegistroRepository registroRepository;
    private final LoteSubidaRepository loteSubidaRepository;
    private final TransaccionPort transaccionPort;
    private final Clock clock;

    public ConfirmarLecturaResponse execute(ConfirmarLecturaRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        LoteSubida lote = loteSubidaRepository.obtener(negocioId, request.loteId());
        ConfirmacionGuardada guardada = confirmar(
                negocioId, lote, request.aConfirmacionLectura(), OffsetDateTime.now(clock));
        return ConfirmarLecturaResponse.de(guardada);
    }

    private ConfirmacionGuardada confirmar(NegocioId negocioId, LoteSubida lote, ConfirmacionLectura confirmacion,
                                           OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> {
            lote.validarConfirmable();
            confirmacion.validar();
            Bloque bloque = destino(negocioId, confirmacion, ahora);
            Map<String, Campo> campos = prepararCampos(negocioId, bloque, confirmacion, ahora);
            List<Registro> registros = armarRegistros(negocioId, bloque, campos, lote, confirmacion, ahora);
            controlPlanService.validarAltaRegistros(negocioId, registros.size(), ahora);
            registroRepository.saveAll(registros);
            marcarConfirmado(lote, bloque.getId(), ahora);
            return new ConfirmacionGuardada(bloque.getProyectoId(), bloque.getId(), registros.size());
        });
    }

    private Bloque destino(NegocioId negocioId, ConfirmacionLectura confirmacion, OffsetDateTime ahora) {
        if (confirmacion.bloqueId() != null) {
            return bloqueRepository.obtener(confirmacion.bloqueId(), negocioId);
        }
        Proyecto proyecto = confirmacion.proyectoId() != null
                ? proyectoRepository.obtener(negocioId, confirmacion.proyectoId())
                : crearProyecto(negocioId, confirmacion.nombreProyecto(), ahora);
        return bloqueRepository.save(proyecto.agregarBloque(confirmacion.nombreBloque(), ahora));
    }

    private Proyecto crearProyecto(NegocioId negocioId, String nombre, OffsetDateTime ahora) {
        controlPlanService.validarAltaProyecto(negocioId, ahora);
        return proyectoRepository.save(Proyecto.enBlanco(negocioId, nombre, null, ahora));
    }

    /**
     * Las columnas con campo existente se reutilizan; las demás se crean al final del bloque, con el nombre y el tipo
     * que la persona dejó al revisar. Si una falla no se crea ninguna: todo corre en la transacción de {@link #confirmar}.
     */
    private Map<String, Campo> prepararCampos(NegocioId negocioId, Bloque bloque, ConfirmacionLectura confirmacion,
                                              OffsetDateTime ahora) {
        Map<CampoId, Campo> existentes = campoRepository.findByBloqueIdAndNegocioId(bloque.getId(), negocioId).stream()
                .collect(Collectors.toMap(Campo::getId, Function.identity()));
        Map<String, Campo> porClave = new LinkedHashMap<>();
        for (int i = 0; i < confirmacion.campos().size(); i++) {
            CampoConfirmado campo = confirmacion.campos().get(i);
            porClave.put(campo.clave(), campo.campoId() == null
                    ? crearCampo(bloque, campo, i, ahora)
                    : campoExistente(existentes, campo.campoId()));
        }
        return porClave;
    }

    private Campo crearCampo(Bloque bloque, CampoConfirmado campo, int indice, OffsetDateTime ahora) {
        ConfiguracionCampo configuracion = new ConfiguracionCampo(
                campo.nombre(), campo.tipo(), false, null, campo.opciones(), null, null);
        try {
            bloque.setCampos(campoRepository.findByBloqueIdAndNegocioId(bloque.getId(), bloque.getNegocioId()));
            return campoRepository.save(bloque.agregarCampo(configuracion, ahora));
        } catch (CampoInvalidoException e) {
            throw new CampoInvalidoException(ubicar("campos[" + indice + "].", e.getErrores()));
        } catch (DuplicateResourceException e) {
            // Un nombre repetido (con un campo del bloque o con otra columna nueva) se ubica en su columna.
            throw new CampoInvalidoException(Map.of("campos[" + indice + "].nombre", e.getMessage()));
        }
    }

    private Campo campoExistente(Map<CampoId, Campo> existentes, CampoId campoId) {
        Campo campo = existentes.get(campoId);
        if (campo == null) {
            throw new ResourceNotFoundException("El campo no existe");
        }
        return campo;
    }

    private List<Registro> armarRegistros(NegocioId negocioId, Bloque bloque, Map<String, Campo> campos,
            LoteSubida lote,
                                          ConfirmacionLectura confirmacion, OffsetDateTime ahora) {
        List<Campo> delBloque = campoRepository.findByBloqueIdAndNegocioId(bloque.getId(), negocioId);
        Map<String, String> clavePorCampoId = new HashMap<>();
        campos.forEach((clave, campo) -> clavePorCampoId.put(campo.getId().toString(), clave));
        Map<String, String> errores = new LinkedHashMap<>();
        List<Registro> registros = new ArrayList<>();
        for (int i = 0; i < confirmacion.filas().size(); i++) {
            FilaConfirmada fila = confirmacion.filas().get(i);
            FotoId fotoOrigenId = fotoOrigen(lote, fila, "filas[" + i + "].pagina", errores);
            try {
                Map<String, Object> validos = Registro.validar(delBloque, valoresPorCampo(campos, fila));
                registroDatosService.validarConexiones(negocioId, delBloque, validos);
                registros.add(Registro.desdeCuaderno(negocioId, bloque.getId(), validos, fotoOrigenId, ahora));
            } catch (RegistroInvalidoException e) {
                errores.putAll(ubicarPorClave("filas[" + i + "].", e.getErrores(), clavePorCampoId));
            }
        }
        if (!errores.isEmpty()) {
            throw new RegistroInvalidoException(errores);
        }
        return registros;
    }

    private FotoId fotoOrigen(LoteSubida lote, FilaConfirmada fila, String ubicacion, Map<String, String> errores) {
        if (fila.pagina() == null) {
            return null;
        }
        return lote.fotoDePagina(fila.pagina())
                .map(FotoCuaderno::getId)
                .orElseGet(() -> {
                    errores.put(ubicacion, "Esta página no está en el cuaderno");
                    return null;
                });
    }

    /** Pasa de valores por clave de columna a valores por id de campo, que es como se guardan. */
    private Map<String, Object> valoresPorCampo(Map<String, Campo> campos, FilaConfirmada fila) {
        Map<String, Object> porCampo = new HashMap<>();
        campos.forEach((clave, campo) -> {
            if (fila.valores().containsKey(clave)) {
                porCampo.put(campo.getId().toString(), fila.valores().get(clave));
            }
        });
        return porCampo;
    }

    /** Si otra confirmación se adelantó, o el lote ya no estaba listo, no se guarda nada. */
    private void marcarConfirmado(LoteSubida lote, BloqueId bloqueId, OffsetDateTime ahora) {
        if (!loteSubidaRepository.confirmar(lote.getId(), lote.getNegocioId(), bloqueId, ahora)) {
            throw new BusinessRuleException("Este cuaderno ya se guardó");
        }
    }

    private static Map<String, String> ubicar(String prefijo, Map<String, String> errores) {
        Map<String, String> ubicados = new LinkedHashMap<>();
        errores.forEach((campo, mensaje) -> ubicados.put(prefijo + campo, mensaje));
        return ubicados;
    }

    private static Map<String, String> ubicarPorClave(String prefijo, Map<String, String> errores,
                                                      Map<String, String> clavePorCampoId) {
        Map<String, String> ubicados = new LinkedHashMap<>();
        errores.forEach((campoId, mensaje) ->
                ubicados.put(prefijo + clavePorCampoId.getOrDefault(campoId, campoId), mensaje));
        return ubicados;
    }
}
