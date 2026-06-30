package com.example.bench.svc;

import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletResponse;
@RestController
public class Service420 {

    public String create_1(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        String html = "<div>" + arg + "</div>";
        System.out.println("XSS output: " + html);
        return "ok_420_1";
    }

    private String helper_1_1(String in) { return "h_1_1_" + in; }
    private String helper_1_2(String in) { return "h_1_2_" + in; }
    private String helper_1_3(String in) { return "h_1_3_" + in; }

    @GetMapping("/api/svc420/m2")
    public String resolve_2(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_420_2";
    }

    private String helper_2_1(String in) { return "h_2_1_" + in; }
    private String helper_2_2(String in) { return "h_2_2_" + in; }
    private String helper_2_3(String in) { return "h_2_3_" + in; }
    private String helper_2_4(String in) { return "h_2_4_" + in; }

    public String create_3(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        new Service419().create_3(arg);
        return "ok_420_3";
    }

    private String helper_3_1(String in) { return "h_3_1_" + in; }
    private String helper_3_2(String in) { return "h_3_2_" + in; }

    @GetMapping("/api/svc420/m4")
    public String handleRequest_4(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_420_4";
    }

    private String helper_4_1(String in) { return "h_4_1_" + in; }
    private String helper_4_2(String in) { return "h_4_2_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    public String fetch_5(@RequestParam("input") String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        new Service419().fetch_5(arg);
        return "ok_420_5";
    }

    private String helper_5_1(String in) { return "h_5_1_" + in; }
    private String helper_5_2(String in) { return "h_5_2_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    public String execute_6(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_420_6";
    }

    private String helper_6_1(String in) { return "h_6_1_" + in; }
    private String helper_6_2(String in) { return "h_6_2_" + in; }

    public String serialize_7(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        new Service419().serialize_7(arg);
        return "ok_420_7";
    }

    private String helper_7_1(String in) { return "h_7_1_" + in; }
    private String helper_7_2(String in) { return "h_7_2_" + in; }
    private String helper_7_3(String in) { return "h_7_3_" + in; }
    private String helper_7_4(String in) { return "h_7_4_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    public String convert_8(@RequestParam("input") String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        new Service419().convert_8(arg);
        return "ok_420_8";
    }

    private String helper_8_1(String in) { return "h_8_1_" + in; }
    private String helper_8_2(String in) { return "h_8_2_" + in; }
    private String helper_8_3(String in) { return "h_8_3_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    public String query_9(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_420_9";
    }

    private String helper_9_1(String in) { return "h_9_1_" + in; }
    private String helper_9_2(String in) { return "h_9_2_" + in; }

    public String create_10(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        new Service419().create_10(arg);
        return "ok_420_10";
    }

    private String helper_10_1(String in) { return "h_10_1_" + in; }
    private String helper_10_2(String in) { return "h_10_2_" + in; }
    private String helper_10_3(String in) { return "h_10_3_" + in; }
    private String helper_10_4(String in) { return "h_10_4_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    public String resolve_11(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        new Service419().resolve_11(arg);
        return "ok_420_11";
    }

    private String helper_11_1(String in) { return "h_11_1_" + in; }
    private String helper_11_2(String in) { return "h_11_2_" + in; }
    private String helper_11_3(String in) { return "h_11_3_" + in; }

    @GetMapping("/api/svc420/m12")
    public String execute_12(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_420_12";
    }

    private String helper_12_1(String in) { return "h_12_1_" + in; }
    private String helper_12_2(String in) { return "h_12_2_" + in; }
    private String helper_12_3(String in) { return "h_12_3_" + in; }
    private String helper_12_4(String in) { return "h_12_4_" + in; }

    @GetMapping("/api/svc420/m13")
    public String getResult_13(@RequestParam("input") String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_420_13";
    }

    private String helper_13_1(String in) { return "h_13_1_" + in; }
    private String helper_13_2(String in) { return "h_13_2_" + in; }

    public String getResult_14(@RequestParam("input") String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_420_14";
    }

    private String helper_14_1(String in) { return "h_14_1_" + in; }
    private String helper_14_2(String in) { return "h_14_2_" + in; }
    private String helper_14_3(String in) { return "h_14_3_" + in; }
    private String helper_14_4(String in) { return "h_14_4_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    public String convert_15(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        new Service419().convert_15(arg);
        return "ok_420_15";
    }

    private String helper_15_1(String in) { return "h_15_1_" + in; }
    private String helper_15_2(String in) { return "h_15_2_" + in; }
    private String helper_15_3(String in) { return "h_15_3_" + in; }
    private String helper_15_4(String in) { return "h_15_4_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    public String serialize_16(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        new Service419().serialize_16(arg);
        return "ok_420_16";
    }

    private String helper_16_1(String in) { return "h_16_1_" + in; }
    private String helper_16_2(String in) { return "h_16_2_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    public String update_17(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        new Service419().update_17(arg);
        return "ok_420_17";
    }

    private String helper_17_1(String in) { return "h_17_1_" + in; }
    private String helper_17_2(String in) { return "h_17_2_" + in; }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    public String retrieve_18(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        new Service419().retrieve_18(arg);
        return "ok_420_18";
    }

    private String helper_18_1(String in) { return "h_18_1_" + in; }
    private String helper_18_2(String in) { return "h_18_2_" + in; }

    @GetMapping("/api/svc420/m19")
    public String find_19(String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        return "ok_420_19";
    }

    private String helper_19_1(String in) { return "h_19_1_" + in; }
    private String helper_19_2(String in) { return "h_19_2_" + in; }
    private String helper_19_3(String in) { return "h_19_3_" + in; }
    private String helper_19_4(String in) { return "h_19_4_" + in; }

    public String create_20(@RequestParam("input") String arg) {
        if (arg == null || arg.isEmpty()) { return "empty"; }
        System.out.println("Processing: " + arg);
        new Service419().create_20(arg);
        return "ok_420_20";
    }

    private String helper_20_1(String in) { return "h_20_1_" + in; }
    private String helper_20_2(String in) { return "h_20_2_" + in; }

    private String config_val_1 = "default_val_420";
    private String config_val_2 = "another_val_420";
    private int counter_420 = 0;
    private static final String VERSION_420 = "v1.0.420";
    private final Service419 dep_420 = new Service419();
}
