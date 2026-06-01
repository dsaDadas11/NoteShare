package com.example.noteshare.service;

import com.example.noteshare.common.BusinessException;
import com.example.noteshare.common.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileServiceTest {

    private static final long MAX_SIZE = 5L * 1024L * 1024L;

    @TempDir
    Path uploadDir;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileService();
        ReflectionTestUtils.setField(fileService, "uploadDir", uploadDir.toString());
        ReflectionTestUtils.setField(fileService, "maxSize", MAX_SIZE);
    }

    @Test
    void uploadStoresImageUnderDateDirectory() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.jpg",
                "image/jpeg",
                "image-content".getBytes()
        );

        String url = fileService.upload(file);

        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertThat(url).startsWith("/uploads/" + dateDir + "/").endsWith(".jpg");
        Path savedFile = uploadDir.resolve(dateDir).resolve(Path.of(url).getFileName().toString());
        assertThat(Files.readString(savedFile)).isEqualTo("image-content");
    }

    @Test
    void uploadAcceptsContentTypeWithParameters() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.jpg",
                "image/jpeg; charset=binary",
                "image-content".getBytes()
        );

        assertThat(fileService.upload(file)).endsWith(".jpg");
    }

    @Test
    void uploadRejectsTooLargeFileBeforeWriting() {
        ReflectionTestUtils.setField(fileService, "maxSize", 2L);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.jpg",
                "image/jpeg",
                "abc".getBytes()
        );

        assertThatThrownBy(() -> fileService.upload(file))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FILE_TOO_LARGE));
    }
}
