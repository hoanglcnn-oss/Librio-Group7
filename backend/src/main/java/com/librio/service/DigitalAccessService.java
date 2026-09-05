package com.librio.service;

import com.librio.domain.Resource;
import com.librio.dto.DigitalAccessDto;
import com.librio.exception.BorrowErrorCode;
import com.librio.exception.BorrowFlowException;
import com.librio.repository.DigitalItemRepository;
import com.librio.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Cấp capability đọc nội dung số đã được bảo vệ bởi Spring Security.
 *
 * <p>Sprint 3 chỉ xác minh vertical slice phân quyền bằng PDF generate tại server.
 * Object storage, signed URL và DRM chưa nằm trong scope implementation hiện tại.
 */
@Service
@RequiredArgsConstructor
public class DigitalAccessService {
    private final ResourceRepository resourceRepository;
    private final DigitalItemRepository digitalItemRepository;

    @Transactional(readOnly = true)
    public DigitalAccessDto getCapability(Long resourceId) {
        requireDigitalResource(resourceId);
        String contentUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/resources/{id}/digital-content")
                .buildAndExpand(resourceId)
                .toUriString();
        return DigitalAccessDto.builder()
                .resourceId(resourceId)
                .canRead(true)
                .contentUrl(contentUrl)
                .temporaryUrl(false)
                .build();
    }

    @Transactional(readOnly = true)
    public byte[] getDemoPdf(Long resourceId) {
        Resource resource = requireDigitalResource(resourceId);
        return createPdf(resource.getTitle(), resource.getDescription());
    }

    private Resource requireDigitalResource(Long resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> notFound(BorrowErrorCode.RESOURCE_NOT_FOUND, "Resource not found"));
        if (!digitalItemRepository.existsByResourceId(resourceId)) {
            throw notFound(BorrowErrorCode.DIGITAL_CONTENT_NOT_FOUND, "Digital content not found");
        }
        return resource;
    }

    private byte[] createPdf(String title, String description) {
        // Demo Sprint 3: tạo PDF tối thiểu trong memory, chưa đại diện cho storage production-ready.
        String safeTitle = pdfText(title);
        String safeDescription = pdfText(description == null || description.isBlank()
                ? "Protected digital content preview"
                : description);
        String stream = "BT /F1 22 Tf 72 740 Td (" + safeTitle + ") Tj "
                + "0 -40 Td /F1 12 Tf (" + safeDescription + ") Tj "
                + "0 -32 Td (Librio authenticated digital access demo) Tj ET";
        List<String> objects = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                        + "/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
                "<< /Length " + stream.getBytes(StandardCharsets.US_ASCII).length + " >>\nstream\n"
                        + stream + "\nendstream"
        );

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeAscii(output, "%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int index = 0; index < objects.size(); index++) {
            offsets.add(output.size());
            writeAscii(output, (index + 1) + " 0 obj\n" + objects.get(index) + "\nendobj\n");
        }
        int xrefOffset = output.size();
        writeAscii(output, "xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n");
        offsets.forEach(offset -> writeAscii(output, String.format("%010d 00000 n \n", offset)));
        writeAscii(output, "trailer\n<< /Size " + (objects.size() + 1)
                + " /Root 1 0 R >>\nstartxref\n" + xrefOffset + "\n%%EOF");
        return output.toByteArray();
    }

    private String pdfText(String value) {
        return value.replaceAll("[^\\x20-\\x7E]", "?")
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private void writeAscii(ByteArrayOutputStream output, String value) {
        output.writeBytes(value.getBytes(StandardCharsets.US_ASCII));
    }

    private BorrowFlowException notFound(BorrowErrorCode code, String message) {
        return new BorrowFlowException(code.name(), HttpStatus.NOT_FOUND, message);
    }
}
