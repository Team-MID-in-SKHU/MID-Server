package com.skhu.mid_skhu.app.dto.auth.responseDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentNoDuplicateInspectionResponseDto {

    private Boolean studentNo;
}
