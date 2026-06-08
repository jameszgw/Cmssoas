package com.codeman.platform.cs.web;

import com.codeman.platform.config.AppProperties;
import com.codeman.platform.cs.domain.CsConversation;
import com.codeman.platform.cs.domain.CsMessage;
import com.codeman.platform.cs.dto.CsDtos.ChatReq;
import com.codeman.platform.cs.service.ChatProvider;
import com.codeman.platform.cs.service.CsService;
import com.codeman.platform.cs.service.KnowledgeBase;
import com.codeman.platform.rbac.service.CurrentUser;
import com.codeman.platform.rbac.service.RequirePerm;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/** 智能客服接口：流式对话(SSE) + 转人工 + 会话审计。 */
@RestController
@RequestMapping("/api/cs")
public class CsController {

    private final CsService service;
    private final ChatProvider provider;
    private final KnowledgeBase kb;
    private final AppProperties props;

    public CsController(CsService service, ChatProvider provider, KnowledgeBase kb, AppProperties props) {
        this.service = service;
        this.provider = provider;
        this.kb = kb;
        this.props = props;
    }

    /** 客服状态(前端展示是否接入大模型/降级)。 */
    @GetMapping("/status")
    @RequirePerm("cs:use")
    public Map<String, Object> status() {
        return Map.of(
                "ready", provider.ready(),
                "model", provider.ready() ? props.getAi().getModel() : "",
                "kbSize", kb.size());
    }

    /** 流式对话。conversationId 为空则自动新建会话。 */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequirePerm("cs:use")
    public SseEmitter chat(@RequestBody ChatReq r) {
        String user = userRef();
        return service.chat(r.conversationId(), user, null, r.question());
    }

    @PostMapping("/{id}/escalate")
    @RequirePerm("cs:use")
    public CsConversation escalate(@PathVariable Long id) {
        return service.escalate(id);
    }

    /** 我的会话(当前登录用户)。 */
    @GetMapping("/conversations/mine")
    @RequirePerm("cs:use")
    public List<CsConversation> mine() {
        return service.listByUser(userRef());
    }

    @GetMapping("/conversations/{id}/messages")
    @RequirePerm("cs:use")
    public List<CsMessage> messages(@PathVariable Long id) {
        return service.messages(id);
    }

    /** 全部会话(审计)。 */
    @GetMapping("/conversations")
    @RequirePerm("cs:view")
    public List<CsConversation> all() {
        return service.list();
    }

    private String userRef() {
        CurrentUser.Ctx c = CurrentUser.get();
        return c == null ? "anonymous" : c.username();
    }
}
