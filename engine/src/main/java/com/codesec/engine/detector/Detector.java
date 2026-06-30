package com.codesec.engine.detector;

import com.codesec.engine.model.Finding;
import com.codesec.engine.parser.ParsedFile;
import com.codesec.engine.rule.Rule;

import java.util.List;

@FunctionalInterface
public interface Detector {
    List<Finding> detect(ParsedFile file, Rule rule);
}
