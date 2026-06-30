package com.codesec.engine.judge;

import java.util.List;
import java.util.Set;

/**
 * User input source types that introduce taint into data flow.
 * <p>
 * Detected in sink method bodies or on method parameters to determine
 * if the data reaching a vulnerability is user-controllable.
 */
public enum TaintSource {

    HTTP_REQUEST_GET_PARAMETER("HttpServletRequest.getParameter()"),
    HTTP_REQUEST_GET_HEADER("HttpServletRequest.getHeader()"),
    HTTP_REQUEST_GET_COOKIE("HttpServletRequest.getCookies()"),
    HTTP_REQUEST_GET_BODY("HttpServletRequest.getInputStream() / getReader()"),
    HTTP_REQUEST_GET_QUERY_STRING("HttpServletRequest.getQueryString()"),
    HTTP_REQUEST_GET_REQUEST_URI("HttpServletRequest.getRequestURI()"),
    HTTP_REQUEST_GET_PATH_INFO("HttpServletRequest.getPathInfo()"),
    SPRING_REQUEST_PARAM("@RequestParam"),
    SPRING_REQUEST_BODY("@RequestBody"),
    SPRING_PATH_VARIABLE("@PathVariable"),
    SPRING_REQUEST_HEADER("@RequestHeader"),
    SPRING_REQUEST_COOKIE("@CookieValue");

    private final String description;

    TaintSource(String description) {
        this.description = description;
    }

    /** Returns a human-readable description of this taint source. */
    public String getDescription() {
        return description;
    }

    /**
     * Fully qualified Spring annotation names that indicate a user-controllable
     * parameter. Used when matching {@code AnnotationExpr#getNameAsString()} against
     * a method parameter.
     */
    public static final List<String> SPRING_ANNOTATIONS = List.of(
        "org.springframework.web.bind.annotation.RequestParam",
        "org.springframework.web.bind.annotation.RequestBody",
        "org.springframework.web.bind.annotation.PathVariable",
        "org.springframework.web.bind.annotation.RequestHeader",
        "org.springframework.web.bind.annotation.CookieValue"
    );

    /**
     * Short Spring annotation simple names for matching against the annotation
     * list in {@link MethodNode#annotations()}, which are stored as {@code @SimpleName}.
     */
    public static final Set<String> SPRING_ANNOTATION_SIMPLE_NAMES = Set.of(
        "RequestParam", "RequestBody", "PathVariable", "RequestHeader", "CookieValue"
    );

    /** Method names on HttpServletRequest that return user input. */
    public static final List<String> HTTP_REQUEST_METHODS = List.of(
        "getParameter", "getHeader", "getCookies", "getInputStream", "getReader",
        "getQueryString", "getRequestURI", "getPathInfo"
    );

    /**
     * Returns the {@link TaintSource} enum constant that corresponds to the given
     * Spring annotation simple name, or {@code null} if not a recognised taint annotation.
     */
    public static TaintSource fromSpringAnnotation(String simpleName) {
        return switch (simpleName) {
            case "RequestParam" -> SPRING_REQUEST_PARAM;
            case "RequestBody" -> SPRING_REQUEST_BODY;
            case "PathVariable" -> SPRING_PATH_VARIABLE;
            case "RequestHeader" -> SPRING_REQUEST_HEADER;
            case "CookieValue" -> SPRING_REQUEST_COOKIE;
            default -> null;
        };
    }

    /**
     * Returns the {@link TaintSource} enum constant that corresponds to the given
     * HttpServletRequest method name, or {@code null} if not a recognised taint method.
     */
    public static TaintSource fromServletMethod(String methodName) {
        return switch (methodName) {
            case "getParameter" -> HTTP_REQUEST_GET_PARAMETER;
            case "getHeader" -> HTTP_REQUEST_GET_HEADER;
            case "getCookies" -> HTTP_REQUEST_GET_COOKIE;
            case "getInputStream", "getReader" -> HTTP_REQUEST_GET_BODY;
            case "getQueryString" -> HTTP_REQUEST_GET_QUERY_STRING;
            case "getRequestURI" -> HTTP_REQUEST_GET_REQUEST_URI;
            case "getPathInfo" -> HTTP_REQUEST_GET_PATH_INFO;
            default -> null;
        };
    }
}
