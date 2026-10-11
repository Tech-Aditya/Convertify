package com.convertify.service;

import com.convertify.dto.ConversionResponse;
import com.convertify.dto.ConvertAllResponse;
import com.convertify.dto.QuantityDto;
import com.convertify.dto.UnitDto;
import com.convertify.model.ConversionResult;
import com.convertify.model.UnitCategory;
import com.convertify.model.UnitDefinition;
import com.convertify.util.NumberDisplayFormatter;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/** Maps full-precision conversion results to display-rounded DTOs. */
@Component
public class ConversionResponseMapper {

    private final NumberDisplayFormatter formatter;

    public ConversionResponseMapper(NumberDisplayFormatter formatter) {
        this.formatter = formatter;
    }

    public ConversionResponse toResponse(String categoryCode, ConversionResult result) {
        return new ConversionResponse(
                categoryCode,
                input(result.value(), result.from()),
                quantity(result.result(), result.to()),
                formatter.format(result.unitRate()));
    }

    public ConvertAllResponse toResponse(String categoryCode, BigDecimal value, UnitDefinition from,
                                         List<ConversionResult> results) {
        return new ConvertAllResponse(
                categoryCode,
                input(value, from),
                results.stream().map(result -> quantity(result.result(), result.to())).toList());
    }

    public QuantityDto quantity(BigDecimal value, UnitDefinition unit) {
        return new QuantityDto(formatter.format(value), UnitDto.from(unit));
    }

    public String number(BigDecimal value) {
        return formatter.format(value);
    }

    /** Echo the user's input exactly (only normalised), not display-rounded. */
    public QuantityDto input(BigDecimal value, UnitDefinition unit) {
        return new QuantityDto(NumberDisplayFormatter.normalise(value).toPlainString(), UnitDto.from(unit));
    }

    public List<UnitDto> units(UnitCategory category) {
        return category.units().stream().map(UnitDto::from).toList();
    }
}
