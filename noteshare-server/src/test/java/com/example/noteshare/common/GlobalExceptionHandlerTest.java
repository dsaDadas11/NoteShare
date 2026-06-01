package com.example.noteshare.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void maxUploadSizeExceededReturnsFileTooLargeBusinessResponse() {
        var response = handler.handleMaxUploadSize(new MaxUploadSizeExceededException(5L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.FILE_TOO_LARGE.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.FILE_TOO_LARGE.getMessage());
    }
}
