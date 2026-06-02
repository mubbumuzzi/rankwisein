package com.rankwise.branch;

import com.rankwise.branch.dto.BranchRequest;
import com.rankwise.branch.dto.BranchResponse;
import com.rankwise.common.audit.Auditable;
import com.rankwise.common.exception.DuplicateResourceException;
import com.rankwise.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BranchService {

    private final BranchRepository repository;
    private final BranchMapper mapper;

    public BranchService(BranchRepository repository, BranchMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> findAll() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BranchResponse get(Long id) {
        return mapper.toResponse(find(id));
    }

    @Auditable("create-branch")
    public BranchResponse create(BranchRequest request) {
        if (repository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Branch code already exists: " + request.code());
        }
        return mapper.toResponse(repository.save(mapper.toEntity(request)));
    }

    @Auditable("update-branch")
    public BranchResponse update(Long id, BranchRequest request) {
        Branch branch = find(id);
        if (!branch.getCode().equals(request.code()) && repository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Branch code already exists: " + request.code());
        }
        mapper.update(branch, request);
        return mapper.toResponse(repository.save(branch));
    }

    @Auditable("delete-branch")
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Branch", id);
        }
        repository.deleteById(id);
    }

    private Branch find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Branch", id));
    }
}
