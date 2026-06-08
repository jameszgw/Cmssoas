package com.codeman.platform.cs;

import com.codeman.platform.cs.service.KnowledgeBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 知识库关键词召回(纯单元，不依赖 Spring 上下文)。 */
class KnowledgeBaseTest {

    private KnowledgeBase kb;

    @BeforeEach
    void setup() {
        kb = new KnowledgeBase();
        // 触发 @PostConstruct 的加载逻辑
        ReflectionTestUtils.invokeMethod(kb, "load");
    }

    @Test
    void loadsFaqEntries() {
        assertTrue(kb.size() >= 10, "应从 classpath 加载到 FAQ 条目");
    }

    @Test
    void retrievesRelevantEntry() {
        List<KnowledgeBase.Entry> hits = kb.search("License 是怎么签发的", 3);
        assertFalse(hits.isEmpty(), "应召回相关条目");
        boolean licenseHit = hits.stream().anyMatch(e -> e.question().contains("License"));
        assertTrue(licenseHit, "Top 结果应包含 License 相关条目");
    }

    @Test
    void emptyQueryReturnsNothing() {
        assertTrue(kb.search("", 3).isEmpty());
        assertTrue(kb.search(null, 3).isEmpty());
    }
}
