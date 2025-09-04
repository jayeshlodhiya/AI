package com.retailai.service.rag;




import com.retailai.api.dto.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
public class RagApiClient {

    private final RagApiProperties props;
    private final RestTemplate rest;

    public RagApiClient(RagApiProperties props, RestTemplate ragRestTemplate) {
        this.props = props;
        this.rest = ragRestTemplate;
    }

    /* ---------- Public API ---------- */

    public IngestResponse ingestText(IngestTextRequest body) {
        return postJson(url(props.getEndpoints().getText()), body, IngestResponse.class);
    }

    public IngestResponse ingestUrl(IngestUrlRequest body) {
        return postJson(url(props.getEndpoints().getUrl()), body, IngestResponse.class);
    }

    public IngestResponse ingestFile(String sourceId, String title, List<String> tags,
                                     String filename, byte[] bytes) {
        URI u = url(props.getEndpoints().getFile());

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("sourceId", sourceId);
        if (title != null) form.add("title", title);
        if (tags != null) for (String t : tags) form.add("tags", t);
        form.add("file", new NamedByteArrayResource(bytes, filename));

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> req = new HttpEntity<>(form, h);
        try {
            ResponseEntity<IngestResponse> res = rest.postForEntity(u, req, IngestResponse.class);
            return res.getBody();
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException(errorMsg("POST", u, e), e);
        }
    }

    public AnswerResponse ask(QueryRequest body) {
        return postJson(url(props.getEndpoints().getAsk()), body, AnswerResponse.class);
    }

    public Map<?,?> reindex() {
        URI u = url(props.getEndpoints().getReindex());
        try {
            ResponseEntity<Map> res = rest.postForEntity(u, HttpEntity.EMPTY, Map.class);
            return res.getBody();
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException(errorMsg("POST", u, e), e);
        }
    }

    /* ---------- Helpers ---------- */

    private <T> T postJson(URI u, Object body, Class<T> responseType) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Object> req = new HttpEntity<>(body, h);
        try {
            ResponseEntity<T> res = rest.postForEntity(u, req, responseType);
            return res.getBody();
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException(errorMsg("POST", u, e), e);
        }
    }

    private String errorMsg(String method, URI u, HttpStatusCodeException e) {
        String body = e.getResponseBodyAsString();
        return method + " " + u + " -> " + e.getStatusCode().value() + " " + e.getStatusCode().toString()
                + (body != null && !body.isBlank() ? " | " + body : "");
    }

    private URI url(String path) {
        // makes baseUrl + path robust (handles trailing slashes)
        return props.getBaseUrl().resolve(path.startsWith("/") ? path.substring(1) : path);
    }

    /** Ensures filename is sent in multipart; default ByteArrayResource returns null filename. */
    static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;
        public NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray); this.filename = filename;
        }
        @Override public String getFilename() { return filename; }
    }
}
