package com.codeman.platform.cs.service;

import com.codeman.platform.common.ApiException;
import com.codeman.platform.common.AuditWriter;
import com.codeman.platform.config.AppProperties;
import com.codeman.platform.cs.domain.CsConversation;
import com.codeman.platform.cs.domain.CsMessage;
import com.codeman.platform.cs.repo.CsConversationRepository;
import com.codeman.platform.cs.repo.CsMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 智能客服业务编排：会话/消息管理、知识库召回、调用 {@link ChatProvider} 流式作答、转人工。
 *
 * <p>设计要点：
 * <ul>
 *   <li><b>通用</b>：仅依赖 {@link ChatProvider} 抽象，不绑定任何厂商。</li>
 *   <li><b>优雅降级</b>：未配置上游(app.ai 未就绪)时，直接用知识库命中内容作答，客服窗仍可用。</li>
 *   <li><b>护栏</b>：system 提示限定“仅答本产品相关、不外泄密钥/隐私、不执行写操作”。</li>
 * </ul>
 */
@Service
public class CsService {

    private static final Logger log = LoggerFactory.getLogger(CsService.class);

    private final CsConversationRepository convRepo;
    private final CsMessageRepository msgRepo;
    private final ChatProvider provider;
    private final KnowledgeBase kb;
    private final AppProperties props;
    private final AuditWriter audit;
    private final ExecutorService pool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "cs-stream");
        t.setDaemon(true);
        return t;
    });

    public CsService(CsConversationRepository convRepo, CsMessageRepository msgRepo,
                     ChatProvider provider, KnowledgeBase kb, AppProperties props, AuditWriter audit) {
        this.convRepo = convRepo;
        this.msgRepo = msgRepo;
        this.provider = provider;
        this.kb = kb;
        this.props = props;
        this.audit = audit;
    }

    public List<CsConversation> list() {
        return convRepo.findAllByOrderByUpdatedAtDesc();
    }

    public List<CsConversation> listByUser(String userRef) {
        return convRepo.findByUserRefOrderByUpdatedAtDesc(userRef);
    }

    public List<CsMessage> messages(Long conversationId) {
        return msgRepo.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    @Transactional
    public CsConversation open(String userRef, String tenantCode, String firstTitle) {
        CsConversation c = new CsConversation();
        c.setUserRef(userRef);
        c.setTenantCode(tenantCode);
        c.setTitle(firstTitle == null || firstTitle.isBlank() ? "新会话"
                : (firstTitle.length() > 40 ? firstTitle.substring(0, 40) : firstTitle));
        c.setStatus("OPEN");
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        return convRepo.save(c);
    }

    @Transactional
    public CsConversation escalate(Long conversationId) {
        CsConversation c = require(conversationId);
        c.setStatus("ESCALATED");
        c.setUpdatedAt(LocalDateTime.now());
        convRepo.save(c);
        audit.log(null, "CS_ESCALATE", "客服会话#" + conversationId + " 转人工");
        return c;
    }

    private CsConversation require(Long id) {
        return convRepo.findById(id).orElseThrow(() -> ApiException.notFound("会话不存在"));
    }

    /**
     * 处理一次提问并以 SSE 流式返回回复。会话不存在时自动新建。
     * @return SseEmitter 直接交给 MVC 输出
     */
    public SseEmitter chat(Long conversationId, String userRef, String tenantCode, String question) {
        if (question == null || question.isBlank()) throw ApiException.badRequest("问题不能为空");
        CsConversation conv = conversationId == null ? open(userRef, tenantCode, question) : require(conversationId);

        // 落库用户消息
        msgRepo.save(new CsMessage(conv.getId(), "user", question));

        // 召回知识库 + 组装消息序列
        List<KnowledgeBase.Entry> hits = kb.search(question, 3);
        List<ChatProvider.Msg> messages = buildMessages(conv.getId(), hits, question);

        SseEmitter emitter = new SseEmitter(Long.valueOf(props.getAi().getTimeoutSec() + 30) * 1000);
        final Long convId = conv.getId();
        // 先把会话 id 发给前端(便于其继续在同一会话提问)
        try { emitter.send(SseEmitter.event().name("meta").data("{\"conversationId\":" + convId + "}")); }
        catch (Exception ignore) { }

        pool.submit(() -> {
            StringBuilder full = new StringBuilder();
            try {
                if (provider.ready()) {
                    String text = provider.stream(messages, tok -> emit(emitter, "delta", tok));
                    full.append(text);
                } else {
                    // 降级：无上游时直接给知识库命中内容
                    String fallback = degrade(hits);
                    full.append(fallback);
                    emit(emitter, "delta", fallback);
                }
                persistAssistant(convId, full.toString());
                emitter.send(SseEmitter.event().name("done").data("{}"));
                emitter.complete();
            } catch (Exception e) {
                log.warn("[cs] 会话#{} 作答失败：{}", convId, e.getMessage());
                String msg = "抱歉，客服暂时不可用：" + e.getMessage() + "。您可点击“转人工”。";
                emit(emitter, "delta", msg);
                persistAssistant(convId, msg);
                try { emitter.send(SseEmitter.event().name("done").data("{}")); } catch (Exception ignore) { }
                emitter.complete();
            }
        });
        return emitter;
    }

    private void emit(SseEmitter emitter, String name, String data) {
        try { emitter.send(SseEmitter.event().name(name).data(data, org.springframework.http.MediaType.TEXT_PLAIN)); }
        catch (Exception e) { /* 客户端断开则忽略 */ }
    }

    @Transactional
    void persistAssistant(Long convId, String content) {
        msgRepo.save(new CsMessage(convId, "assistant", content));
        convRepo.findById(convId).ifPresent(c -> { c.setUpdatedAt(LocalDateTime.now()); convRepo.save(c); });
    }

    /** system 护栏 + 知识库参考资料 + 历史(截断) + 当前问题。 */
    private List<ChatProvider.Msg> buildMessages(Long convId, List<KnowledgeBase.Entry> hits, String question) {
        StringBuilder sys = new StringBuilder();
        sys.append("你是 CODEMAN(软件授权运营平台)的智能客服。请遵守：\n")
           .append("1) 只回答与本产品相关的问题；无关问题礼貌拒绝并引导。\n")
           .append("2) 优先依据下方【参考资料】作答；资料不足时如实说明并建议“转人工”。\n")
           .append("3) 严禁透露或编造私钥、密码、完整密钥串、密钥配置或他人隐私。\n")
           .append("4) 你只提供信息，不执行任何账号/数据写操作。\n")
           .append("5) 用简体中文，简洁、分点、友好。\n");
        if (!hits.isEmpty()) {
            sys.append("\n【参考资料】\n");
            int i = 1;
            for (KnowledgeBase.Entry e : hits) {
                sys.append(i++).append(". ").append(e.question()).append("：").append(e.answer()).append("\n");
            }
        }
        List<ChatProvider.Msg> list = new ArrayList<>();
        list.add(new ChatProvider.Msg("system", sys.toString()));
        // 历史(不含刚落库的当前问题本身，保留最近 N 条)
        List<CsMessage> history = msgRepo.findByConversationIdOrderByCreatedAtAsc(convId);
        int limit = props.getAi().getHistoryLimit();
        int from = Math.max(0, history.size() - 1 - limit);   // -1 排除末尾刚插入的 user 问题
        for (int i = from; i < history.size() - 1; i++) {
            CsMessage m = history.get(i);
            if ("user".equals(m.getRole()) || "assistant".equals(m.getRole())) {
                list.add(new ChatProvider.Msg(m.getRole(), m.getContent()));
            }
        }
        list.add(new ChatProvider.Msg("user", question));
        return list;
    }

    private String degrade(List<KnowledgeBase.Entry> hits) {
        if (hits.isEmpty()) {
            return "（当前未接入大模型，智能问答暂不可用）未在知识库中找到相关内容，建议点击“转人工”由人工为您解答。";
        }
        StringBuilder sb = new StringBuilder("（当前未接入大模型，以下为知识库匹配结果）\n");
        int i = 1;
        for (KnowledgeBase.Entry e : hits) {
            sb.append("\n").append(i++).append("、").append(e.question()).append("\n").append(e.answer()).append("\n");
        }
        sb.append("\n如未解决，请点击“转人工”。");
        return sb.toString();
    }
}
