package br.com.cinepass.notificacao.mensageria;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import br.com.cinepass.notificacao.observability.TraceSupport;
import java.nio.charset.StandardCharsets;
@Component
public class ReservaEventosListener {
    private final ReservaInbox inbox;
    private final TraceSupport traces;
    public ReservaEventosListener(ReservaInbox inbox,TraceSupport traces){this.inbox=inbox;this.traces=traces;}
    @KafkaListener(topics="${cinepass.kafka.topico-reservas}")
    public void onEvento(ConsumerRecord<String,String> record){
        var carrier=traces.capture();
        var header=record.headers().lastHeader("correlationId");
        if(header!=null && header.value()!=null) carrier.put("correlationId",TraceSupport.safe(new String(header.value(),StandardCharsets.UTF_8)));
        var eventHeader=record.headers().lastHeader("eventId");
        if(eventHeader!=null && eventHeader.value()!=null) carrier.put("eventId",TraceSupport.safe(new String(eventHeader.value(),StandardCharsets.UTF_8)));
        carrier.put("reservaId",TraceSupport.safe(record.key()));
        traces.within(carrier,"inbox.receive",()->{inbox.receive(record);return null;});
    }
}
