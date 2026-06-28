package com.tyler.sentinel.dto;

import lombok.Data;

@Data
public class SecurityExplanation {
    private String riskSummary;
    private String potentialImpact;
    private String recommendedFix;
    private int riskRating;
}