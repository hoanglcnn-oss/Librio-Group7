package com.librio.service;

import com.librio.domain.Account;
import com.librio.domain.AccountRole;
import com.librio.domain.AccountStatus;
import com.librio.domain.Borrowing;
import com.librio.domain.PhysicalItemStatus;
import com.librio.dto.LibrarianBorrowingDto;
import com.librio.dto.ManagedResourceDto;
import com.librio.dto.ReaderBorrowRequestItemDto;
import com.librio.dto.ResourceAdminRequestDto;
import com.librio.exception.BorrowFlowException;
import com.librio.repository.AccountRepository;
import com.librio.repository.BorrowRequestRepository;
import com.librio.repository.BorrowingRepository;
import com.librio.repository.PhysicalItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Sprint3ServiceTest {
    @Autowired private BorrowService borrowService;
    @Autowired private ResourceAdminService resourceAdminService;
    @Autowired private DigitalAccessService digitalAccessService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private BorrowRequestRepository borrowRequestRepository;
    @Autowired private BorrowingRepository borrowingRepository;
    @Autowired private PhysicalItemRepository physicalItemRepository;

    @Test
    void derivesOverdueAndAtomicallyReturnsExactItem() {
        Account reader = account("s3-reader@test.local", AccountRole.READER);
        Account librarian = account("s3-librarian@test.local", AccountRole.LIBRARIAN);
        ReaderBorrowRequestItemDto request = borrowService.createRequest(reader.getId(), 4L);
        Long itemId = borrowRequestRepository.findById(request.getId()).orElseThrow().getPhysicalItem().getId();
        borrowService.prepare(librarian.getId(), request.getId(), itemId);
        LibrarianBorrowingDto fulfilled = borrowService.fulfil(librarian.getId(), request.getId(), itemId);

        Borrowing borrowing = borrowingRepository.findById(fulfilled.getId()).orElseThrow();
        borrowing.setDueAt(LocalDateTime.now().minusDays(1));
        borrowingRepository.flush();

        var readerBorrowings = borrowService.getReaderBorrowings(reader.getId()).getActiveBorrowings();
        assertThat(readerBorrowings).hasSize(1);
        assertThat(readerBorrowings.get(0).isOverdue()).isTrue();
        var librarianBorrowings = borrowService.getActiveBorrowingsForLibrarian(librarian.getId()).getActiveBorrowings();
        assertThat(librarianBorrowings).hasSize(1);
        assertThat(librarianBorrowings.get(0).isOverdue()).isTrue();

        LibrarianBorrowingDto returned = borrowService.returnBorrowing(librarian.getId(), borrowing.getId());
        assertThat(returned.getReturnedAt()).isNotNull();
        assertThat(returned.isOverdue()).isFalse();
        assertThat(physicalItemRepository.findById(itemId).orElseThrow().getStatus())
                .isEqualTo(PhysicalItemStatus.AVAILABLE);
        assertThat(borrowService.getReaderBorrowings(reader.getId()).getActiveBorrowings()).isEmpty();

        assertThatThrownBy(() -> borrowService.returnBorrowing(librarian.getId(), borrowing.getId()))
                .isInstanceOf(BorrowFlowException.class)
                .satisfies(error -> assertThat(((BorrowFlowException) error).getCode())
                        .isEqualTo("BORROWING_ALREADY_RETURNED"));
    }

    @Test
    void createsAndUpdatesManagedResourceAndAccessRecords() {
        ResourceAdminRequestDto create = resourceRequest("Backend Design", 2, true);
        ManagedResourceDto created = resourceAdminService.create(create);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getCategory()).isEqualTo("Technology");
        assertThat(created.getAccessTypes()).containsExactly("PHYSICAL", "DIGITAL");
        assertThat(created.getPhysical().getTotalCopies()).isEqualTo(2);

        ResourceAdminRequestDto update = resourceRequest("Backend Design, Second Edition", 1, false);
        ManagedResourceDto updated = resourceAdminService.update(created.getId(), update);
        assertThat(updated.getTitle()).isEqualTo("Backend Design, Second Edition");
        assertThat(updated.getAccessTypes()).containsExactly("PHYSICAL");
        assertThat(updated.getPhysical().getTotalCopies()).isEqualTo(1);
    }

    @Test
    void returnsProtectedDemoPdfOnlyForDigitalResource() {
        assertThat(digitalAccessService.getCapability(1L).isCanRead()).isTrue();
        assertThat(new String(digitalAccessService.getDemoPdf(1L)))
                .startsWith("%PDF-1.4")
                .endsWith("%%EOF");

        assertThatThrownBy(() -> digitalAccessService.getCapability(2L))
                .isInstanceOf(BorrowFlowException.class)
                .satisfies(error -> assertThat(((BorrowFlowException) error).getCode())
                        .isEqualTo("DIGITAL_CONTENT_NOT_FOUND"));
    }

    private ResourceAdminRequestDto resourceRequest(String title, long copies, boolean digital) {
        ResourceAdminRequestDto request = new ResourceAdminRequestDto();
        request.setTitle(title);
        request.setAuthors(List.of("Test Author"));
        request.setDescription("Test description");
        request.setCategory("Technology");
        request.setAccessTypes(digital ? List.of("PHYSICAL", "DIGITAL") : List.of("PHYSICAL"));
        ResourceAdminRequestDto.PhysicalInput physical = new ResourceAdminRequestDto.PhysicalInput();
        physical.setTotalCopies(copies);
        request.setPhysical(physical);
        return request;
    }

    private Account account(String email, AccountRole role) {
        LocalDateTime now = LocalDateTime.now();
        return accountRepository.save(Account.builder()
                .email(email)
                .passwordHash("not-used")
                .displayName(email)
                .role(role)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }
}
