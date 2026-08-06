package com.zhiplatform.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void illegalArgument_mapsTo400WithMessage() {
        Result<Void> result = handler.handleIllegalArgument(new IllegalArgumentException("班级名称不能为空"));
        assertEquals(400, result.getCode());
        assertEquals("班级名称不能为空", result.getMessage());
    }

    @Test
    void illegalArgument_nullMessage_fallback() {
        Result<Void> result = handler.handleIllegalArgument(new IllegalArgumentException());
        assertEquals(400, result.getCode());
        assertEquals("参数不合法", result.getMessage());
    }

    @Test
    void otherException_mapsTo500() {
        Result<Void> result = handler.handleException(new RuntimeException("boom"));
        assertEquals(500, result.getCode());
        assertEquals("服务器内部错误：boom", result.getMessage());
    }
}