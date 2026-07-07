package com.codesec.api.module.cpg;

import com.codesec.api.module.cpg.controller.CpgController;
import com.codesec.api.module.cpg.dto.CpgResponse;
import com.codesec.engine.judge.CpgService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CpgControllerTest {

    @Mock
    private CpgQueryService cpgQueryService;
    @Mock
    private CpgService cpgService;

    @InjectMocks
    private CpgController controller;

    @Test
    void getCpg_WhenCpgUnavailable_ReturnsNotFound() {
        when(cpgQueryService.getCpgForVuln(1L)).thenReturn(Optional.empty());

        ResponseEntity<CpgResponse> resp = controller.getCpg(1L);

        assertTrue(resp.getStatusCode().is4xxClientError());
        verify(cpgQueryService).getCpgForVuln(1L);
    }

    @Test
    void getCpg_WhenVulnNotFound_ReturnsNotFound() {
        when(cpgQueryService.getCpgForVuln(999L)).thenReturn(Optional.empty());

        ResponseEntity<CpgResponse> resp = controller.getCpg(999L);

        assertTrue(resp.getStatusCode().is4xxClientError());
        verify(cpgService, never()).findLatestByProjectId(any());
    }

    @Test
    void getCpg_WhenDataFound_Returns200() {
        CpgResponse cpgResponse = new CpgResponse();
        when(cpgQueryService.getCpgForVuln(1L)).thenReturn(Optional.of(cpgResponse));

        ResponseEntity<CpgResponse> resp = controller.getCpg(1L);

        assertTrue(resp.getStatusCode().is2xxSuccessful());
        assertNotNull(resp.getBody());
    }
}
