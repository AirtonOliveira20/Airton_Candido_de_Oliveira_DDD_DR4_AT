package br.com.cinepass.auditoria.presentation;
import br.com.cinepass.auditoria.mensageria.ReservaInbox;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
@RestController
@RequestMapping("/internal/eventos")
public class InboxOperationsController {
    private final ReservaInbox inbox;
    private final String token;
    public InboxOperationsController(ReservaInbox inbox,@Value("${cinepass.operations-token:}") String token){this.inbox=inbox;this.token=token;}
    private void authorize(String supplied){
        if(token.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Configure OPERATIONS_TOKEN");
        if(supplied==null || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    @GetMapping("/falhas") public Object failures(@RequestHeader(value="X-Operations-Token",required=false) String token){authorize(token);return inbox.failures();}
    @GetMapping("/{id}") public Object detail(@PathVariable long id,@RequestHeader(value="X-Operations-Token",required=false) String token){authorize(token);return inbox.detail(id);}
    @PostMapping("/{id}/reprocessar") public Object retry(@PathVariable long id,@RequestHeader(value="X-Operations-Token",required=false) String token){
        authorize(token);if(!inbox.retry(id)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Evento não está DEAD");
        return Map.of("id",id,"status","PENDING");
    }
}
