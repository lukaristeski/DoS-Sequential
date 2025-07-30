import java.util.HashMap;
import java.util.Map;

public class TrafficStats {
    private final long windowDurationMs;
    private final Map<String, Integer> ipCounts = new HashMap<>();
    private long windowStartTime;

    private final int THRESHOLD = 20; // threshold for DoS alert

    public TrafficStats(long windowDurationMs) {
        this.windowDurationMs = windowDurationMs;
        this.windowStartTime = System.currentTimeMillis();
    }

    public void record(String ip) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - windowStartTime >= windowDurationMs) {
            System.out.println("--- Window Summary ---");
            for (Map.Entry<String, Integer> entry : ipCounts.entrySet()) {
                if (entry.getValue() >= THRESHOLD) {
                    System.out.printf("Possible DoS attack from %s: %d requests%n", entry.getKey(), entry.getValue());
                } else {
                    System.out.printf("%s: %d requests (OK)%n", entry.getKey(), entry.getValue());
                }
            }
            System.out.println("----------------------");

            ipCounts.clear();
            windowStartTime = currentTime;
        }

        ipCounts.put(ip, ipCounts.getOrDefault(ip, 0) + 1);
    }
}
