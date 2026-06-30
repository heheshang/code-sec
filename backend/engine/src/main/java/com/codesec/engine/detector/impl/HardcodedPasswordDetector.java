package com.codesec.engine.detector.impl;

import com.codesec.engine.detector.RegexDetector;
import com.codesec.engine.model.Finding;
import com.codesec.engine.parser.ParsedFile;
import com.codesec.engine.rule.Rule;

import java.util.regex.Matcher;

public final class HardcodedPasswordDetector extends RegexDetector {

    @Override
    protected boolean isValidMatch(Finding finding, ParsedFile file, Rule rule, Matcher matcher) {
        if (matcher.groupCount() < 2) {
            return true;
        }
        String variableName = matcher.group(1);
        if (variableName == null) {
            return true;
        }
        String lowerName = variableName.toLowerCase();
        if (lowerName.contains("password") || lowerName.contains("secret")
            || lowerName.contains("apikey") || lowerName.contains("token")) {
            return true;
        }
        if (lowerName.contains("pwd") || lowerName.contains("pass")
            || lowerName.contains("key") || lowerName.contains("credential")) {
            return true;
        }
        return false;
    }
}
