package com.codesec.api.module.cpg.dto;

import java.util.List;

public class CpgResponse {
    private List<CpgNode> nodes;
    private List<CpgEdge> edges;

    public CpgResponse() {}

    public CpgResponse(List<CpgNode> nodes, List<CpgEdge> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public List<CpgNode> getNodes() { return nodes; }
    public void setNodes(List<CpgNode> nodes) { this.nodes = nodes; }
    public List<CpgEdge> getEdges() { return edges; }
    public void setEdges(List<CpgEdge> edges) { this.edges = edges; }

    public static class CpgNode {
        private String id;
        private String label;
        private String type;
        private String filePath;
        private int lineNumber;

        public CpgNode() {}

        public CpgNode(String id, String label, String type, String filePath, int lineNumber) {
            this.id = id;
            this.label = label;
            this.type = type;
            this.filePath = filePath;
            this.lineNumber = lineNumber;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public int getLineNumber() { return lineNumber; }
        public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
    }

    public static class CpgEdge {
        private String sourceId;
        private String targetId;
        private String type;

        public CpgEdge() {}

        public CpgEdge(String sourceId, String targetId, String type) {
            this.sourceId = sourceId;
            this.targetId = targetId;
            this.type = type;
        }

        public String getSourceId() { return sourceId; }
        public void setSourceId(String sourceId) { this.sourceId = sourceId; }
        public String getTargetId() { return targetId; }
        public void setTargetId(String targetId) { this.targetId = targetId; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}
