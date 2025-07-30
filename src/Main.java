import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting DoS Detection System (Sequential)");

        try {
            ProcessBuilder pb = new ProcessBuilder("tcpdump", "-i", "enp0s3", "-l", "-n", "tcp", "port", "8080");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;


            Pattern pattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)");
            TrafficStats stats = new TrafficStats(5000); // 5-second window

            while ((line = reader.readLine()) != null) {
                System.out.println("RAW >>> " + line);

                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String ip = matcher.group(1);
                    System.out.println("MATCH >>> IP: " + ip);
                    stats.record(ip);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
