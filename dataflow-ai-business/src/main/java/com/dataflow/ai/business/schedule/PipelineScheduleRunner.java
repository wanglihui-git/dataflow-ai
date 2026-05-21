package com.dataflow.ai.business.schedule;

import com.dataflow.ai.business.repository.PipelineRepository;
import com.dataflow.ai.business.service.PipelineService;
import com.dataflow.ai.domain.entity.Pipeline;
import com.dataflow.ai.domain.vo.ScheduleConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pipeline 定时调度：CRON / FIXED_RATE / FIXED_DELAY
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PipelineScheduleRunner {

    private final PipelineRepository pipelineRepository;
    private final PipelineService pipelineService;

    private final Map<String, LocalDateTime> lastCronRun = new ConcurrentHashMap<>();
    private final Map<String, Long> lastFixedRunMs = new ConcurrentHashMap<>();

    @Scheduled(fixedRateString = "${app.schedule.poll-interval-ms:60000}")
    public void pollScheduledPipelines() {
        for (Pipeline pipeline : pipelineRepository.findByStatus("active")) {
            ScheduleConfig schedule = pipeline.getSchedule();
            if (schedule == null || schedule.getScheduleType() == null
                    || schedule.getScheduleType() == ScheduleConfig.ScheduleType.MANUAL) {
                continue;
            }
            if (Boolean.FALSE.equals(schedule.getEnabled())) {
                continue;
            }
            if (!shouldRun(pipeline.getId(), schedule)) {
                continue;
            }
            try {
                log.info("Scheduled execution triggered: pipelineId={}, type={}",
                        pipeline.getId(), schedule.getScheduleType());
                pipelineService.executePipeline(pipeline.getId(), "scheduler");
                markRun(pipeline.getId(), schedule);
            } catch (Exception e) {
                log.error("Scheduled execution failed: pipelineId={}", pipeline.getId(), e);
            }
        }
    }

    private boolean shouldRun(String pipelineId, ScheduleConfig schedule) {
        return switch (schedule.getScheduleType()) {
            case CRON -> shouldRunCron(pipelineId, schedule.getCronExpression());
            case FIXED_RATE -> shouldRunFixed(pipelineId, schedule.getInterval(), true);
            case FIXED_DELAY -> shouldRunFixed(pipelineId, schedule.getInterval(), false);
            default -> false;
        };
    }

    private boolean shouldRunCron(String pipelineId, String cronExpression) {
        if (cronExpression == null || cronExpression.isBlank()) {
            return false;
        }
        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime last = lastCronRun.get(pipelineId);
            LocalDateTime next = cron.next(last != null ? last : now.minusMinutes(1));
            if (next != null && !next.isAfter(now)) {
                return true;
            }
        } catch (Exception e) {
            log.warn("Invalid cron for pipeline {}: {}", pipelineId, cronExpression);
        }
        return false;
    }

    private boolean shouldRunFixed(String pipelineId, Long intervalSec, boolean fixedRate) {
        if (intervalSec == null || intervalSec <= 0) {
            return false;
        }
        long intervalMs = intervalSec * 1000;
        long now = System.currentTimeMillis();
        Long last = lastFixedRunMs.get(pipelineId);
        if (last == null) {
            return true;
        }
        long elapsed = now - last;
        return fixedRate ? elapsed >= intervalMs : elapsed >= intervalMs;
    }

    private void markRun(String pipelineId, ScheduleConfig schedule) {
        if (schedule.getScheduleType() == ScheduleConfig.ScheduleType.CRON) {
            lastCronRun.put(pipelineId, LocalDateTime.now(ZoneId.systemDefault()));
        } else {
            lastFixedRunMs.put(pipelineId, System.currentTimeMillis());
        }
    }
}
