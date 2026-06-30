// Expected: EXPLOITABLE — 用户输入通过 3 层调用链到达 sink
package com.example.indirect;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entry point controller for the indirect exploitability test.
 * Receives user input via @RequestParam and delegates to IndirectService.
 */
@RestController
public class IndirectController {

    private final IndirectService indirectService = new IndirectService();

    @GetMapping("/api/user/find")
    public String findUser(@RequestParam("id") String userId) {
        return indirectService.process("search-data");
    }
}
