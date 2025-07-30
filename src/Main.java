import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) {
        String[] command = {"tcpdump", "-i", "any", "port", "8080"};

        Map<String, Long> packetCount = new HashMap<>();
        Map<String, Long> packetSize = new HashMap<>();
        Map<String, TrafficStats> ipStats = new HashMap<>();

        int windowSize = 5;
        double multiplier = 2.0;
        long timeWindow = 5_000;
        long lastChecked = System.currentTimeMillis();

        Pattern pattern = Pattern.compile(
                "^(\\d{2}:\\d{2}:\\d{2}\\.\\d{6}).*?(\\S+) (\\S+) > (\\S+):.*?Flags \\[(.*?)\\].*?length (\\d+).*?$"
        );

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            System.out.println("Starting DoS detection...");
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String timestamp = matcher.group(1);
                    String protocol = matcher.group(2);
                    String source = matcher.group(3);
                    String destination = matcher.group(4);
                    String flags = matcher.group(5);
                    long size = Long.parseLong(matcher.group(6));

                    packetCount.put(source, packetCount.getOrDefault(source, 0L) + 1);
                    packetSize.put(source, packetSize.getOrDefault(source, 0L) + size);

                    System.out.printf("Time: %s, Src: %s, Dst: %s, Flags: %s, Size: %d%n",
                            timestamp, source, destination, flags, size);
                }

                if (System.currentTimeMillis() - lastChecked >= timeWindow) {
                    System.out.println("\n--- Checking for anomalies ---");
                    for (Map.Entry<String, Long> entry : packetCount.entrySet()) {
                        String ip = entry.getKey();
                        long count = entry.getValue();

                        TrafficStats stats = ipStats.computeIfAbsent(ip, k -> new TrafficStats(windowSize));
                        if (stats.isAboveUpperBand(count, multiplier) && stats.getMean() != 0) {
                            System.out.printf("DoS ALERT from %s (Packets: %d, Mean: %.2f, StdDev: %.2f)%n",
                                    ip, count, stats.getMean(), stats.getStdDev());
                        }
                        stats.add(count);
                    }
                    packetCount.clear();
                    packetSize.clear();
                    lastChecked = System.currentTimeMillis();
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to execute tcpdump: " + e.getMessage());
        }
    }
}
