package com.convertify;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.convertify.model.ExchangeRateSnapshot;
import com.convertify.provider.currency.ExchangeRateProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Boots the whole application (with a stub exchange-rate provider) and exercises the public API. */
@SpringBootTest(properties = "convertify.currency.provider.type=stub")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @TestConfiguration
    static class StubRates {

        @Bean
        ExchangeRateProvider stubExchangeRateProvider() {
            return new ExchangeRateProvider() {
                @Override
                public ExchangeRateSnapshot fetchLatestRates(String baseCurrency) {
                    return new ExchangeRateSnapshot(baseCurrency,
                            Map.of("USD", BigDecimal.ONE, "INR", new BigDecimal("88"), "JPY", new BigDecimal("150"),
                                    "EUR", new BigDecimal("0.8")),
                            null, null, Instant.now(), name());
                }

                @Override
                public String name() {
                    return "Stub";
                }
            };
        }
    }

    private ResultActions postJson(String path, String json) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void healthIsUp() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void convertsLengthWithoutFloatingPointNoise() throws Exception {
        postJson("/api/v1/units/convert", """
                {"category":"LENGTH","value":180,"from":"cm","to":"m"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.value").value("1.8"))
                .andExpect(jsonPath("$.unitRate").value("0.01"));
    }

    @Test
    void convertsTemperatureWithOffsets() throws Exception {
        postJson("/api/v1/units/convert", """
                {"category":"TEMPERATURE","value":100,"from":"C","to":"F"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.value").value("212"));
    }

    @Test
    void rejectsTemperatureBelowAbsoluteZero() throws Exception {
        postJson("/api/v1/units/convert", """
                {"category":"TEMPERATURE","value":-300,"from":"C","to":"K"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
    }

    @Test
    void rejectsUnknownUnit() throws Exception {
        postJson("/api/v1/units/convert", """
                {"category":"LENGTH","value":1,"from":"cm","to":"parsec"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("UNSUPPORTED_UNIT"));
    }

    @Test
    void rejectsValuesWithExcessivePrecisionInsteadOfExpandingThem() throws Exception {
        postJson("/api/v1/units/convert", """
                {"category":"LENGTH","value":1e-999999999,"from":"cm","to":"m"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
        postJson("/api/v1/time-distance/calculate", """
                {"solveFor":"TIME","distance":1,"distanceUnit":"km","speed":1e-5000000,"speedUnit":"kmh"}""")
                .andExpect(status().isBadRequest());
        postJson("/api/v1/currency/convert", """
                {"amount":1e-999999999,"from":"USD","to":"INR"}""")
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        postJson("/api/v1/units/convert", "{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"))
                .andExpect(header().exists("X-Request-ID"));
    }

    @Test
    void convertsCurrencyAndRoundsToMinorUnits() throws Exception {
        postJson("/api/v1/currency/convert", """
                {"amount":100,"from":"usd","to":"INR"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("8800.00"))
                .andExpect(jsonPath("$.rates.stale").value(false));
        postJson("/api/v1/currency/convert", """
                {"amount":10,"from":"EUR","to":"JPY"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("1875"));
    }

    @Test
    void rejectsUnsupportedCurrency() throws Exception {
        postJson("/api/v1/currency/convert", """
                {"amount":1,"from":"USD","to":"XYZ"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_CURRENCY"));
    }

    @Test
    void convertsRegionalLandUnits() throws Exception {
        postJson("/api/v1/area/convert", """
                {"region":"MAHARASHTRA","value":1,"from":"ac","to":"guntha"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.value").value("40"));
    }

    @Test
    void solvesForTravelTime() throws Exception {
        postJson("/api/v1/time-distance/calculate", """
                {"solveFor":"TIME","distance":500,"distanceUnit":"km","speed":80,"speedUnit":"kmh"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.value").value("6.25"))
                .andExpect(jsonPath("$.readable").value("6 hours 15 minutes"));
    }

    @Test
    void rejectsZeroSpeedWhenSolvingForTime() throws Exception {
        postJson("/api/v1/time-distance/calculate", """
                {"solveFor":"TIME","distance":500,"distanceUnit":"km","speed":0,"speedUnit":"kmh"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Speed must be greater than zero"));
    }

    @Test
    void shiftsDatesByBusinessDays() throws Exception {
        postJson("/api/v1/date/shift", """
                {"date":"2026-09-15","operation":"ADD","amount":20,"unit":"BUSINESS_DAYS","holidayCalendar":"IN_NATIONAL"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultDate").value("2026-10-14"))
                .andExpect(jsonPath("$.resultDayOfWeek").value("Wednesday"));
    }

    @Test
    void calculatesDateDifference() throws Exception {
        postJson("/api/v1/date/difference", """
                {"startDate":"2026-08-15","endDate":"2026-11-03"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDays").value(80))
                .andExpect(jsonPath("$.weeks").value(11))
                .andExpect(jsonPath("$.remainingDays").value(3))
                .andExpect(jsonPath("$.months").value(2))
                .andExpect(jsonPath("$.days").value(19));
    }
}
