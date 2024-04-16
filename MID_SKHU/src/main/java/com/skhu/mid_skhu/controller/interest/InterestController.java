package com.skhu.mid_skhu.controller.interest;

import com.skhu.mid_skhu.dto.interest.requestDto.InterestRegisterRequestDto;
import com.skhu.mid_skhu.dto.interest.responseDto.InterestRegisterResponseDto;
import com.skhu.mid_skhu.global.common.dto.ApiResponse;
import com.skhu.mid_skhu.service.interest.InterestRegisterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@AllArgsConstructor
@Tag(name = "관심사")
@RequestMapping("/api/v1/interest")
public class InterestController {

    private final InterestRegisterService registerService;

    @PostMapping("/register")
    @Operation(summary = "관심사 등록", description = "관심사를 List로 받고 받은 관심사 정보를 해당 사용자 정보에 저장합니다.")
    public ResponseEntity<ApiResponse<InterestRegisterResponseDto>> interestRegister(@RequestBody InterestRegisterRequestDto requestDto,
                                                                                     Principal principal) {
        ApiResponse<InterestRegisterResponseDto> apiResponse = registerService.interestRegister(requestDto, principal);

        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }
}
