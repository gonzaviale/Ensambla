package com.ensambla.backend.domain.useCase.registro.listarRegistros;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.TotalesRegistros;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class ListarRegistros {

    private final NegocioRepository negocioRepository;
    private final BloqueRepository bloqueRepository;
    private final CampoRepository campoRepository;
    private final RegistroRepository registroRepository;

    public ListarRegistrosResponse execute(ListarRegistrosRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Bloque bloque = obtenerBloque(request.bloqueId(), negocioId);
        List<Campo> campos = obtenerCamposDelBloque(bloque, negocioId);
        ConsultaRegistros consulta = armarConsulta(request, negocioId, bloque, campos);
        Pagina<Registro> pagina = buscar(consulta);
        TotalesRegistros totales = totales(consulta, campos, pagina.totalElementos());
        return ListarRegistrosResponse.de(pagina.map(registro -> registro.conFormulas(campos)), totales);
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Bloque obtenerBloque(BloqueId bloqueId, NegocioId negocioId) {
        return bloqueRepository.obtener(bloqueId, negocioId);
    }

    private List<Campo> obtenerCamposDelBloque(Bloque bloque, NegocioId negocioId) {
        return campoRepository.findByBloqueIdAndNegocioId(bloque.getId(), negocioId);
    }

    private static ConsultaRegistros armarConsulta(ListarRegistrosRequest request, NegocioId negocioId,
                                                   Bloque bloque, List<Campo> campos) {
        return ConsultaRegistros.armar(negocioId, bloque.getId(), campos, request.texto(), request.orden(),
                request.direccion(), request.pagina(), request.tamanio());
    }

    private Pagina<Registro> buscar(ConsultaRegistros consulta) {
        return registroRepository.buscar(consulta);
    }

    /** Cantidad y suma de cada campo numérico de los registros que cumplen la consulta, sin importar la página. */
    private TotalesRegistros totales(ConsultaRegistros consulta, List<Campo> campos, long cantidad) {
        List<CampoId> numericos = camposNumericos(campos);
        return TotalesRegistros.armar(cantidad, numericos, sumarNumericos(consulta, numericos));
    }

    private static List<CampoId> camposNumericos(List<Campo> campos) {
        return campos.stream()
                .filter(campo -> campo.getTipo() == TipoCampo.NUMERO)
                .map(Campo::getId)
                .toList();
    }

    private Map<CampoId, BigDecimal> sumarNumericos(ConsultaRegistros consulta, List<CampoId> numericos) {
        return registroRepository.sumarNumericos(consulta, numericos);
    }
}
