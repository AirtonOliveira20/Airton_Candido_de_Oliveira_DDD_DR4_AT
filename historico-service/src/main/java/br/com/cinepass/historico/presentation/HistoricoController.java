package br.com.cinepass.historico.presentation;

import br.com.cinepass.historico.application.HistoricoNaoEncontradoException;
import br.com.cinepass.historico.application.HistoricoService;
import br.com.cinepass.historico.application.HistoricoView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/historico")
public class HistoricoController {

    private final HistoricoService service;

    public HistoricoController(HistoricoService service) {
        this.service = service;
    }

    @GetMapping("/clientes/{clienteId}")
    public HistoricoView consultar(@PathVariable UUID clienteId) {
        return service.consultar(clienteId);
    }

    @ExceptionHandler(HistoricoNaoEncontradoException.class)
    ProblemDetail naoEncontrado(HistoricoNaoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
