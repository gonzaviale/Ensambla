package com.ensambla.backend.domain.service.procesamiento;

import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionGuardada;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura.CampoConfirmado;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura.FilaConfirmada;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.lote.LoteSubidaService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Guarda de una sola vez lo que la persona revisó de una lectura: el bloque y sus campos (o los de un bloque existente)
 * y los registros, cada uno vinculado a su foto. Corre en una transacción: si algo falla no queda nada a medias.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConfirmacionLecturaService {

    private final ProyectoService proyectoService;

    private final BloqueService bloqueService;

    private final CampoService campoService;

    private final RegistroDatosService registroDatosService;

    private final LoteSubidaService loteSubidaService;

    /** {@code lote} ya viene validado contra el negocio de la sesión y con sus fotos cargadas. */
    @Transactional
    public ConfirmacionGuardada confirmar(UUID negocioId, LoteSubida lote, ConfirmacionLectura confirmacion,
                                          OffsetDateTime ahora) {
        lote.validarConfirmable();
        confirmacion.validar();
        Bloque bloque = destino(negocioId, confirmacion, ahora);
        Map<String, Campo> campos = prepararCampos(negocioId, bloque, confirmacion, ahora);
        List<Registro> registros = armarRegistros(negocioId, bloque, campos, lote, confirmacion, ahora);
        registroDatosService.crearVarios(registros);
        loteSubidaService.confirmar(lote, bloque.getId(), ahora);
        return new ConfirmacionGuardada(bloque.getProyectoId(), bloque.getId(), registros.size());
    }

    private Bloque destino(UUID negocioId, ConfirmacionLectura confirmacion, OffsetDateTime ahora) {
        if (confirmacion.bloqueId() != null) {
            return bloqueService.obtener(confirmacion.bloqueId(), negocioId);
        }
        Proyecto proyecto = confirmacion.proyectoId() != null
                ? proyectoService.obtener(negocioId, confirmacion.proyectoId())
                : proyectoService.crear(negocioId, confirmacion.nombreProyecto(), null, ahora);
        return bloqueService.crear(proyecto, confirmacion.nombreBloque(), ahora);
    }

    /** Las columnas con campo existente se reutilizan; las demás se crean al final del bloque. */
    private Map<String, Campo> prepararCampos(UUID negocioId, Bloque bloque, ConfirmacionLectura confirmacion,
                                              OffsetDateTime ahora) {
        Map<UUID, Campo> existentes = campoService.listarDeBloque(bloque.getId(), negocioId).stream()
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
            return campoService.crear(bloque, configuracion, ahora);
        } catch (CampoInvalidoException e) {
            throw new CampoInvalidoException(ubicar("campos[" + indice + "].", e.getErrores()));
        }
    }

    private Campo campoExistente(Map<UUID, Campo> existentes, UUID campoId) {
        Campo campo = existentes.get(campoId);
        if (campo == null) {
            throw new ResourceNotFoundException("El campo no existe");
        }
        return campo;
    }

    private List<Registro> armarRegistros(UUID negocioId, Bloque bloque, Map<String, Campo> campos, LoteSubida lote,
                                          ConfirmacionLectura confirmacion, OffsetDateTime ahora) {
        List<Campo> delBloque = campoService.listarDeBloque(bloque.getId(), negocioId);
        Map<String, String> clavePorCampoId = new HashMap<>();
        campos.forEach((clave, campo) -> clavePorCampoId.put(campo.getId().toString(), clave));
        Map<String, String> errores = new LinkedHashMap<>();
        List<Registro> registros = new ArrayList<>();
        for (int i = 0; i < confirmacion.filas().size(); i++) {
            FilaConfirmada fila = confirmacion.filas().get(i);
            UUID fotoOrigenId = fotoOrigen(lote, fila, "filas[" + i + "].pagina", errores);
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

    private UUID fotoOrigen(LoteSubida lote, FilaConfirmada fila, String ubicacion, Map<String, String> errores) {
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
