package com.ensambla.backend.domain.useCase.registro.listarRegistros;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.TotalesRegistros;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ListarRegistros {

    private final NegocioRepository negocioRepository;
    private final BloqueRepository bloqueRepository;
    private final CampoRepository campoRepository;
    private final RegistroRepository registroRepository;

    public ListarRegistrosResponse execute(ListarRegistrosRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        Bloque bloque = bloqueRepository.obtener(request.bloqueId(), negocioId);
        List<Campo> campos = campoRepository.findByBloqueIdAndNegocioId(bloque.getId(), negocioId);
        ConsultaRegistros consulta = ConsultaRegistros.armar(
                negocioId,
                bloque.getId(),
                campos,
                request.texto(),
                request.orden(),
                request.direccion(),
                request.pagina(),
                request.tamanio());
        Pagina<Registro> pagina = registroRepository.buscar(consulta);
        TotalesRegistros totales = totales(consulta, campos, pagina.totalElementos());
        return ListarRegistrosResponse.de(pagina.map(registro -> registro.conFormulas(campos)), totales);
    }

    /** Cantidad y suma de cada campo numérico de los registros que cumplen la consulta, sin importar la página. */
    private TotalesRegistros totales(ConsultaRegistros consulta, List<Campo> campos, long cantidad) {
        List<CampoId> numericos = campos.stream()
                .filter(campo -> campo.getTipo() == TipoCampo.NUMERO)
                .map(Campo::getId)
                .toList();
        return TotalesRegistros.armar(cantidad, numericos, registroRepository.sumarNumericos(consulta, numericos));
    }
}
