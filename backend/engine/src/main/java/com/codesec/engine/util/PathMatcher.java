package com.codesec.engine.util;

import java.nio.file.Path;
import java.util.List;

public final class PathMatcher {

    private PathMatcher() {}

    public static boolean isExcluded(Path filePath, List<String> excludePatterns) {
        if (excludePatterns.isEmpty()) {
            return false;
        }
        String relativePath = filePath.toString().replace('\\', '/');
        for (String pattern : excludePatterns) {
            if (matchesGlob(relativePath, pattern)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesGlob(String path, String glob) {
        String regex = globToRegex(glob);
        return path.matches(regex);
    }

    private static String globToRegex(String glob) {
        StringBuilder sb = new StringBuilder();
        sb.append('^');
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*' -> {
                    if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                        i++;
                        if (i + 1 < glob.length() && glob.charAt(i + 1) == '/') {
                            i++;
                            sb.append("(.*/)?");
                        } else {
                            sb.append(".*");
                        }
                    } else {
                        sb.append("[^/]*");
                    }
                }
                case '?' -> sb.append('.');
                case '.' -> sb.append("\\.");
                case '/' -> sb.append('/');
                default -> sb.append(c);
            }
        }
        sb.append('$');
        return sb.toString();
    }
}
