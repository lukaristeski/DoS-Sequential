import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrafficGenerator {
    private static final int NUM_THREADS = 20;
    private static final int REQUESTS_PER_THREAD = 10;

    public static void main(String[] args) throws IOException {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        String urlString = "http://localhost:8080";

        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(() -> {
                for (int j = 0; j < REQUESTS_PER_THREAD; j++) {
                    try {
                        URL url = new URL(urlString);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("GET");
                        int responseCode = connection.getResponseCode();
                        System.out.println("Thread: " + Thread.currentThread().getId() +
                                " - Response Code: " + responseCode);
                        connection.disconnect();
                    } catch (IOException e) {
                        System.err.println("Request failed: " + e.getMessage());
                    }
                }
            });
        }
        executor.shutdown();
    }
}