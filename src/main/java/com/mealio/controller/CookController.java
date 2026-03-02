package com.mealio.controller;

import com.mealio.dto.HeadcountResponse;
import com.mealio.port.in.HeadcountUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * FR-3: Cook's Dashboard headcount.
 * Depends on HeadcountUseCase interface.
 */
@RestController
@RequestMapping("/api/cook")
@RequiredArgsConstructor
public class CookController {

    private final HeadcountUseCase headcountUseCase; // DIP

    @GetMapping("/headcount")
    public ResponseEntity<HeadcountResponse> getHeadcount(@RequestParam UUID messId) {
        return ResponseEntity.ok(headcountUseCase.getHeadcount(messId));
    }
}
