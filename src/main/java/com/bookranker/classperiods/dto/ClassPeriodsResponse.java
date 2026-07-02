package com.bookranker.classperiods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Response containing class periods owned by the authenticated teacher")
public record ClassPeriodsResponse(
    @Schema(description = "Class periods owned by the authenticated teacher")
    List<ClassPeriodSummaryResponse> classes
) {
}
