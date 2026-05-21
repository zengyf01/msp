package com.msp.kuscia.client;

import com.msp.common.core.TaskRequest;
import com.msp.common.core.TaskStatus;
import com.msp.kuscia.dto.KusciaTaskSpec;
import com.msp.kuscia.dto.KusciaTaskStatusResponse;
import com.msp.kuscia.dto.KusciaTaskResultResponse;
import com.msp.kuscia.mapper.KusciaStatusMapper;
import com.msp.kuscia.service.TaskSpecGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Kuscia API客户端实现
 * 完整实现：通过WebClient调用Kuscia Master REST API
 */
@Component
public class KusciaClientImpl implements KusciaClient {

    private static final Logger log = LoggerFactory.getLogger(KusciaClientImpl.class);

    private final WebClient kusciaWebClient;
    private final TaskSpecGenerator taskSpecGenerator;

    @Value("${kuscia.submit.timeout-seconds:30}")
    private int submitTimeoutSeconds;

    @Value("${kuscia.status.timeout-seconds:10}")
    private int statusTimeoutSeconds;

    // 本地状态缓存（用于快速返回，不依赖Kuscia）
    private final ConcurrentMap<String, TaskStatus> localStatusCache = new ConcurrentHashMap<>();

    public KusciaClientImpl(WebClient kusciaWebClient, TaskSpecGenerator taskSpecGenerator) {
        this.kusciaWebClient = kusciaWebClient;
        this.taskSpecGenerator = taskSpecGenerator;
    }

    @Override
    public boolean submitTask(String taskId, TaskRequest request) {
        log.info("Submitting task {} to Kuscia", taskId);

        try {
            KusciaTaskSpec spec = taskSpecGenerator.generate(taskId, request);

            String response = kusciaWebClient.post()
                .uri("/api/v1/tasks")
                .bodyValue(spec)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(submitTimeoutSeconds))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                    .filter(this::isRetryableException))
                .block();

            log.info("Task {} submitted successfully, response: {}", taskId, response);
            localStatusCache.put(taskId, TaskStatus.PENDING);
            return true;

        } catch (WebClientResponseException e) {
            log.error("Kuscia API error submitting task {}: {} - {}",
                taskId, e.getStatusCode(), e.getResponseBodyAsString());
            throw KusciaApiException.fromWebClientException(e);
        } catch (Exception e) {
            log.error("Unexpected error submitting task {}", taskId, e);
            // 降级：标记为PENDING，让状态轮询服务处理
            localStatusCache.put(taskId, TaskStatus.PENDING);
            return true;
        }
    }

    @Override
    public TaskStatus getTaskStatus(String taskId) {
        log.debug("Querying status for task {} from Kuscia", taskId);

        try {
            KusciaTaskStatusResponse response = kusciaWebClient.get()
                .uri("/api/v1/tasks/{taskId}", taskId)
                .retrieve()
                .bodyToMono(KusciaTaskStatusResponse.class)
                .timeout(Duration.ofSeconds(statusTimeoutSeconds))
                .block();

            if (response == null) {
                log.warn("Empty response for task {}", taskId);
                return localStatusCache.getOrDefault(taskId, TaskStatus.PENDING);
            }

            TaskStatus status = KusciaStatusMapper.map(response.getPhase());
            localStatusCache.put(taskId, status);

            log.debug("Task {} status: {} -> {}", taskId, response.getPhase(), status);
            return status;

        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Task {} not found in Kuscia", taskId);
                return localStatusCache.getOrDefault(taskId, TaskStatus.PENDING);
            }
            log.error("Kuscia API error querying task {}: {}", taskId, e.getMessage());
            // 降级：返回本地缓存状态
            return localStatusCache.getOrDefault(taskId, TaskStatus.PENDING);
        } catch (Exception e) {
            log.error("Unexpected error querying task {}: {}", taskId, e.getMessage());
            // 降级：返回本地缓存状态
            return localStatusCache.getOrDefault(taskId, TaskStatus.PENDING);
        }
    }

    @Override
    public boolean cancelTask(String taskId) {
        log.info("Cancelling task {} in Kuscia", taskId);

        try {
            kusciaWebClient.delete()
                .uri("/api/v1/tasks/{taskId}", taskId)
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(Duration.ofSeconds(statusTimeoutSeconds))
                .block();

            localStatusCache.put(taskId, TaskStatus.CANCELLED);
            log.info("Task {} cancelled successfully", taskId);
            return true;

        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Task {} not found for cancellation", taskId);
                localStatusCache.put(taskId, TaskStatus.CANCELLED);
                return true;
            }
            log.error("Kuscia API error cancelling task {}: {}", taskId, e.getMessage());
            throw KusciaApiException.fromWebClientException(e);
        } catch (Exception e) {
            log.error("Unexpected error cancelling task {}", taskId, e);
            // 降级：标记为已取消
            localStatusCache.put(taskId, TaskStatus.CANCELLED);
            return true;
        }
    }

    @Override
    public byte[] getTaskResult(String taskId) {
        log.info("Retrieving result for task {} from Kuscia", taskId);

        try {
            KusciaTaskResultResponse response = kusciaWebClient.get()
                .uri("/api/v1/tasks/{taskId}/result", taskId)
                .retrieve()
                .bodyToMono(KusciaTaskResultResponse.class)
                .timeout(Duration.ofSeconds(statusTimeoutSeconds))
                .block();

            if (response == null || response.getData() == null) {
                log.warn("No result data for task {}", taskId);
                return new byte[0];
            }

            log.info("Task {} result received, size: {} bytes", taskId, response.getData().length);
            return response.getData();

        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Result not available for task {}", taskId);
                return new byte[0];
            }
            log.error("Kuscia API error getting task result {}: {}", taskId, e.getMessage());
            throw KusciaApiException.fromWebClientException(e);
        } catch (Exception e) {
            log.error("Unexpected error getting task result {}: {}", taskId, e);
            return new byte[0];
        }
    }

    /**
     * 获取本地缓存状态（不调用Kuscia）
     */
    public TaskStatus getCachedStatus(String taskId) {
        return localStatusCache.getOrDefault(taskId, TaskStatus.PENDING);
    }

    /**
     * 更新本地状态
     */
    public void updateLocalStatus(String taskId, TaskStatus status) {
        localStatusCache.put(taskId, status);
    }

    private boolean isRetryableException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException e) {
            int status = e.getStatusCode().value();
            return status == 408 || status == 429 || status >= 500;
        }
        return throwable instanceof java.net.ConnectException
            || throwable instanceof java.net.SocketTimeoutException;
    }
}