package com.example.demo.service;

import com.example.demo.model.CurrencyLog;
import com.example.demo.repository.CurrencyRepository;
import com.example.demo.repository.ApiKeyRepository;
import com.example.demo.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final CurrencyRepository currencyRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final RestTemplate restTemplate;

    private static final String RATE_API_BASE = "https://open.er-api.com/v6/latest/";

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchLiveRates(String baseCurrency) {
        Map<String, Object> response = restTemplate.getForObject(
                RATE_API_BASE + baseCurrency, Map.class);

        if (response == null || !"success".equals(response.get("result"))) {
            throw new IllegalStateException("Unable to fetch live exchange rates for " + baseCurrency);
        }
        return (Map<String, Object>) response.get("rates");
    }

    public CurrencyLog convertAndSave(double amount, String fromCurrency, String toCurrency) {
        String from = fromCurrency.trim().toUpperCase();
        String to = toCurrency.trim().toUpperCase();

        Map<String, Object> rates = fetchLiveRates(from);

        if (!rates.containsKey(to)) {
            throw new IllegalArgumentException("Unsupported currency code: " + to);
        }

        double rate = Double.parseDouble(rates.get(to).toString());
        double result = amount * rate;

        CurrencyLog log = new CurrencyLog();
        log.setInputAmount(amount);
        log.setInputCurrency(from);
        log.setOutputAmount(result);
        log.setOutputCurrency(to);
        log.setTimestamp(java.time.LocalDateTime.now().toString());

        return currencyRepository.save(log);
    }

    public List<CurrencyLog> getAllLogs() {
        return currencyRepository.findAll();
    }

    public String getTransactionWarning(double amount, String currency) {
        String cleanCurrency = currency.trim().toUpperCase();
        Map<String, Object> rates = fetchLiveRates("USD");

        if (!rates.containsKey(cleanCurrency)) {
            return "Error: Unsupported currency code '" + cleanCurrency + "'.";
        }

        double rateToUsd = Double.parseDouble(rates.get(cleanCurrency).toString());
        double amountInUsd = amount / rateToUsd;

        if (amountInUsd >= 10000) {
            return "Warning: " + amount + " " + cleanCurrency + " is a LARGE transaction. Additional verification required.";
        } else if (amountInUsd <= 1) {
            return "Warning: " + amount + " " + cleanCurrency + " is a very small amount, conversion may round to zero.";
        } else {
            return "Transaction amount is within normal limits.";
        }
    }

    public List<CurrencyLog> getLogsByCurrency(String currency) {
        return currencyRepository.findByInputCurrencyIgnoreCase(currency.trim());
    }

    public void validateApiKey(String requestKey) {
        if (requestKey == null || requestKey.trim().isEmpty()) {
            throw new UnauthorizedException("API Key missing from HTTP Headers!");
        }
        apiKeyRepository.findByKeyValueAndActiveTrue(requestKey.trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid, inactive, or revoked API Key provided!"));
    }
}