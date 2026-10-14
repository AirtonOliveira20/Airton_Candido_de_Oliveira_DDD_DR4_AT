package br.com.cinepass.reserva.observability;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.AsyncAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.logging.*;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import static org.junit.jupiter.api.Assertions.*;

class GelfLoggingTest {
    @TempDir Path directory;

    @Test
    void sendsMdcAndStructuredFieldsOverUdpAndKeepsLocalFile() throws Exception {
        try (var receiver = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
            receiver.setSoTimeout(5000);
            var env = new MockEnvironment()
                .withProperty("spring.application.name", "reserva-service")
                .withProperty("graylog.host", "127.0.0.1")
                .withProperty("graylog.port", Integer.toString(receiver.getLocalPort()))
                .withProperty("logging.file.name", directory.resolve("service.json").toString())
                .withProperty("logging.structured.format.file", "ecs")
                .withProperty("logging.structured.format.console", "ecs");
            var logging = LoggingSystem.get(getClass().getClassLoader());
            var file = LogFile.get(env);
            logging.getSystemProperties(env).apply(file);
            logging.beforeInitialize();
            logging.initialize(new LoggingInitializationContext(env), "classpath:logback-spring.xml", file);
            var context = (LoggerContext) LoggerFactory.getILoggerFactory();
            try {
                var async = (AsyncAppender) context.getLogger("ROOT").getAppender("ASYNC_GELF");
                assertTrue(async.isStarted());
                assertTrue(async.isNeverBlock());
                MDC.put("correlationId", "udp-test");
                MDC.put("traceId", "0123456789abcdef");
                MDC.put("spanId", "1234567890abcdef");
                LoggerFactory.getLogger("gelf.test").atInfo()
                    .addKeyValue("eventId", "evento-udp")
                    .addKeyValue("reservaId", "reserva-udp")
                    .setCause(new IllegalStateException("SECRET_NOT_EXPORTED"))
                    .log("operacao=UDP_TEST resultado=SUCESSO");
                MDC.clear();
                var chunks = new TreeMap<Integer, byte[]>();
                byte[] bytes;
                while (true) {
                    var packet = new DatagramPacket(new byte[65535], 65535);
                    receiver.receive(packet);
                    bytes = Arrays.copyOf(packet.getData(), packet.getLength());
                    if (bytes[0] != 0x1e || bytes[1] != 0x0f) break;
                    chunks.put(Byte.toUnsignedInt(bytes[10]), Arrays.copyOfRange(bytes, 12, bytes.length));
                    if (chunks.size() == Byte.toUnsignedInt(bytes[11])) {
                        var joined = new ByteArrayOutputStream();
                        for (var chunk : chunks.values()) joined.write(chunk);
                        bytes = joined.toByteArray();
                        break;
                    }
                }
                try (var gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
                    String message = new String(gzip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    var json = JsonMapper.builder().build().readTree(message);
                    assertEquals("reserva-service", json.get("_service").asText());
                    assertEquals("udp-test", json.get("_correlationId").asText());
                    assertEquals("evento-udp", json.get("_eventId").asText());
                    assertEquals("reserva-udp", json.get("_reservaId").asText());
                    assertEquals("0123456789abcdef", json.get("_traceId").asText());
                    assertEquals("1234567890abcdef", json.get("_spanId").asText());
                    assertEquals("INFO", json.get("_logLevel").asText());
                    assertFalse(message.contains("SECRET_NOT_EXPORTED"));
                }
                var local = JsonMapper.builder().build().readTree(Files.readAllLines(directory.resolve("service.json")).getFirst());
                assertEquals("reserva-service", local.get("service").get("name").asText());
                assertEquals("udp-test", local.get("correlationId").asText());
                assertEquals("evento-udp", local.get("eventId").asText());
            } finally {
                MDC.clear();
                logging.cleanUp();
                context.stop();
                for (String key : List.of("LOG_FILE", "CONSOLE_LOG_STRUCTURED_FORMAT", "FILE_LOG_STRUCTURED_FORMAT"))
                    System.clearProperty(key);
            }
        }
    }
}

