package plus.crates.Utils;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import plus.crates.CratesPlus;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class LinfootUpdater {
    private final CratesPlus cratesPlus;
    private final String branch;
    private LinfootUpdater.UpdateResult result = LinfootUpdater.UpdateResult.FAILED;
    private String version;

    public enum UpdateResult {
        NO_UPDATE,
        FAILED,
        SNAPSHOT_UPDATE_AVAILABLE,
        UPDATE_AVAILABLE
    }

    public LinfootUpdater(CratesPlus cratesPlus, String branch) {
        if (branch.equalsIgnoreCase("spigot"))
            branch = "release";
        this.cratesPlus = cratesPlus;
        this.branch = branch;
        doCheck();
    }

    private void doCheck() {
        String data = null;
        String url = "http://api.connorlinfoot.com/v2/resource/" + branch + "/cratesplus/";
        try {
            data = doCurl(url);
        } catch (IOException e) {
            cratesPlus.getLogger().warning("Failed to check for updates: " + e.getMessage());
            result = UpdateResult.FAILED;
            return;
        }
        
        // Check if we got a valid response
        if (data == null || data.trim().isEmpty()) {
            cratesPlus.getLogger().warning("Update check returned empty response");
            result = UpdateResult.FAILED;
            return;
        }
        
        JSONParser jsonParser = new JSONParser();
        try {
            JSONObject obj = (JSONObject) jsonParser.parse(data);
            if (obj.get("version") != null) {
                String newestVersion = obj.get("version") + "." + obj.get("snapshot");
                String currentVersion = cratesPlus.getDescription().getVersion().replaceAll("-SNAPSHOT-", "."); // Changes 4.0.0-SNAPSHOT-4 to 4.0.0.4
                if (Integer.parseInt(newestVersion.replace(".", "")) > Integer.parseInt(currentVersion.replace(".", ""))) {
                    if (branch.equalsIgnoreCase("snapshot")) {
                        result = UpdateResult.UPDATE_AVAILABLE;
                        version = obj.get("version").toString();
                    } else {
                        result = UpdateResult.SNAPSHOT_UPDATE_AVAILABLE;
                        version = obj.get("version") + "-SNAPSHOT-" + obj.get("snapshot");
                    }
                } else {
                    result = UpdateResult.NO_UPDATE;
                }
            }
        } catch (ParseException e) {
            cratesPlus.getLogger().warning("Failed to parse update response: " + e.getMessage());
            cratesPlus.getLogger().warning("Response data: " + data);
            result = UpdateResult.FAILED;
        }
    }

    public UpdateResult getResult() {
        return result;
    }

    public String getVersion() {
        return version;
    }

    public String doCurl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setInstanceFollowRedirects(true);
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setConnectTimeout(5000); // 5 second timeout
        con.setReadTimeout(5000); // 5 second timeout
        
        DataOutputStream output = new DataOutputStream(con.getOutputStream());
        output.close();
        
        // Check response code
        int responseCode = con.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP error code: " + responseCode);
        }
        
        DataInputStream input = new DataInputStream(con.getInputStream());
        int c;
        StringBuilder resultBuf = new StringBuilder();
        while ((c = input.read()) != -1) {
            resultBuf.append((char) c);
        }
        input.close();
        return resultBuf.toString();
    }

}