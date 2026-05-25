package com.dataflow.ai.business.util;

import cn.hutool.crypto.digest.DigestUtil;

/**
 * 指令文本哈希（与 instruction_patterns.instruction_hash 对应）
 */
public final class InstructionHashUtils {

    private InstructionHashUtils() {
    }

    public static String hash(String instruction) {
        if (instruction == null) {
            return DigestUtil.sha256Hex("");
        }
        return DigestUtil.sha256Hex(instruction.trim().toLowerCase());
    }
}
