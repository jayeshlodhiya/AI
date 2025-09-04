package com.retailai.config;



import com.retailai.service.rag.RagApiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(RagApiProperties.class)
public class RestClientConfig {

    @Bean
    RestTemplate ragRestTemplate(RagApiProperties props) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout((int) props.getConnectTimeout().toMillis());
        f.setReadTimeout((int) props.getReadTimeout().toMillis());
        return new RestTemplate(f);
    }
}
