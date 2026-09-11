package com.librio.controller;

import com.librio.dto.MembershipPlanDto;
import com.librio.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/membership/plans")
@RequiredArgsConstructor
public class MembershipPlanController {

    private final MembershipService membershipService;

    @GetMapping
    public ResponseEntity<List<MembershipPlanDto>> getActivePlans() {
        return ResponseEntity.ok(membershipService.getActivePlans());
    }
}
