package com.rankwise.college;

import com.rankwise.common.audit.Auditable;
import com.rankwise.common.exception.DuplicateResourceException;
import com.rankwise.common.exception.ResourceNotFoundException;
import com.rankwise.college.dto.CollegeRequest;
import com.rankwise.college.dto.CollegeResponse;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class CollegeService {

    private final CollegeRepository repository;
    private final CollegeMapper mapper;

    public CollegeService(CollegeRepository repository, CollegeMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<CollegeResponse> search(String q, String district, Pageable pageable) {
        return repository.findAll(filter(q, district), pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<CollegeResponse> findAll(String q, String district) {
        return repository.findAll(filter(q, district)).stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CollegeResponse get(Long id) {
        return mapper.toResponse(find(id));
    }

    @Auditable("create-college")
    public CollegeResponse create(CollegeRequest request) {
        if (repository.existsByCode(request.code())) {
            throw new DuplicateResourceException("College code already exists: " + request.code());
        }
        College saved = repository.save(mapper.toEntity(request));
        return mapper.toResponse(saved);
    }

    @Auditable("update-college")
    public CollegeResponse update(Long id, CollegeRequest request) {
        College college = find(id);
        if (!college.getCode().equals(request.code()) && repository.existsByCode(request.code())) {
            throw new DuplicateResourceException("College code already exists: " + request.code());
        }
        mapper.update(college, request);
        college.setAutonomous(request.autonomous());
        return mapper.toResponse(repository.save(college));
    }

    @Auditable("delete-college")
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("College", id);
        }
        repository.deleteById(id);
    }

    private College find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("College", id));
    }

    private Specification<College> filter(String q, String district) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("code")), like)
                ));
            }
            if (district != null && !district.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("district")), district.toLowerCase()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
