import java.util.LinkedList;

public class TrafficStats {
    private final LinkedList<Long> history = new LinkedList<>();
    private final int maxWindow;

    public TrafficStats(int maxWindow) {
        this.maxWindow = maxWindow;
    }

    public void add(long value) {
        if (history.size() >= maxWindow) {
            history.removeFirst();
        }
        history.add(value);
    }

    public double getMean() {
        return history.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    public double getStdDev() {
        double mean = getMean();
        return Math.sqrt(history.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0));
    }

    public boolean isAboveUpperBand(long current, double multiplier) {
        double mean = getMean();
        double std = getStdDev();
        return current > mean + multiplier * std;
    }
}
