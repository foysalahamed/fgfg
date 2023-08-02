

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class WebClientConfig {
    private static String baseUrl;
    private static WebClient webClient;
    WebClientConfig(@Value("${webClient.baseUrl}") String value) {
        baseUrl = value;
    }

    public static WebClient getWebClient(Long userId, String token) {
            webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader("UserId", userId.toString())
                    .defaultHeader("Authorization", token)
                    .build();
            return webClient;
    }
}
