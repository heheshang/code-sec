package com.codesec.codex.capability;

import com.codesec.codex.client.CodexClient;
import com.codesec.codex.config.CodexProperties;
import com.codesec.codex.model.CodexRequest;
import com.codesec.codex.model.CodexResponse;
import com.codesec.codex.model.LogicVulnResult;
import com.codesec.codex.prompt.PromptLoader;
import com.codesec.codex.prompt.PromptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LogicVulnMiningCapabilityTest {

    private CodexClient mockClient;
    private CodexProperties props;
    private LogicVulnMiningCapability capability;

    @BeforeEach
    void setUp() {
        mockClient = mock(CodexClient.class);
        PromptLoader loader = new PromptLoader();
        PromptRepository repo = new PromptRepository(loader);
        props = new CodexProperties();
        props.getCapabilities().setLogicVulnMining(true);
        capability = new LogicVulnMiningCapability(mockClient, repo, props);
    }

    @Test
    void getTypeReturnsLogicVulnMining() {
        assertEquals(com.codesec.codex.model.CodexCapability.LOGIC_VULN_MINING, capability.getType());
    }

    @Test
    void isEnabledWhenConfiguredTrue() {
        assertTrue(capability.isEnabled());
    }

    @Test
    void isDisabledWhenConfiguredFalse() {
        props.getCapabilities().setLogicVulnMining(false);
        assertFalse(capability.isEnabled());
    }

    @Test
    void executeReturnsEmptyListWhenNoVulnsFound() {
        when(mockClient.execute(any(), anyString(), anyString())).thenReturn("[]");
        CodexRequest req = makeRequest();
        CodexResponse<List<LogicVulnResult>> resp = capability.execute(req);
        assertTrue(resp.isSuccess());
        assertNotNull(resp.getData());
        assertTrue(resp.getData().isEmpty());
    }

    @Test
    void executeReturnsLogicVulnResults() {
        String json = """
            [
              {
                "vuln_type": "BusinessLogicBypass",
                "evidence_chain": ["步骤1: 未校验订单状态", "步骤2: 重复提交"],
                "exploit_condition": "未登录状态可访问订单接口",
                "risk_level": "HIGH",
                "recommended_fix": "添加订单状态校验",
                "code_snippet": "if (order.status == PENDING) { process(order); }",
                "line_start": 42,
                "line_end": 45
              },
              {
                "vuln_type": "RaceCondition",
                "evidence_chain": ["步骤1: 并发扣减", "步骤2: 余额超支"],
                "exploit_condition": "高并发下可重复扣减",
                "risk_level": "CRITICAL",
                "recommended_fix": "添加分布式锁",
                "code_snippet": "balance -= amount;",
                "line_start": 100,
                "line_end": 102
              }
            ]
            """;
        when(mockClient.execute(any(), anyString(), anyString())).thenReturn(json);
        CodexRequest req = makeRequest();
        CodexResponse<List<LogicVulnResult>> resp = capability.execute(req);
        assertTrue(resp.isSuccess());
        assertNotNull(resp.getData());
        assertEquals(2, resp.getData().size());
        assertEquals("BusinessLogicBypass", resp.getData().get(0).getVulnType());
        assertEquals("RaceCondition", resp.getData().get(1).getVulnType());
        assertEquals("HIGH", resp.getData().get(0).getRiskLevel());
        assertEquals("CRITICAL", resp.getData().get(1).getRiskLevel());
        assertEquals(2, resp.getData().get(0).getEvidenceChain().size());
        assertEquals("步骤1: 未校验订单状态", resp.getData().get(0).getEvidenceChain().get(0));
    }

    @Test
    void executeReturnsErrorResponseWhenDisabled() {
        props.getCapabilities().setLogicVulnMining(false);
        CodexResponse<List<LogicVulnResult>> resp = capability.execute(makeRequest());
        assertFalse(resp.isSuccess());
        assertTrue(resp.getErrorMessage().contains("disabled"));
    }

    @Test
    void executeHandlesMalformedJsonGracefully() {
        when(mockClient.execute(any(), anyString(), anyString())).thenReturn("not json at all");
        CodexResponse<List<LogicVulnResult>> resp = capability.execute(makeRequest());
        assertTrue(resp.isSuccess());
        assertNotNull(resp.getData());
        assertTrue(resp.getData().isEmpty());
    }

    @Test
    void executeHandlesNullResponse() {
        when(mockClient.execute(any(), anyString(), anyString())).thenReturn(null);
        CodexResponse<List<LogicVulnResult>> resp = capability.execute(makeRequest());
        assertTrue(resp.isSuccess());
        assertNotNull(resp.getData());
        assertTrue(resp.getData().isEmpty());
    }

    private CodexRequest makeRequest() {
        CodexRequest req = new CodexRequest("scan-1", "vuln-1", "java",
            "public void processOrder(Order order) {\n    if (order.getAmount() > 0) {\n        balance -= order.getAmount();\n    }\n}",
            "src/main/java/com/example/OrderService.java", 10, 30);
        req.setCallChain("OrderService.processOrder -> AccountService.deduct");
        req.setDataSource("HttpServletRequest.getParameter");
        req.setReachable(true);
        req.setExtra(Map.of("frameworkProtection", "无"));
        return req;
    }
}
