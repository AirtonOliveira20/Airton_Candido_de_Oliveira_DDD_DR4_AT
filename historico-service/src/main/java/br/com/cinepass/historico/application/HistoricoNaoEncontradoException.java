package br.com.cinepass.historico.application;

import java.util.UUID;

public class HistoricoNaoEncontradoException extends RuntimeException {
    public HistoricoNaoEncontradoException(UUID clienteId) {
        super("Não há histórico para o cliente " + clienteId);
    }
}
