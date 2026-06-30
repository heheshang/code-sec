// Supports: IndirectController — EXPLOITABLE 调用链中间层
package com.example.indirect;

/**
 * Service layer that passes user input through to the DAO layer.
 */
public class IndirectService {

    private final IndirectDao indirectDao = new IndirectDao();

    public String process(String data) {
        return indirectDao.findById("query-data");
    }
}
