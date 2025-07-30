// TrafficStats.java
import java.util.*;

public class TrafficStats {
    private final Map<String, LinkedList<Long>> trafficMap = new HashMap<>();
    private final List<Integer> recentCounts = new ArrayList<>();
    private final int maxWindowSize = 10;

    public int incrementAndGet(String ip, int windowSize) {
        long now = System.currentTimeMillis();
        trafficMap.putIfAbsent(ip, new LinkedList<>());
        LinkedList<Long> timestamps = trafficMap.get(ip);
        timestamps.add(now);

        // Remove old timestamps beyond the time window
        while (!timestamps.isEmpty() && now - timestamps.peekFirst() > windowSize * 1000) {
            timestamps.pollFirst();
        }

        int count = timestamps.size();

        // Update stats
        recentCounts.add(count);
        if (recentCounts.size() > maxWindowSize) {
            recentCounts.remove(0);
        }

        return count;
    }

    public double getMean() {
        if (recentCounts.isEmpty()) return 0.0;
        double sum = 0.0;
        for (int c : recentCounts) {
            sum += c;
        }
        return sum / recentCounts.size();
    }

    public double getStdDev() {
        double mean = getMean();
        if (recentCounts.isEmpty()) return 0.0;
        double sumSquaredDiffs = 0.0;
        for (int c : recentCounts) {
            sumSquaredDiffs += Math.pow(c - mean, 2);
        }
        return Math.sqrt(sumSquaredDiffs / recentCounts.size());
    }

    public boolean isAboveUpperBand(int count, double multiplier) {
        double mean = getMean();
        double stdDev = getStdDev();
        return count > (mean + multiplier * stdDev);
    }
}
