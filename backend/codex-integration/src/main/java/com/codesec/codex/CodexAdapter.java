package com.codesec.codex;

import com.codesec.codex.model.*;

import java.util.List;

public interface CodexAdapter {

    CodexResponse<String> analyzeVuln(CodexRequest request);

    CodexResponse<List<CodexVerdict>> batchFilter(CodexRequest request);

    CodexResponse<PocResult> generatePoc(CodexRequest request);

    CodexResponse<PatchResult> generatePatch(CodexRequest request);

    CodexResponse<List<LogicVulnResult>> mineLogicVulns(CodexRequest request);

    CodexHealth health();
}
