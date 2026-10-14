package br.com.cinepass.reserva.infrastructure.mensageria;

public final class TiposEventoReserva {
    public static final String RESERVA_CRIADA = "ReservaCriada";
    public static final String PAGAMENTO_APROVADO = "PagamentoAprovado";
    public static final String RESERVA_CONFIRMADA = "ReservaConfirmada";
    public static final String RESERVA_CANCELADA = "ReservaCancelada";

    private TiposEventoReserva() {}
}
