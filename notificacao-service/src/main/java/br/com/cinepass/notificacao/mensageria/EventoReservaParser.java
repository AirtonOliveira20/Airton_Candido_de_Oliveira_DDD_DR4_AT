package br.com.cinepass.notificacao.mensageria;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class EventoReservaParser {
    private final JsonMapper jsonMapper;

    public EventoReservaParser(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public EventoReserva ler(String json) {
        try {
            EventoReserva evento = jsonMapper.readValue(json, EventoReserva.class);
            evento.validar();
            return evento;
        } catch (JacksonException ex) {
            throw new EventoInvalidoException("JSON inválido");
        }
    }
}
