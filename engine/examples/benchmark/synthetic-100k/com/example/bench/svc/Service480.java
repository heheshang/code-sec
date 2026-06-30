package com.example.bench.svc;

import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletResponse;
@RestController
public class Service480 {

    public String compute_1(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        String html = "<div>" + arg + "</div>";
        System.out.println("XSS output: " + html);
        new Service479().compute_1(arg);
        return "ok_480_1";
    }

    private String helper_1_1(String in) { return "h_1_1_" + in; }
    private String helper_1_2(String in) { return "h_1_2_" + in; }
    private String helper_1_3(String in) { return "h_1_3_" + in; }

    public String retrieve_2(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_480_2";
    }

    private String helper_2_1(String in) { return "h_2_1_" + in; }
    private String helper_2_2(String in) { return "h_2_2_" + in; }
    private String helper_2_3(String in) { return "h_2_3_" + in; }
    private String helper_2_4(String in) { return "h_2_4_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    @GetMapping("/api/svc480/m3")
    public String getResult_3(@RequestParam("input") String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        new Service479().getResult_3(arg);
        return "ok_480_3";
    }

    private String helper_3_1(String in) { return "h_3_1_" + in; }
    private String helper_3_2(String in) { return "h_3_2_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    public String validate_4(@RequestParam("input") String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        new Service479().validate_4(arg);
        return "ok_480_4";
    }

    private String helper_4_1(String in) { return "h_4_1_" + in; }
    private String helper_4_2(String in) { return "h_4_2_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    public String create_5(@RequestParam("input") String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_480_5";
    }

    private String helper_5_1(String in) { return "h_5_1_" + in; }
    private String helper_5_2(String in) { return "h_5_2_" + in; }
    private String helper_5_3(String in) { return "h_5_3_" + in; }

    @GetMapping("/api/svc480/m6")
    public String handleRequest_6(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_480_6";
    }

    private String helper_6_1(String in) { return "h_6_1_" + in; }
    private String helper_6_2(String in) { return "h_6_2_" + in; }
    private String helper_6_3(String in) { return "h_6_3_" + in; }
    private String helper_6_4(String in) { return "h_6_4_" + in; }

    public String handleRequest_7(@RequestParam("input") String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_480_7";
    }

    private String helper_7_1(String in) { return "h_7_1_" + in; }
    private String helper_7_2(String in) { return "h_7_2_" + in; }
    private String helper_7_3(String in) { return "h_7_3_" + in; }
    private String helper_7_4(String in) { return "h_7_4_" + in; }

    @GetMapping("/api/svc480/m8")
    public String compute_8(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_480_8";
    }

    private String helper_8_1(String in) { return "h_8_1_" + in; }
    private String helper_8_2(String in) { return "h_8_2_" + in; }
    private String helper_8_3(String in) { return "h_8_3_" + in; }
    private String helper_8_4(String in) { return "h_8_4_" + in; }

    public String convert_9(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_480_9";
    }

    private String helper_9_1(String in) { return "h_9_1_" + in; }
    private String helper_9_2(String in) { return "h_9_2_" + in; }
    private String helper_9_3(String in) { return "h_9_3_" + in; }
    private String helper_9_4(String in) { return "h_9_4_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    public String find_10(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_480_10";
    }

    private String helper_10_1(String in) { return "h_10_1_" + in; }
    private String helper_10_2(String in) { return "h_10_2_" + in; }

    public String validate_11(@RequestParam("input") String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_480_11";
    }

    private String helper_11_1(String in) { return "h_11_1_" + in; }
    private String helper_11_2(String in) { return "h_11_2_" + in; }

    public String getResult_12(@RequestParam("input") String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_480_12";
    }

    private String helper_12_1(String in) { return "h_12_1_" + in; }
    private String helper_12_2(String in) { return "h_12_2_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    @GetMapping("/api/svc480/m13")
    public String update_13(@RequestParam("input") String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_480_13";
    }

    private String helper_13_1(String in) { return "h_13_1_" + in; }
    private String helper_13_2(String in) { return "h_13_2_" + in; }
    private String helper_13_3(String in) { return "h_13_3_" + in; }

    public String update_14(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        new Service479().update_14(arg);
        return "ok_480_14";
    }

    private String helper_14_1(String in) { return "h_14_1_" + in; }
    private String helper_14_2(String in) { return "h_14_2_" + in; }
    private String helper_14_3(String in) { return "h_14_3_" + in; }

    public String getResult_15(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        new Service479().getResult_15(arg);
        return "ok_480_15";
    }

    private String helper_15_1(String in) { return "h_15_1_" + in; }
    private String helper_15_2(String in) { return "h_15_2_" + in; }
    private String helper_15_3(String in) { return "h_15_3_" + in; }

    public String resolve_16(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_480_16";
    }

    private String helper_16_1(String in) { return "h_16_1_" + in; }
    private String helper_16_2(String in) { return "h_16_2_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    public String resolve_17(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        new Service479().resolve_17(arg);
        return "ok_480_17";
    }

    private String helper_17_1(String in) { return "h_17_1_" + in; }
    private String helper_17_2(String in) { return "h_17_2_" + in; }
    private String helper_17_3(String in) { return "h_17_3_" + in; }
    private String helper_17_4(String in) { return "h_17_4_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    @GetMapping("/api/svc480/m18")
    public String handleRequest_18(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_480_18";
    }

    private String helper_18_1(String in) { return "h_18_1_" + in; }
    private String helper_18_2(String in) { return "h_18_2_" + in; }
    private String helper_18_3(String in) { return "h_18_3_" + in; }
    private String helper_18_4(String in) { return "h_18_4_" + in; }

    public String getResult_19(@RequestParam("input") String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_480_19";
    }

    private String helper_19_1(String in) { return "h_19_1_" + in; }
    private String helper_19_2(String in) { return "h_19_2_" + in; }
    private String helper_19_3(String in) { return "h_19_3_" + in; }
    private String helper_19_4(String in) { return "h_19_4_" + in; }

    private String config_val_1 = "default_val_480";
    private String config_val_2 = "another_val_480";
    private int counter_480 = 0;
    private static final String VERSION_480 = "v1.0.480";
    private final Service479 dep_480 = new Service479();
}
