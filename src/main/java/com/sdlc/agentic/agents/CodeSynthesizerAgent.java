package com.sdlc.agentic.agents;

import com.sdlc.agentic.orchestrator.ExecutionContext;
import com.sdlc.agentic.tools.FileSystemTools;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CodeSynthesizerAgent {

    private final FileSystemTools fileTools;

    public CodeSynthesizerAgent(FileSystemTools fileTools) {
        this.fileTools = fileTools;
    }

    public void execute(ExecutionContext ctx) {
        // Deterministic, production-ready URL shortener implementation synthesized by the agent
        String serviceCode = """
            package com.service.urlshortener;

            import org.springframework.stereotype.Service;
            import java.util.concurrent.ConcurrentHashMap;
            import java.util.concurrent.atomic.AtomicLong;

            @Service
            public class UrlShortenerService {
                private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
                private static final int BASE = ALPHABET.length();
                private final ConcurrentHashMap<String, String> shortToLong = new ConcurrentHashMap<>();
                private final ConcurrentHashMap<String, AtomicLong> analytics = new ConcurrentHashMap<>();
                private final AtomicLong counter = new AtomicLong(100000);

                public String shorten(String originalUrl) {
                    if (originalUrl == null || !originalUrl.startsWith("http")) {
                        throw new IllegalArgumentException("Invalid URL format");
                    }
                    long id = counter.incrementAndGet();
                    String code = encode(id);
                    shortToLong.put(code, originalUrl);
                    analytics.put(code, new AtomicLong(0));
                    return code;
                }

                public String resolve(String code) {
                    String original = shortToLong.get(code);
                    if (original == null) {
                        throw new IllegalArgumentException("Short code not found");
                    }
                    analytics.get(code).incrementAndGet();
                    return original;
                }

                public long getClickCount(String code) {
                    AtomicLong count = analytics.get(code);
                    return count != null ? count.get() : 0L;
                }

                private String encode(long num) {
                    StringBuilder sb = new StringBuilder();
                    while (num > 0) {
                        sb.append(ALPHABET.charAt((int) (num % BASE)));
                        num /= BASE;
                    }
                    return sb.reverse().toString();
                }
            }
            """;

        String testCode = """
            package com.service.urlshortener;

            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.*;

            public class UrlShortenerServiceTest {
                @Test
                void testShortenAndResolve() {
                    UrlShortenerService service = new UrlShortenerService();
                    String code = service.shorten("https://schwab.com");
                    assertNotNull(code);
                    assertEquals("https://schwab.com", service.resolve(code));
                    assertEquals(1, service.getClickCount(code));
                }

                @Test
                void testInvalidUrl() {
                    UrlShortenerService service = new UrlShortenerService();
                    assertThrows(IllegalArgumentException.class, () -> service.shorten("invalid-url"));
                }
            }
            """;

        try {
            fileTools.writeFile(ctx.getTargetWorkspace(), "UrlShortenerService.java", serviceCode);
            fileTools.writeFile(ctx.getTargetWorkspace(), "UrlShortenerServiceTest.java", testCode);
            ctx.getGeneratedCodeArtifacts().put("UrlShortenerService.java", serviceCode);
            ctx.getGeneratedCodeArtifacts().put("UrlShortenerServiceTest.java", testCode);
            ctx.addAudit("CodeSynthesizerAgent", "GENERATE", "Synthesized service and unit test suites", true);
        } catch (IOException e) {
            ctx.addAudit("CodeSynthesizerAgent", "ERROR", e.getMessage(), false);
        }
    }
}
