package com.rankwise.branch;

import com.rankwise.branch.dto.BranchRequest;
import com.rankwise.branch.dto.BranchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/branches")
@Tag(name = "Branches", description = "Branch management")
public class BranchController {

    private final BranchService service;

    public BranchController(BranchService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all branches")
    public List<BranchResponse> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a branch")
    public BranchResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @Operation(summary = "Create a branch")
    public BranchResponse create(@Valid @RequestBody BranchRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a branch")
    public BranchResponse update(@PathVariable Long id, @Valid @RequestBody BranchRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a branch")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
