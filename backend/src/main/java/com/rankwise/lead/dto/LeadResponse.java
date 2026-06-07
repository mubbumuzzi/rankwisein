package com.rankwise.lead.dto;

import com.rankwise.lead.Lead;

import java.time.LocalDateTime;

public record LeadResponse(
        Long id,
        String name,
        String mobile,
        int rank,
        String category,
        String gender,
        String branch,
        LocalDateTime createdAt
) {
    public static LeadResponse from(Lead lead) {
        return new LeadResponse(
                lead.getId(),
                lead.getName(),
                lead.getMobile(),
                lead.getRank(),
                lead.getCategory(),
                lead.getGender(),
                lead.getBranch(),
                lead.getCreatedAt()
        );
    }
}
