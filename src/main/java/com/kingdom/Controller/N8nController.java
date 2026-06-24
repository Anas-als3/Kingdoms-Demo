package com.kingdom.Controller;

import com.kingdom.API.ApiResponse;
import com.kingdom.Service.APIService.N8nWebhookClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * n8n PUSH endpoint. POST /api/v1/n8n/test fires a sample payload at the configured n8n webhook (n8n.webhook.url)
 * so you can confirm the backend -> n8n pipe works before wiring real triggers. Admin-only (it triggers an
 * external workflow). Once Maysun confirms the payload her workflow expects, real events can call
 * {@link N8nWebhookClient#send} directly.
 */
@RestController
@RequestMapping("/api/v1/n8n")
@RequiredArgsConstructor
public class N8nController {

    private final N8nWebhookClient n8nWebhookClient;

    @PostMapping("/test")
    public ApiResponse test(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "test");
        payload.put("message", (body != null && body.get("message") != null)
                ? body.get("message") : "مرحبًا من Kingdom 👑 — تم توصيل خطاف الويب الخاص بـ n8n.");
        payload.put("sentAt", LocalDateTime.now().toString());

        boolean ok = n8nWebhookClient.send(payload);
        return new ApiResponse(ok
                ? "تم الإرسال إلى خطاف الويب الخاص بـ n8n ✅ — تحقق من تنفيذ سير العمل في n8n."
                : "تعذّر الوصول إلى خطاف الويب الخاص بـ n8n. هل n8n قيد التشغيل وهل سير العمل يستمع للحدث؟ "
                  + "(تم ضبطه في n8n.webhook.url)");
    }
}
