package cn.edu.zjnu.acm.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class RESTService {

    public String postJson(String json, String path) {
        String result = null;
        HttpURLConnection connection = null;
        try {
            log.debug("Posting JSON to: {}", path);
            URL url = new URL(path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            byte[] writebytes = json.getBytes();
            connection.setRequestProperty("Content-Length", String.valueOf(writebytes.length));
            connection.connect();
            
            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
            out.append(json);
            out.flush();
            out.close();
            
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                result = response.toString();
                log.debug("Response from {}: {}", path, result);
            } else {
                log.error("HTTP error response from {}: {}", path, responseCode);
                // Try to read error stream
                try {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
            }
                    errorReader.close();
                    log.error("Error response body: {}", errorResponse.toString());
                } catch (Exception e) {
                    log.error("Failed to read error stream", e);
                }
                return null;
            }
        } catch (ConnectException e) {
            log.error("Connection refused to judge service: {}", path, e);
            return null;
        } catch (SocketTimeoutException e) {
            log.error("Timeout connecting to judge service: {}", path, e);
            return null;
        } catch (java.net.UnknownHostException e) {
            log.error("Unknown host for judge service: {}", path, e);
            return null;
        } catch (Exception e) {
            log.error("Error posting JSON to judge service: {}", path, e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return result;
    }

}
