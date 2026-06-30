package com.codesec.sample;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Sample code demonstrating XSS vulnerability.
 * Finding expected at line 14 (request parameter written to response without encoding).
 */
public class WebController {

    // VULNERABLE: User input written to response without encoding - line 14
    public void search(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String query = request.getParameter("q");
        response.getWriter().println("<h1>Results for: " + query + "</h1>");
    }

    // SAFE: Output is static content, no user input
    public void status(HttpServletResponse response) throws IOException {
        response.getWriter().println("<h1>Server is running</h1>");
    }
}
