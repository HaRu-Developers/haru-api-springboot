package com.haru.api.infra.websocket;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class FastApiClient {

    private final String FASTAPI_URL = "http://localhost:8000/stt";

    private final WebClient webClient;

    public FastApiClient() {
        this.webClient = WebClient.builder()
                .baseUrl(FASTAPI_URL)
                .build();
    }

    public Mono<String> sendRawBytesToFastAPI(byte[] audioBytes) {
        return webClient.post()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .bodyValue(audioBytes)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just("{\"error\": \"Failed to send\"}");
                });
    }

//    public String sendRawBytesToFastAPI(byte[] audioBytes) {
//        try (CloseableHttpClient client = HttpClients.createDefault()) {
//            HttpPost post = new HttpPost(FASTAPI_URL);
//            post.setEntity(new ByteArrayEntity(audioBytes, ContentType.APPLICATION_OCTET_STREAM));
//
//            try (CloseableHttpResponse response = client.execute(post)) {
//                return EntityUtils.toString(response.getEntity());
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "{\"error\": \"Failed to send\"}";
//        }
//    }

//    @Override
//    public boolean isVoice(byte[] audioChunk) {
//        OkHttpClient client = new OkHttpClient();
//
//        RequestBody body = RequestBody.create(audioChunk, MediaType.parse("application/octet-stream"));
//
//        Request request = new Request.Builder()
//                .url("http://localhost:8000/vad")  // FastAPI 주소
//                .post(body)
//                .build();
//
//        try (Response response = client.newCall(request).execute()) {
//            if (!response.isSuccessful()) {
//                System.err.println("VAD request failed: " + response);
//                return false;
//            }
//
//            String responseBody = response.body().string();
//            JSONObject json = new JSONObject(responseBody);
//            return json.getBoolean("is_voice");
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
}
