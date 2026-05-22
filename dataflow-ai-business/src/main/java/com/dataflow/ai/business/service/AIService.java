package com.dataflow.ai.business.service;

import com.dataflow.ai.domain.response.GenerateTransformsResponse;
import com.dataflow.ai.domain.request.GenerateTransformsRequest;
import com.dataflow.ai.domain.request.SearchSimilarRequest;
import com.dataflow.ai.domain.request.FeedbackRequest;
import com.dataflow.ai.domain.response.SearchSimilarResponse;
import com.dataflow.ai.domain.entity.User;

/**
 * AI 辅助服务接口。
 * <p>基于 LLM/向量检索生成 Pipeline 转换节点，并支持相似指令搜索与用户反馈闭环。</p>
 */
public interface AIService {

    /**
     * 根据自然语言指令生成转换节点（同步，通常 10–15 秒内返回）。
     * <p>优先命中历史指令模式以跳过 LLM 调用。</p>
     *
     * @param request 指令、上下文及关联 Pipeline ID
     * @param user    当前操作用户
     * @return 生成的节点列表、来源类型及元数据
     */
    GenerateTransformsResponse generateTransforms(GenerateTransformsRequest request,
                                                  User user);

    /**
     * 按向量相似度搜索历史相似指令及采纳率。
     *
     * @param request 查询指令与相似度阈值、条数限制
     * @return 相似结果列表
     */
    SearchSimilarResponse searchSimilar(SearchSimilarRequest request);

    /**
     * 提交用户对 AI 生成结果的反馈（接受/拒绝/修改），并更新指令模式库。
     *
     * @param request 含 aiHelperId、action 及可选修改节点
     * @param user    当前用户
     */
    void submitFeedback(FeedbackRequest request, User user);
}
