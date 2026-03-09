package com.dataflow.ai.business.permission.processor;

import java.util.List;

/**
 * 脱敏处理器
 * 支持多种预定义脱敏规则
 */
public interface MaskProcessor {

    /**
     * 对值进行脱敏
     * @param value 原始值
     * @param rule 脱敏规则 (PHONE, ID_CARD, EMAIL, CUSTOM)
     * @return 脱敏后的值
     */
    Object mask(Object value, String rule);

    /**
     * 判断是否支持该规则
     */
    boolean supports(String rule);

    /**
     * 获取所有支持的规则
     */
    List<String> getSupportedRules();
}