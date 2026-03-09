package com.dataflow.ai.business.service;

import com.dataflow.ai.domain.response.GenerateTransformsResponse;
import com.dataflow.ai.domain.request.GenerateTransformsRequest;
import com.dataflow.ai.domain.request.SearchSimilarRequest;
import com.dataflow.ai.domain.request.FeedbackRequest;
import com.dataflow.ai.domain.response.SearchSimilarResponse;
import com.dataflow.ai.domain.entity.User;

/**
 * AI辅助服务接口
 */
public interface AIService {

    /**
     * 根据自然语言生成转换节点
     * 同步返回，超时时间10-15秒
     */
    GenerateTransformsResponse generateTransforms(GenerateTransformsRequest request,
                                                  User user);

    /**
     * 搜索相似指令
     */
    SearchSimilarResponse searchSimilar(SearchSimilarRequest request);

    /**
     * 提交用户反馈
     */
    void submitFeedback(FeedbackRequest request, User user);
}
