package com.rankwise.predict;

import com.rankwise.predict.dto.PredictRequest;
import com.rankwise.predict.dto.PredictResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Predict", description = "Student college recommendations")
public class PredictController {

    private final PredictService predictService;

    public PredictController(PredictService predictService) {
        this.predictService = predictService;
    }

    @PostMapping("/predict")
    @Operation(summary = "Get Dream, Target and Safe college recommendations")
    public PredictResponse predict(@Valid @RequestBody PredictRequest request,
                                   HttpServletRequest httpRequest) {
        return predictService.predict(request, httpRequest);
    }
}
