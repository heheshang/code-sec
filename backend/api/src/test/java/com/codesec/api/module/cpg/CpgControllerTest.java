package com.codesec.api.module.cpg;

import com.codesec.domain.entity.VulnFindingEntity;
import com.codesec.domain.repository.VulnFindingRepository;
import com.codesec.api.module.cpg.dto.CpgResponse;
import com.codesec.engine.judge.CpgService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CpgControllerTest {

    @Mock
    private CpgService cpgService;

    @Mock
    private VulnFindingRepository vulnRepo;

    @InjectMocks
    private CpgController controller;

    @Test
    void getCpg_WhenCpgUnavailable_ReturnsNotFound() {
        when(cpgService.isAvailable()).thenReturn(false);

        ResponseEntity<CpgResponse> resp = controller.getCpg(1L);

        assertTrue(resp.getStatusCode().is4xxClientError());
        verify(vulnRepo, never()).findById(any());
    }

    @Test
    void getCpg_WhenVulnNotFound_ReturnsNotFound() {
        when(cpgService.isAvailable()).thenReturn(true);
        when(vulnRepo.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<CpgResponse> resp = controller.getCpg(999L);

        assertTrue(resp.getStatusCode().is4xxClientError());
        verify(cpgService, never()).findLatestByProjectId(any());
    }

    @Test
    void getCpg_WhenDataFound_Returns200() {
        VulnFindingEntity vuln = new VulnFindingEntity();
        vuln.setId(1L);
        vuln.setScanTaskId(42L);
        when(cpgService.isAvailable()).thenReturn(true);
        when(vulnRepo.findById(1L)).thenReturn(Optional.of(vuln));
        when(cpgService.findLatestByProjectId("42")).thenReturn(List.of());
        when(cpgService.findReachablePaths("42")).thenReturn(List.of());

        ResponseEntity<CpgResponse> resp = controller.getCpg(1L);

        assertTrue(resp.getStatusCode().is2xxSuccessful());
        assertNotNull(resp.getBody());
    }
}
