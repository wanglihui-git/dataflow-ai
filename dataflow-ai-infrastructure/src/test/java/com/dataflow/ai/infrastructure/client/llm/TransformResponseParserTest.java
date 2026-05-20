package com.dataflow.ai.infrastructure.client.llm;

import com.dataflow.ai.domain.exception.BusinessException;
import com.dataflow.ai.domain.enums.TransformType;
import com.dataflow.ai.domain.vo.Transform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransformResponseParserTest {

    private final TransformResponseParser parser = new TransformResponseParser();

    @Test
    @DisplayName("parse - 解析 nodes 数组")
    void parse_validNodes() {
        String json = """
                {
                  "nodes": [
                    {
                      "nodeId": "n1",
                      "type": "FIELD_MAPPER",
                      "name": "Map fields",
                      "config": {"mappings": []},
                      "dependsOn": []
                    }
                  ]
                }
                """;
        List<Transform> nodes = parser.parse(json);
        assertEquals(1, nodes.size());
        assertEquals("n1", nodes.get(0).getNodeId());
        assertEquals(TransformType.FIELD_MAPPER, nodes.get(0).getType());
    }

    @Test
    @DisplayName("parse - 支持 markdown 代码块包裹")
    void parse_markdownFenced() {
        String json = """
                ```json
                {"nodes":[{"nodeId":"a","type":"FILTER","dependsOn":[]}]}
                ```
                """;
        List<Transform> nodes = parser.parse(json);
        assertEquals(1, nodes.size());
        assertEquals(TransformType.FILTER, nodes.get(0).getType());
    }

    @Test
    @DisplayName("parse - 非法 JSON 抛出 BusinessException")
    void parse_invalidJson() {
        assertThrows(BusinessException.class, () -> parser.parse("not json"));
    }

    @Test
    @DisplayName("stripMarkdownFences")
    void stripMarkdownFences() {
        assertEquals("{\"a\":1}", TransformResponseParser.stripMarkdownFences("```\n{\"a\":1}\n```"));
    }
}
