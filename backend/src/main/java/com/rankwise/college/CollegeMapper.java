package com.rankwise.college;

import com.rankwise.college.dto.CollegeRequest;
import com.rankwise.college.dto.CollegeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CollegeMapper {

    CollegeResponse toResponse(College college);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    College toEntity(CollegeRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void update(@MappingTarget College college, CollegeRequest request);
}
