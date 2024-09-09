package com.dialodds.dialodds_api.controller;

import com.dialodds.dialodds_api.service.OddsPopulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Manual Update", description = "Manual database update operations")
public class ManualUpdateController {

    @Autowired
    private OddsPopulationService oddsPopulationService;

    @PostMapping("/update-database")
    @Operation(summary = "Manually update database", description = "Triggers a manual update of the database")
    public ResponseEntity<String> manuallyUpdateDatabase() {
        String result = oddsPopulationService.manuallyPopulateDatabase();
        return ResponseEntity.ok(result);
    }
}