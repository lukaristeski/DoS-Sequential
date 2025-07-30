import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) {
        // tcpdump command with -n to disable hostname resolution
        String[] command = {"tcpdump", "-i", "any", "-n", "port", "9090"};

        // Packet counters
        Map<String, Long> packetCount = new HashMap<>();
        Map<String, Long> packetSize = new HashMap<>();
        Map<String, TrafficStats> ipStats = new HashMap<>();

        // Thresholds
        int windowSize = 3;
        double multiplier = 1.0;
        long timeWindow = 5_000;
        long lastChecked = System.currentTimeMillis();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            // Pattern: (timestamp, protocol, source, destination, flags, size)
            Pattern pattern = Pattern.compile("^(\\d{2}:\\d{2}:\\d{2}\\.\\d{6}).*?(\\S+) (\\S+) > (\\S+):.*?Flags \\[(.*?)\\].*?length (\\d+).*?$");

            System.out.println("Filtered tcpdump output:");

            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String timestamp = matcher.group(1);
                    String protocol = matcher.group(2);
                    String sourceWithPort = matcher.group(3);
                    String destination = matcher.group(4);
                    String flags = matcher.group(5);
                    String packetSizeStr = matcher.group(6);

                    long packetSizeValue = Long.parseLong(packetSizeStr);

                    // Extract source IP without port by removing last '.' and after
                    int lastDotIndex = sourceWithPort.lastIndexOf('.');
                    String sourceIp = (lastDotIndex != -1) ? sourceWithPort.substring(0, lastDotIndex) : sourceWithPort;

                    // Update packet count and size for source IP only
                    packetCount.put(sourceIp, packetCount.getOrDefault(sourceIp, 0L) + 1);
                    packetSize.put(sourceIp, packetSize.getOrDefault(sourceIp, 0L) + packetSizeValue);

                    System.out.println("Time: " + timestamp + ", Protocol: " + protocol + ", Source: " + sourceIp + ", Destination: " + destination + ", Flags: " + flags + ", Size: " + packetSizeStr);
                }

                if (System.currentTimeMillis() - lastChecked >= timeWindow) {
                    System.out.println("\n--- Checking for anomalies --- ");

                    int absoluteThreshold = 1000;

                    for (Map.Entry<String, Long> entry : packetCount.entrySet()) {
                        String ip = entry.getKey();
                        long count = entry.getValue();

                        TrafficStats stats = ipStats.computeIfAbsent(ip, k -> new TrafficStats(windowSize));
                        boolean isAnomaly = stats.isAboveUpperBand(count, multiplier);

                        System.out.printf("IP: %s, Count: %d%n", ip, count);
                        System.out.printf("Mean: %.2f, StdDev: %.2f%n", stats.getMean(), stats.getStdDev());

                        // DoS detection alert
                        if (isAnomaly && stats.getMean() != 0) {
                            System.out.println("BOLLINGER ALERT: DoS suspected from " + ip +
                                    " (Packets: " + count + ", Mean: " + stats.getMean() +
                                    ", StdDev: " + stats.getStdDev() + ")");
                        }
                        if (count > absoluteThreshold) {
                            System.out.println("ABSOLUTE ALERT: Possible DoS from: " + ip + "with" + count + "requests");
                        }
                        stats.add(count);
                    }
                    packetCount.clear();
                    packetSize.clear();
                    lastChecked = System.currentTimeMillis();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}