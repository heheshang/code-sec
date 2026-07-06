package com.codesec.api.e2e;

import com.codesec.domain.entity.*;
import com.codesec.domain.repository.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * E-S3-PDF: PDF export E2E tests.
 * Tests single ticket PDF export and batch ZIP export.
 */
public class E14PdfExportE2ETest extends BaseE2ETest {
    @Autowired
    private VulnFindingRepository vulnRepo;
    @Autowired
    private VulnTicketRepository ticketRepo;
    @Autowired
    private AuditRecordRepository auditRepo;

    private String token;
    private Long ticketId;
    private Long vulnId;

    @BeforeEach
    void setUp() throws Exception {
        token = getAdminToken();

        // Create a repo to own the test data
        MvcResult repoResult = mockMvc.perform(post("/api/v1/repos")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("""
                    {
                      "name": "pdf-export-test",
                      "platform": "gitlab",
                      "url": "https://gitlab.example.com/pdf-test",
                      "accessToken": "test-token",
                      "businessLine": "Test",
                      "gitlabProjectId": 14001
                    }
                    """))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode repoTree = new ObjectMapper().readTree(repoResult.getResponse().getContentAsString());
        Long repoId = repoTree.get("id").asLong();

        // Seed a vuln finding directly
        var vuln = vulnRepo.save(VulnFindingEntity.builder()
            .scanTaskId(99001L)
            .projectId(repoId)
            .ruleId("sql-injection-001")
            .severity("critical")
            .exploitability("exploitable")
            .title("SQL Injection in UserController.findById")
            .description("User input flows unsanitized into a MyBatis ${} interpolation, allowing SQL injection.")
            .codeSnippet("""
                String sql = "SELECT * FROM users WHERE id = " + userId;
                return jdbcTemplate.queryForList(sql);""")
            .filePath("src/main/java/com/example/UserController.java")
            .lineStart(42)
            .lineEnd(45)
            .cwe("CWE-89")
            .engine("self_sast")
            .discoveredAt(LocalDateTime.now())
            .build());
        vulnId = vuln.getId();

        // Seed a ticket linked to the vuln finding
        var ticket = ticketRepo.save(VulnTicketEntity.builder()
            .vulnId(vulnId)
            .projectId(repoId)
            .status("confirmed")
            .severity("critical")
            .createdAt(LocalDateTime.now())
            .build());
        ticketId = ticket.getId();

        // Seed an audit record
        auditRepo.save(AuditRecordEntity.builder()
            .vulnId(vulnId)
            .auditorId(1L)
            .action("confirmed")
            .exploitCondition("Attacker-controlled userId parameter reaches raw SQL execution")
            .fixSuggestion("Use parameterized queries with ? placeholders")
            .build());
    }

    @Test
    void shouldExportSingleTicketAsPdf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/tickets/" + ticketId + "/export")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=ticket-" + ticketId + ".pdf"))
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 200, "PDF should be larger than 200 bytes");

        // Verify it's a valid PDF (starts with %PDF-)
        String pdfHeader = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", pdfHeader, "Response should be a valid PDF");
    }

    @Test
    void shouldExportBatchTicketsAsZip() throws Exception {
        var tree = new ObjectMapper().readTree(
            mockMvc.perform(get("/api/v1/tickets")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString()
        );

        JsonNode items = tree.get("items");
        if (items.size() == 0) return; // skip if no tickets

        var ticketIds = new java.util.ArrayList<Long>();
        for (var item : items) {
            ticketIds.add(item.get("id").asLong());
        }

        MvcResult result = mockMvc.perform(post("/api/v1/tickets/export-batch")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(new ObjectMapper().writeValueAsString(ticketIds)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=tickets-export.zip"))
                .andExpect(header().string("Content-Type", "application/zip"))
                .andReturn();

        byte[] zipBytes = result.getResponse().getContentAsByteArray();
        assertNotNull(zipBytes);
        assertTrue(zipBytes.length > 200, "ZIP should be larger than 200 bytes");

        // Verify it's a valid ZIP containing PDFs
        var zis = new ZipInputStream(new ByteArrayInputStream(zipBytes));
        int entryCount = 0;
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            assertTrue(entry.getName().matches("ticket-\\d+\\.pdf"),
                "ZIP entry should be a PDF: " + entry.getName());
            entryCount++;
            zis.closeEntry();
        }
        zis.close();
        assertTrue(entryCount > 0, "ZIP should contain at least one PDF");
    }

    @Test
    void shouldRejectExportForNonexistentTicket() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/999999/export")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError());
    }
}
