package br.com.cinepass.auditoria.mensageria;
import br.com.cinepass.auditoria.application.AuditoriaService;
import br.com.cinepass.auditoria.observability.TraceSupport;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class ReservaInboxTest {
    JdbcTemplate db;
    ReservaInbox inbox;
    AuditoriaService service;
    String a=UUID.randomUUID().toString(), b=UUID.randomUUID().toString();
    public static boolean advisoryLock(long key){return true;}
    @BeforeEach void setup(){
        var ds=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");
        db=new JdbcTemplate(ds);
        db.execute("CREATE ALIAS PG_TRY_ADVISORY_XACT_LOCK FOR 'br.com.cinepass.auditoria.mensageria.ReservaInboxTest.advisoryLock'");
        db.execute("create table effects(event_id varchar(100) primary key)");
        service=mock(AuditoriaService.class);
        var traces=mock(TraceSupport.class);
        when(traces.within(anyMap(),anyString(),any())).thenAnswer(c->((java.util.function.Supplier<?>)c.getArgument(2)).get());
        when(traces.capture()).thenReturn(Map.of("correlationId","test"));
        var mapper=JsonMapper.builder().build();
        inbox=new ReservaInbox(db,new DataSourceTransactionManager(ds),new EventoReservaParser(mapper),service,traces,mapper,2,0);
        inbox.initialize();
    }
    @AfterEach void close(){inbox.close();}
    long insert(String key,long offset){
        String payload=JsonMapper.builder().build().writeValueAsString(new EventoReserva(
            UUID.randomUUID(), "ReservaCriada", UUID.fromString(key), java.time.Instant.now(), "test", 1, "reserva-service",
            new EventoReserva.Payload(UUID.randomUUID(),UUID.randomUUID(),List.of("A1"),java.math.BigDecimal.TEN,"AGUARDANDO_PAGAMENTO",null,null,null)));

        db.update("insert into reserva_inbox(topic,partition_no,offset_no,aggregate_key,payload,trace_context) values('events',0,?,?,?,'{}')",offset,key,payload);
        return db.queryForObject("select id from reserva_inbox where offset_no=?",Long.class,offset);
    }
    String status(long id){return db.queryForObject("select status from reserva_inbox where id=?",String.class,id);}
    @Test void failureIsolatesReservationAndRecoveryReleasesSuccessor(){
        long first=insert(a,1), second=insert(a,2), other=insert(b,3);
        when(service.registrar(any(),anyString(),anyInt(),anyLong(),anyString(),anyString())).thenAnswer(c->{
            EventoReserva e=c.getArgument(0);
            db.update("insert into effects values(?)",e.eventId().toString());
            if(e.reservaId().toString().equals(a)) throw new IllegalStateException("sensitive-test-value");
            return true;
        });
        inbox.process(first); inbox.process(first);
        assertEquals("DEAD",status(first));
        assertFalse(inbox.eligible().contains(second));
        inbox.process(second); assertEquals("PENDING",status(second));
        inbox.process(other); assertEquals("DONE",status(other));
        assertEquals(1,db.queryForObject("select count(*) from effects",Integer.class));
        assertEquals("IllegalStateException",db.queryForObject("select last_error from reserva_inbox where id=?",String.class,first));
        reset(service);
        when(service.registrar(any(),anyString(),anyInt(),anyLong(),anyString(),anyString())).thenReturn(true);
        assertTrue(inbox.retry(first)); inbox.process(first);
        assertEquals("DONE",status(first)); assertTrue(inbox.eligible().contains(second));
        inbox.process(second); assertEquals("DONE",status(second));
        inbox.process(second);
        verify(service,times(2)).registrar(any(),anyString(),anyInt(),anyLong(),anyString(),anyString());
    }
    @Test void malformedPayloadRemainsAvailableWithoutBlockingOtherKey(){
        long broken=insert(a,1), other=insert(b,2);
        db.update("update reserva_inbox set payload='invalid-json' where id=?",broken);
        inbox.process(broken);inbox.process(broken);inbox.process(other);
        assertEquals("DEAD",status(broken));assertEquals("DONE",status(other));
        assertEquals("invalid-json",inbox.detail(broken).getFirst().get("payload"));
        assertEquals(1,inbox.failures().size());
    }
}
