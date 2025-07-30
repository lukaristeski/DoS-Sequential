// Main.java
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) {
        try {
            // Run tcpdump to monitor localhost traffic on port 8080
            ProcessBuilder pb = new ProcessBuilder("tcpdump", "-i", "any", "port", "8080");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            Pattern ipPattern = Pattern.compile("IP (\\d+\\.\\d+\\.\\d+\\.\\d+) > (\\d+\\.\\d+\\.\\d+\\.\\d+)");
            TrafficStats stats = new TrafficStats();
            int windowSize = 10; // configurable
            double multiplier = 2.0;

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("RAW >>> " + line); // 🔍 see what tcpdump is printing

                if (line.contains("IP ")) { // ✅ simplified matching
                    System.out.println("MATCH >>> " + line);

                    Matcher matcher = ipPattern.matcher(line);
                    if (matcher.find()) {
                        String sourceIp = matcher.group(1);
                        String destIp = matcher.group(2);
                        System.out.printf("TIME >>> %d, SRC >>> %s, DST >>> %s%n", System.currentTimeMillis(), sourceIp, destIp);

                        int count = stats.incrementAndGet(sourceIp, windowSize);
                        boolean isAnomaly = stats.isAboveUpperBand(count, multiplier);

                        System.out.printf("STATS >>> IP: %s, Count: %d, Mean: %.2f, StdDev: %.2f%n",
                                sourceIp, count, stats.getMean(), stats.getStdDev());

                        if (isAnomaly) {
                            System.out.printf("!!! Possible DoS attack detected from %s with %d requests%n", sourceIp, count);
                        }
                    } else {
                        System.out.println("REGEX >>> No match found, possible format mismatch");
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
