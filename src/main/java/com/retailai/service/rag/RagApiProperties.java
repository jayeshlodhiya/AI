package com.retailai.service.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "rag.api")
public class RagApiProperties {
    private URI baseUrl = URI.create("https://ffbe2654905c.ngrok-free.app:8089");
    private Endpoints endpoints = new Endpoints();
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(10);

    public static class Endpoints {
        private String text = "/v1/documents/text";
        private String file = "/v1/documents/file";
        private String url  = "/v1/documents/url";
        private String ask  = "/v1/ask";
        private String reindex = "/v1/admin/reindex";
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getFile() { return file; }
        public void setFile(String file) { this.file = file; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getAsk() { return ask; }
        public void setAsk(String ask) { this.ask = ask; }
        public String getReindex() { return reindex; }
        public void setReindex(String reindex) { this.reindex = reindex; }
    }

    public URI getBaseUrl() { return baseUrl; }
    public void setBaseUrl(URI baseUrl) { this.baseUrl = baseUrl; }
    public Endpoints getEndpoints() { return endpoints; }
    public void setEndpoints(Endpoints endpoints) { this.endpoints = endpoints; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
}
