package com.hiresense.dto.interview;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnswerSubmitRequest(
        @NotBlank(message = "Answer text is required")
        @Size(min = 5, max = 10000, message = "Answer must be between 5 and 10000 characters")
        String text,

        Integer timeTakenSeconds
) {}