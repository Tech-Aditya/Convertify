package com.convertify.service;

import com.convertify.config.CurrencyProperties;
import com.convertify.converter.UnitDefinitionFactory;
import com.convertify.dto.CurrencyConversionRequest;
import com.convertify.dto.CurrencyConversionResponse;
import com.convertify.dto.CurrencyDto;
import com.convertify.dto.CurrencyListResponse;
import com.convertify.dto.RateInfoDto;
import com.convertify.exception.ErrorCode;
import com.convertify.exception.InvalidInputException;
import com.convertify.model.ExchangeRateSnapshot;
import com.convertify.util.InputLimits;
import com.convertify.util.NumberDisplayFormatter;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Currency conversion using {@link BigDecimal} throughout. Any pair is derived from a single
 * base-currency snapshot: rate(from → to) = rate(base → to) / rate(base → from).
 */
@Service
public class CurrencyService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyService.class);
    private static final int DEFAULT_FRACTION_DIGITS = 2;

    private final ExchangeRateCache rateCache;
    private final CurrencyProperties properties;
    private final InputLimits inputLimits;
    private final MathContext mathContext;
    private final List<String> featured;

    public CurrencyService(ExchangeRateCache rateCache, CurrencyProperties properties, InputLimits inputLimits,
                           UnitDefinitionFactory factory) {
        this.rateCache = rateCache;
        this.properties = properties;
        this.inputLimits = inputLimits;
        this.mathContext = factory.mathContext();
        this.featured = properties.featured().stream().map(code -> code.toUpperCase(Locale.ROOT)).toList();
    }

    public CurrencyListResponse currencies() {
        log.debug("Currency list requested");
        ExchangeRateCache.Rates rates = rateCache.get();
        Comparator<CurrencyDto> featuredFirst = Comparator
                .comparingInt((CurrencyDto c) -> c.featured() ? featured.indexOf(c.code()) : Integer.MAX_VALUE)
                .thenComparing(CurrencyDto::code);
        List<CurrencyDto> currencies = rates.snapshot().rates().keySet().stream()
                .map(this::describe)
                .sorted(featuredFirst)
                .toList();
        return new CurrencyListResponse(properties.defaultFrom().toUpperCase(Locale.ROOT),
                properties.defaultTo().toUpperCase(Locale.ROOT), currencies, rateInfo(rates));
    }

    public CurrencyConversionResponse convert(CurrencyConversionRequest request) {
        inputLimits.requireNonNegative(request.amount(), "Amount");
        String from = request.from().toUpperCase(Locale.ROOT);
        String to = request.to().toUpperCase(Locale.ROOT);
        long start = System.nanoTime();
        log.info("Currency conversion requested: {} -> {}", from, to);

        ExchangeRateCache.Rates rates = rateCache.get();
        BigDecimal fromRate = rateFor(rates.snapshot(), from);
        BigDecimal toRate = rateFor(rates.snapshot(), to);

        BigDecimal rate = toRate.divide(fromRate, mathContext);
        CurrencyDto fromCurrency = describe(from);
        CurrencyDto toCurrency = describe(to);
        BigDecimal result = request.amount().multiply(rate, mathContext)
                .setScale(toCurrency.fractionDigits(), properties.roundingMode());

        log.info("Currency conversion completed: {} -> {} in {} ms{}", from, to, (System.nanoTime() - start) / 1_000_000,
                rates.stale() ? " (using stale cached rates)" : "");
        return new CurrencyConversionResponse(
                NumberDisplayFormatter.normalise(request.amount()).toPlainString(),
                fromCurrency,
                toCurrency,
                result.toPlainString(),
                scaledRate(rate),
                scaledRate(fromRate.divide(toRate, mathContext)),
                rateInfo(rates));
    }

    private BigDecimal rateFor(ExchangeRateSnapshot snapshot, String code) {
        BigDecimal rate = snapshot.rates().get(code);
        if (rate == null) {
            throw new InvalidInputException(ErrorCode.INVALID_CURRENCY,
                    "Currency '%s' is not supported".formatted(code));
        }
        return rate;
    }

    /** Rates keep {@code rateScale} decimals, but tiny rates keep enough significant digits to be meaningful. */
    private String scaledRate(BigDecimal rate) {
        BigDecimal scaled = rate.compareTo(BigDecimal.ONE) >= 0
                ? rate.setScale(properties.rateScale(), properties.roundingMode())
                : rate.round(new MathContext(properties.rateScale(), properties.roundingMode()));
        return NumberDisplayFormatter.normalise(scaled).toPlainString();
    }

    private CurrencyDto describe(String code) {
        String name = code;
        int fractionDigits = DEFAULT_FRACTION_DIGITS;
        try {
            Currency currency = Currency.getInstance(code);
            name = currency.getDisplayName(Locale.ENGLISH);
            if (currency.getDefaultFractionDigits() >= 0) {
                fractionDigits = currency.getDefaultFractionDigits();
            }
        } catch (IllegalArgumentException ignored) {
            // Provider supports a code the JDK does not know (e.g. regional pound variants): use defaults.
        }
        return new CurrencyDto(code, name, fractionDigits, featured.contains(code));
    }

    private static RateInfoDto rateInfo(ExchangeRateCache.Rates rates) {
        ExchangeRateSnapshot snapshot = rates.snapshot();
        return new RateInfoDto(snapshot.source(), snapshot.lastUpdated(), snapshot.nextUpdate(), snapshot.fetchedAt(),
                rates.stale());
    }
}
