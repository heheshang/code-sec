package com.example.bench.svc;

import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletResponse;
@RestController
public class Service360 {

    public String convert_1(@RequestParam("input") String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        String html = "<div>" + arg + "</div>";
        System.out.println("XSS output: " + html);
        return "ok_360_1";
    }

    private String helper_1_1(String in) { return "h_1_1_" + in; }
    private String helper_1_2(String in) { return "h_1_2_" + in; }
    private String helper_1_3(String in) { return "h_1_3_" + in; }
    private String helper_1_4(String in) { return "h_1_4_" + in; }

    public String find_2(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_360_2";
    }

    private String helper_2_1(String in) { return "h_2_1_" + in; }
    private String helper_2_2(String in) { return "h_2_2_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    public String execute_3(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_360_3";
    }

    private String helper_3_1(String in) { return "h_3_1_" + in; }
    private String helper_3_2(String in) { return "h_3_2_" + in; }
    private String helper_3_3(String in) { return "h_3_3_" + in; }
    private String helper_3_4(String in) { return "h_3_4_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    public String update_4(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_360_4";
    }

    private String helper_4_1(String in) { return "h_4_1_" + in; }
    private String helper_4_2(String in) { return "h_4_2_" + in; }
    private String helper_4_3(String in) { return "h_4_3_" + in; }
    private String helper_4_4(String in) { return "h_4_4_" + in; }

    @GetMapping("/api/svc360/m5")
    public String aggregate_5(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_360_5";
    }

    private String helper_5_1(String in) { return "h_5_1_" + in; }
    private String helper_5_2(String in) { return "h_5_2_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    @GetMapping("/api/svc360/m6")
    public String getResult_6(@RequestParam("input") String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        new Service359().getResult_6(arg);
        return "ok_360_6";
    }

    private String helper_6_1(String in) { return "h_6_1_" + in; }
    private String helper_6_2(String in) { return "h_6_2_" + in; }
    private String helper_6_3(String in) { return "h_6_3_" + in; }
    private String helper_6_4(String in) { return "h_6_4_" + in; }

    public String retrieve_7(@RequestParam("input") String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_360_7";
    }

    private String helper_7_1(String in) { return "h_7_1_" + in; }
    private String helper_7_2(String in) { return "h_7_2_" + in; }
    private String helper_7_3(String in) { return "h_7_3_" + in; }
    private String helper_7_4(String in) { return "h_7_4_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    public String load_8(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_360_8";
    }

    private String helper_8_1(String in) { return "h_8_1_" + in; }
    private String helper_8_2(String in) { return "h_8_2_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    @GetMapping("/api/svc360/m9")
    public String update_9(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_360_9";
    }

    private String helper_9_1(String in) { return "h_9_1_" + in; }
    private String helper_9_2(String in) { return "h_9_2_" + in; }
    private String helper_9_3(String in) { return "h_9_3_" + in; }
    private String helper_9_4(String in) { return "h_9_4_" + in; }

    public String convert_10(@RequestParam("input") String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        new Service359().convert_10(arg);
        return "ok_360_10";
    }

    private String helper_10_1(String in) { return "h_10_1_" + in; }
    private String helper_10_2(String in) { return "h_10_2_" + in; }
    private String helper_10_3(String in) { return "h_10_3_" + in; }
    private String helper_10_4(String in) { return "h_10_4_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    public String execute_11(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        new Service359().execute_11(arg);
        return "ok_360_11";
    }

    private String helper_11_1(String in) { return "h_11_1_" + in; }
    private String helper_11_2(String in) { return "h_11_2_" + in; }

    public String getResult_12(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_360_12";
    }

    private String helper_12_1(String in) { return "h_12_1_" + in; }
    private String helper_12_2(String in) { return "h_12_2_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    public String aggregate_13(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_360_13";
    }

    private String helper_13_1(String in) { return "h_13_1_" + in; }
    private String helper_13_2(String in) { return "h_13_2_" + in; }
    private String helper_13_3(String in) { return "h_13_3_" + in; }
    private String helper_13_4(String in) { return "h_13_4_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    public String query_14(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        new Service359().query_14(arg);
        return "ok_360_14";
    }

    private String helper_14_1(String in) { return "h_14_1_" + in; }
    private String helper_14_2(String in) { return "h_14_2_" + in; }
    private String helper_14_3(String in) { return "h_14_3_" + in; }
    private String helper_14_4(String in) { return "h_14_4_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    public String serialize_15(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_360_15";
    }

    private String helper_15_1(String in) { return "h_15_1_" + in; }
    private String helper_15_2(String in) { return "h_15_2_" + in; }
    private String helper_15_3(String in) { return "h_15_3_" + in; }
    private String helper_15_4(String in) { return "h_15_4_" + in; }

    @GetMapping("/api/svc360/m16")
    public String convert_16(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_360_16";
    }

    private String helper_16_1(String in) { return "h_16_1_" + in; }
    private String helper_16_2(String in) { return "h_16_2_" + in; }
    private String helper_16_3(String in) { return "h_16_3_" + in; }
    private String helper_16_4(String in) { return "h_16_4_" + in; }

    public String load_17(@RequestParam("input") String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_360_17";
    }

    private String helper_17_1(String in) { return "h_17_1_" + in; }
    private String helper_17_2(String in) { return "h_17_2_" + in; }

    @GetMapping("/api/svc360/m18")
    public String create_18(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_360_18";
    }

    private String helper_18_1(String in) { return "h_18_1_" + in; }
    private String helper_18_2(String in) { return "h_18_2_" + in; }

    public String load_19(@RequestParam("input") String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_360_19";
    }

    private String helper_19_1(String in) { return "h_19_1_" + in; }
    private String helper_19_2(String in) { return "h_19_2_" + in; }
    private String helper_19_3(String in) { return "h_19_3_" + in; }
    private String helper_19_4(String in) { return "h_19_4_" + in; }

    @GetMapping("/api/svc360/m20")
    public String update_20(@RequestParam("input") String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        new Service359().update_20(arg);
        return "ok_360_20";
    }

    private String helper_20_1(String in) { return "h_20_1_" + in; }
    private String helper_20_2(String in) { return "h_20_2_" + in; }
    private String helper_20_3(String in) { return "h_20_3_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    public String create_21(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_360_21";
    }

    private String helper_21_1(String in) { return "h_21_1_" + in; }
    private String helper_21_2(String in) { return "h_21_2_" + in; }
    private String helper_21_3(String in) { return "h_21_3_" + in; }
    private String helper_21_4(String in) { return "h_21_4_" + in; }

    public String execute_22(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_360_22";
    }

    private String helper_22_1(String in) { return "h_22_1_" + in; }
    private String helper_22_2(String in) { return "h_22_2_" + in; }
    private String helper_22_3(String in) { return "h_22_3_" + in; }

    private String config_val_1 = "default_val_360";
    private String config_val_2 = "another_val_360";
    private int counter_360 = 0;
    private static final String VERSION_360 = "v1.0.360";
    private final Service359 dep_360 = new Service359();
}
