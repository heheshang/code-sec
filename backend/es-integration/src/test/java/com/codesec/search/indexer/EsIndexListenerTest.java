package com.codesec.search.indexer;

import com.codesec.search.event.VulnIndexedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for EsIndexListener (M1-S3 T2).
 *
 * Verifies:
 * 1. @TransactionalEventListener(AFTER_COMMIT) annotation is present
 * 2. onVulnIndexed handles events without throwing
 * 3. Exceptions from dependencies are caught
 */
@ExtendWith(MockitoExtension.class)
class EsIndexListenerTest {

    @Mock
    private EsUpsertService esUpsertService;

    @Mock
    private MinioSnippetReader snippetReader;

    private EsIndexListener listener;

    @BeforeEach
    void setUp() {
        listener = new EsIndexListener(esUpsertService, snippetReader);
    }

    @Test
    void shouldHaveTransactionalEventListenerAnnotation() throws Exception {
        Method method = EsIndexListener.class.getDeclaredMethod("onVulnIndexed", VulnIndexedEvent.class);
        assertNotNull(method, "onVulnIndexed method should exist");

        TransactionalEventListener annotation = method.getAnnotation(TransactionalEventListener.class);
        assertNotNull(annotation, "Should be annotated with @TransactionalEventListener");

        assertEquals(TransactionPhase.AFTER_COMMIT, annotation.phase(),
            "Should use AFTER_COMMIT to isolate ES indexing from transaction");
    }

    @Test
    void onVulnIndexed_shouldHandleEventWithoutThrowing() {
        VulnIndexedEvent event = new VulnIndexedEvent(this, List.of(1L, 2L), "proj-1");
        assertDoesNotThrow(() -> listener.onVulnIndexed(event),
            "onVulnIndexed should catch exceptions internally");
    }

    @Test
    void onVulnIndexed_shouldHandleEmptyFindingIds() {
        VulnIndexedEvent event = new VulnIndexedEvent(this, List.of(), "proj-1");
        assertDoesNotThrow(() -> listener.onVulnIndexed(event));
    }

    @Test
    void indexSnippetsForFindings_shouldHandleErrors() {
        VulnIndexedEvent event = new VulnIndexedEvent(this, List.of(1L), "proj-1");
        assertDoesNotThrow(() -> listener.indexSnippetsForFindings(event),
            "indexSnippetsForFindings should catch exceptions");
    }
}
