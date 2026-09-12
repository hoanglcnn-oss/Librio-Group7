package com.librio.service;

import com.librio.domain.DigitalItem;
import com.librio.domain.Resource;
import com.librio.dto.DigitalAccessDto;
import com.librio.dto.DigitalAccessLevel;
import com.librio.exception.BorrowErrorCode;
import com.librio.exception.BorrowFlowException;
import com.librio.repository.AccountRepository;
import com.librio.repository.DigitalItemRepository;
import com.librio.repository.MembershipSubscriptionRepository;
import com.librio.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DigitalAccessService {
    private final ResourceRepository resourceRepository;
    private final DigitalItemRepository digitalItemRepository;
    private final AccountRepository accountRepository;
    private final MembershipSubscriptionRepository membershipSubscriptionRepository;

    @Transactional(readOnly = true)
    public DigitalAccessDto getCapability(Long resourceId, String email) {
        DigitalItem digitalItem = requireDigitalItem(resourceId);

        boolean isActiveMember = false;
        if (email != null) {
            isActiveMember = accountRepository.findByEmail(email)
                    .flatMap(acc -> membershipSubscriptionRepository.findActiveByAccountId(acc.getId(), LocalDateTime.now()))
                    .isPresent();
        }

        DigitalAccessLevel accessLevel = null;
        String previewUrl = null;
        String contentUrl = null;

        if (isActiveMember && digitalItem.getFullContentKey() != null) {
            accessLevel = DigitalAccessLevel.FULL;
            contentUrl = buildUrl(resourceId, "digital-content");
        } else if (digitalItem.getPreviewContentKey() != null) {
            accessLevel = DigitalAccessLevel.PREVIEW;
        }

        if (digitalItem.getPreviewContentKey() != null) {
            previewUrl = buildUrl(resourceId, "digital-preview");
        }

        if (accessLevel == null) {
            throw notFound(BorrowErrorCode.DIGITAL_CONTENT_NOT_FOUND, "Digital content not found");
        }

        return DigitalAccessDto.builder()
                .resourceId(resourceId)
                .accessLevel(accessLevel)
                .previewUrl(previewUrl)
                .contentUrl(contentUrl)
                .build();
    }

    @Transactional(readOnly = true)
    public byte[] getDemoPdfPreview(Long resourceId) {
        DigitalItem digitalItem = requireDigitalItem(resourceId);
        if (digitalItem.getPreviewContentKey() == null) {
            throw notFound(BorrowErrorCode.DIGITAL_CONTENT_NOT_FOUND, "Preview not configured");
        }
        return createPdf("Preview: " + digitalItem.getResource().getTitle(), digitalItem.getResource().getDescription());
    }

    @Transactional(readOnly = true)
    public byte[] getDemoPdfContent(Long resourceId, String email) {
        DigitalItem digitalItem = requireDigitalItem(resourceId);

        boolean isActiveMember = accountRepository.findByEmail(email)
                .flatMap(acc -> membershipSubscriptionRepository.findActiveByAccountId(acc.getId(), LocalDateTime.now()))
                .isPresent();

        if (!isActiveMember) {
            throw new BorrowFlowException("DIGITAL_MEMBERSHIP_REQUIRED", HttpStatus.FORBIDDEN, "Active membership required for full content");
        }

        if (digitalItem.getFullContentKey() == null) {
            throw notFound(BorrowErrorCode.DIGITAL_CONTENT_NOT_FOUND, "Full content not configured");
        }

        return createPdf(digitalItem.getResource().getTitle(), digitalItem.getResource().getDescription());
    }

    private DigitalItem requireDigitalItem(Long resourceId) {
        if (!resourceRepository.existsById(resourceId)) {
            throw notFound(BorrowErrorCode.RESOURCE_NOT_FOUND, "Resource not found");
        }
        return digitalItemRepository.findByResourceId(resourceId)
                .orElseThrow(() -> notFound(BorrowErrorCode.DIGITAL_CONTENT_NOT_FOUND, "Digital content not found"));
    }

    private String buildUrl(Long resourceId, String endpoint) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/resources/" + resourceId + "/" + endpoint)
                .toUriString();
    }

    private byte[] createPdf(String title, String description) {
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
        for (byte b : value.getBytes(StandardCharsets.US_ASCII)) {
            output.write(b);
        }
    }

    private BorrowFlowException notFound(BorrowErrorCode code, String message) {
        return new BorrowFlowException(code.name(), HttpStatus.NOT_FOUND, message);
    }
}
