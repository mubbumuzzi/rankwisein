package com.rankwise.branch;

import com.rankwise.branch.dto.BranchRequest;
import com.rankwise.branch.dto.BranchResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BranchMapper {

    BranchResponse toResponse(Branch branch);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Branch toEntity(BranchRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void update(@MappingTarget Branch branch, BranchRequest request);
}
