package com.cmssoas.platform.cs.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 极简知识库检索(关键词召回，不引入向量库等重组件)。
 *
 * <p>从 {@code classpath:knowledge/faq.md} 加载 FAQ，按 "## 问题" 切分为条目；
 * 检索时用问题与条目的字符 2-gram 重叠度打分，返回 Top-N 拼进 system 提示作为参考资料。
 * 初版足够好用；后续如需更强语义检索，仅替换本类实现即可，上层无感知。
 */
@Service
public class KnowledgeBase {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBase.class);

    /** 单条 FAQ。 */
    public record Entry(String question, String answer) {
        String text() { return question + "\n" + answer; }
    }

    private List<Entry> entries = new ArrayList<>();

    @PostConstruct
    void load() {
        List<Entry> list = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                new ClassPathResource("knowledge/faq.md").getInputStream(), StandardCharsets.UTF_8))) {
            String line; String q = null; StringBuilder a = new StringBuilder();
            while ((line = r.readLine()) != null) {
                if (line.startsWith("## ")) {
                    if (q != null) list.add(new Entry(q, a.toString().trim()));
                    q = line.substring(3).trim();
                    a.setLength(0);
                } else if (q != null && !line.startsWith("#") && !line.startsWith(">")) {
                    a.append(line).append('\n');
                }
            }
            if (q != null) list.add(new Entry(q, a.toString().trim()));
        } catch (Exception e) {
            log.warn("[cs] 知识库加载失败，将以空库运行：{}", e.getMessage());
        }
        this.entries = list;
        log.info("[cs] 知识库已加载 {} 条 FAQ", list.size());
    }

    /** 召回与 query 最相关的最多 topN 条 FAQ(得分>0)。 */
    public List<Entry> search(String query, int topN) {
        if (query == null || query.isBlank() || entries.isEmpty()) return List.of();
        Set<String> qg = grams(query);
        if (qg.isEmpty()) return List.of();
        return entries.stream()
                .map(e -> Map.entry(e, score(qg, e)))
                .filter(en -> en.getValue() > 0)
                .sorted((x, y) -> Double.compare(y.getValue(), x.getValue()))
                .limit(topN)
                .map(Map.Entry::getKey)
                .toList();
    }

    private double score(Set<String> qg, Entry e) {
        Set<String> eg = grams(e.text());
        if (eg.isEmpty()) return 0;
        long hit = qg.stream().filter(eg::contains).count();
        return (double) hit / qg.size();   // 召回率口径，问题命中比例
    }

    /** 字符 2-gram(去空白/标点)，对中文/英文均适用、无需分词。 */
    private Set<String> grams(String s) {
        String norm = s.toLowerCase().replaceAll("[\\s\\p{Punct}，。、；：？！“”‘’（）【】]", "");
        Set<String> g = new HashSet<>();
        for (int i = 0; i + 2 <= norm.length(); i++) g.add(norm.substring(i, i + 2));
        if (norm.length() == 1) g.add(norm);
        return g;
    }

    public int size() { return entries.size(); }
}
