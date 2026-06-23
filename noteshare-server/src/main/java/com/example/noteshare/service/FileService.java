package com.example.noteshare.service;

import com.example.noteshare.common.BusinessException;
import com.example.noteshare.common.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传服务
 */
@Service
public class FileService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4", "video/webm", "video/3gpp"
    );

    @Value("${file.max-size}")
    private long maxSize;

    @Value("${file.video-max-size}")
    private long videoMaxSize;

    /**
     * 上传单张图片，返回可访问 URL
     */
    public String upload(MultipartFile file) {
        // 1. 非空校验
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }

        // 2. 类型校验
        String contentType = normalizeContentType(file.getContentType());
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }

        // 3. 大小校验
        if (file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        // 4. 生成唯一文件名
        String ext = getExtension(contentType);
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;

        // 5. 按日期分目录
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Path dirPath = Path.of(uploadDir).toAbsolutePath().normalize().resolve(dateDir);
        Path targetPath = dirPath.resolve(filename).normalize();
        try {
            Files.createDirectories(dirPath);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        // 6. 返回可访问 URL
        return "/uploads/" + dateDir + "/" + filename;
    }

    /**
     * 上传视频文件，返回可访问 URL
     */
    public String uploadVideo(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }

        String contentType = normalizeContentType(file.getContentType());
        if (!ALLOWED_VIDEO_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.VIDEO_TYPE_NOT_ALLOWED);
        }

        if (file.getSize() > videoMaxSize) {
            throw new BusinessException(ErrorCode.VIDEO_TOO_LARGE);
        }

        String ext = getVideoExtension(contentType);
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;

        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Path dirPath = Path.of(uploadDir).toAbsolutePath().normalize().resolve(dateDir);
        Path targetPath = dirPath.resolve(filename).normalize();
        try {
            Files.createDirectories(dirPath);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        return "/uploads/" + dateDir + "/" + filename;
    }

    private String getExtension(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    private String getVideoExtension(String contentType) {
        return switch (contentType) {
            case "video/webm" -> ".webm";
            case "video/3gpp" -> ".3gp";
            default -> ".mp4";
        };
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        return contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }
}
