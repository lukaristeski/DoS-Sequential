import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrafficGenerator {
    private static final int NUM_THREADS = 50;
    private static final int REQUESTS_PER_THREAD = 100;

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        String targetUrl = "http://localhost:8080";

        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(() -> {
                for (int j = 0; j < REQUESTS_PER_THREAD; j++) {
                    try {
                        HttpURLConnection con = (HttpURLConnection) new URL(targetUrl).openConnection();
                        con.setRequestMethod("GET");
                        int response = con.getResponseCode();
                        System.out.printf("Thread %d → Response Code: %d%n", Thread.currentThread().getId(), response);
                        con.disconnect();
                    } catch (IOException e) {
                        System.err.println("Request failed: " + e.getMessage());
                    }
                }
            });
        }

        executor.shutdown();
    }
}
