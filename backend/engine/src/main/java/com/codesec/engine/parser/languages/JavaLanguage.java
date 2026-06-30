package com.codesec.engine.parser.languages;

import com.codesec.engine.parser.AstParser;
import com.codesec.engine.parser.ParsedFile;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ParserConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public final class JavaLanguage implements AstParser {
    private static final Logger log = LoggerFactory.getLogger(JavaLanguage.class);
    private static final String LANGUAGE = "java";
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".java");

    static {
        StaticJavaParser.setConfiguration(
            new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
        );
    }

    @Override
    public ParsedFile parse(Path filePath) throws IOException {
        String sourceCode = Files.readString(filePath);
        var compilationUnit = StaticJavaParser.parse(sourceCode);
        log.debug("Parsed Java file: {}", filePath);
        return new ParsedFile(filePath, LANGUAGE, sourceCode, compilationUnit);
    }

    @Override
    public boolean supports(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    @Override
    public String languageName() {
        return LANGUAGE;
    }
}
