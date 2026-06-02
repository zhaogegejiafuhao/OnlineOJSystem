package cn.edu.zjnu.acm.service;

import cn.edu.zjnu.acm.config.Config;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class JudgeRunService {
    private final Config config;
    private final RESTService restService;

    public JudgeRunService(Config config, RESTService restService) {
        this.config = config;
        this.restService = restService;
    }

    public RunResult runCode(String source, String input, String language, int maxCpuTime, long maxMemoryBytes) {
        String host = getJudgerHost();
        if (host == null) {
            RunResult result = new RunResult();
            result.setStatus("SE");
            result.setError("No judge service available");
            return result;
        }

        String url = host + "/run";
        JSONObject payload = new JSONObject();
        payload.put("run_id", String.valueOf(System.currentTimeMillis()));
        payload.put("source", source);
        payload.put("input", input != null ? input : "");
        payload.put("language", language);
        payload.put("max_cpu_time", maxCpuTime);
        payload.put("max_memory", maxMemoryBytes);

        try {
            String response = restService.postJson(payload.toJSONString(), url);
            if (response == null) {
                RunResult result = new RunResult();
                result.setStatus("SE");
                result.setError("Judge service returned null");
                return result;
            }
            JSONObject json = JSON.parseObject(response);
            RunResult result = new RunResult();
            result.setStatus(json.getString("status"));
            result.setOutput(json.getString("output"));
            result.setError(json.getString("error"));
            result.setCpuTime(json.getIntValue("cpu_time"));
            result.setMemory(json.getIntValue("memory"));
            return result;
        } catch (Exception e) {
            log.error("Error calling judge /run endpoint", e);
            RunResult result = new RunResult();
            result.setStatus("SE");
            result.setError("Error: " + e.getMessage());
            return result;
        }
    }

    public List<RunResult> runCodeWithMultipleInputs(String source, String language,
                                                      List<String> inputs, int maxCpuTime, long maxMemoryBytes) {
        List<RunResult> results = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            log.info("Running test case {}/{} with language {}", i + 1, inputs.size(), language);
            RunResult result = runCode(source, inputs.get(i), language, maxCpuTime, maxMemoryBytes);
            result.setCaseNum(i + 1);
            results.add(result);
            if (!"AC".equals(result.getStatus())) {
                log.warn("Test case {} failed with status: {}", i + 1, result.getStatus());
            }
        }
        return results;
    }

    private String getJudgerHost() {
        if (config.getJudgerhost() == null || config.getJudgerhost().isEmpty()) {
            return null;
        }
        String host = config.getJudgerhost().get(0);
        if (host.endsWith("/judge")) {
            host = host.substring(0, host.length() - "/judge".length());
        }
        return host;
    }

    @Data
    public static class RunResult {
        private int caseNum;
        private String status;
        private String output;
        private String error;
        private int cpuTime;
        private int memory;
    }
}
