package com.dataflow.ai.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据源连通性测试结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionTestResult {

  /** 是否连接成功 */
  private boolean connected;

  /** 结果说明（成功或失败原因，便于排查） */
  private String message;

  /**
   * 连接成功结果。
   *
   * @return 成功结果
   */
  public static ConnectionTestResult success() {
    return ConnectionTestResult.builder().connected(true).message("连接成功").build();
  }

  /**
   * 连接失败结果。
   *
   * @param message 失败原因
   * @return 失败结果
   */
  public static ConnectionTestResult failure(String message) {
    return ConnectionTestResult.builder().connected(false).message(message).build();
  }
}
