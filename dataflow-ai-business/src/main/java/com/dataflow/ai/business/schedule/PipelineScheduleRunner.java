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
 * Pipeline 定时调度执行器。
 * <p>周期性轮询 {@code active} 状态的 Pipeline，按 CRON / 固定频率 / 固定延迟触发 {@link PipelineService#executePipeline}。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PipelineScheduleRunner {

    private final PipelineRepository pipelineRepository;
    private final PipelineService pipelineService;

    /** CRON 类型 Pipeline 的上次触发时间（用于计算 next）。 */
    private final Map<String, LocalDateTime> lastCronRun = new ConcurrentHashMap<>();
    /** 固定间隔类型 Pipeline 的上次执行时间戳（毫秒）。 */
    private final Map<String, Long> lastFixedRunMs = new ConcurrentHashMap<>();

    /**
     * 定时轮询并触发满足调度条件的 Pipeline 执行。
     * <p>轮询间隔由配置项 {@code app.schedule.poll-interval-ms} 控制，默认 60 秒。</p>
     */
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

    /**
     * 按调度类型判断是否应在本次轮询中触发执行。
     *
     * @param pipelineId Pipeline ID
     * @param schedule   调度配置
     * @return 应执行返回 true
     */
    private boolean shouldRun(String pipelineId, ScheduleConfig schedule) {
        return switch (schedule.getScheduleType()) {
            case CRON -> shouldRunCron(pipelineId, schedule.getCronExpression());
            case FIXED_RATE -> shouldRunFixed(pipelineId, schedule.getInterval(), true);
            case FIXED_DELAY -> shouldRunFixed(pipelineId, schedule.getInterval(), false);
            default -> false;
        };
    }

    /**
     * CRON 调度：若当前时间不晚于 cron 计算的 next 时刻则触发。
     *
     * @param pipelineId     Pipeline ID
     * @param cronExpression Spring 6 风格 CRON 表达式
     * @return 到达触发窗口返回 true
     */
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

    /**
     * 固定间隔调度：根据距上次执行的毫秒间隔判断是否触发。
     *
     * @param pipelineId Pipeline ID
     * @param intervalSec 间隔秒数
     * @param fixedRate   true 表示 FIXED_RATE，false 表示 FIXED_DELAY（当前实现逻辑相同）
     * @return 间隔已满足返回 true
     */
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

    /**
     * 记录本次触发时间，供下次轮询计算间隔或 CRON next。
     */
    private void markRun(String pipelineId, ScheduleConfig schedule) {
        if (schedule.getScheduleType() == ScheduleConfig.ScheduleType.CRON) {
            lastCronRun.put(pipelineId, LocalDateTime.now(ZoneId.systemDefault()));
        } else {
            lastFixedRunMs.put(pipelineId, System.currentTimeMillis());
        }
    }
}
