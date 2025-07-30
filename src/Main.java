import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) {
        // tcpdump
        String[] command = {"tcpdump", "-i", "any", "port", "8080"};

        // Packet counters
        Map<String, Long> packetCount = new HashMap<>();
        Map<String, Long> packetSize = new HashMap<>();
        Map<String, TrafficStats> ipStats = new HashMap<>();


        // Thresholds

        int windowSize = 5;
        double multiplier = 2.0;
        long timeWindow = 5_000;
        long lastChecked = System.currentTimeMillis();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            // (timestamp, protocol, source, destination, flags, size)
            Pattern pattern = Pattern.compile("^(\\d{2}:\\d{2}:\\d{2}\\.\\d{6}).*?(\\S+) (\\S+) > (\\S+):.*?Flags \\[(.*?)\\].*?length (\\d+).*?$");

            System.out.println("Filtered tcpdump output:");
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String timestamp = matcher.group(1);
                    String protocol = matcher.group(2);
                    String source = matcher.group(3);
                    String destination = matcher.group(4);
                    String flags = matcher.group(5);
                    String packetSizeStr = matcher.group(6);

                    long packetSizeValue = Long.parseLong(packetSizeStr);

                    // Update packet count and size
                    packetCount.put(source, packetCount.getOrDefault(source, 0L) + 1);
                    packetSize.put(source, packetSize.getOrDefault(source, 0L) + packetSizeValue);

                    System.out.println("Time: " + timestamp + ", Protocol: " + protocol + ", Source: " + source + ", Destination: " + destination + ", Flags: " + flags + ", Size: " + packetSizeStr);
                }
                if (System.currentTimeMillis() - lastChecked >= timeWindow) {
                    System.out.println("\n--- Checking for anomalies ---");
                    for (Map.Entry<String, Long> entry : packetCount.entrySet()) {
                        String ip = entry.getKey();
                        long count = entry.getValue();

                        TrafficStats stats = ipStats.computeIfAbsent(ip, k -> new TrafficStats(windowSize));
                        boolean isAnomaly = stats.isAboveUpperBand(count, multiplier);

                        // DoS detection
                        if (isAnomaly && stats.getMean() != 0) {
                            System.out.println("BOOLLINGER ALERT: DoS suspected from " + ip +
                                    " (Packets: " + count + ", Mean: " + stats.getMean() +
                                    ", StdDev: " + stats.getStdDev() + ")");
                        }
                        stats.add(count);
                    }
                    packetCount.clear();
                    packetSize.clear();
                    lastChecked = System.currentTimeMillis();

                }
            }
        } catch(IOException e){
            throw new RuntimeException(e);
        }
    }
}