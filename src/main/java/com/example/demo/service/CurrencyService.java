package com.example.demo.service;

import com.example.demo.model.CurrencyLog;
import com.example.demo.repository.CurrencyRepository;
import com.example.demo.repository.ApiKeyRepository;
import com.example.demo.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final CurrencyRepository currencyRepository;
    private final ApiKeyRepository apiKeyRepository; // NEW

    private final Map<String, Double> ratesToUsd = Map.of(
        "USD", 1.0,
        "LKR", 300.0,
        "EUR", 0.92,
        "GBP", 0.79,
        "INR", 83.0
    );

    public CurrencyLog convertAndSave(double amount, String fromCurrency, String toCurrency) {
        String from = fromCurrency.trim().toUpperCase();
        String to = toCurrency.trim().toUpperCase();

        if (!ratesToUsd.containsKey(from) || !ratesToUsd.containsKey(to)) {
            throw new IllegalArgumentException("Unsupported currency code. Supported currencies: " + ratesToUsd.keySet());
        }

        double amountInUsd = amount / ratesToUsd.get(from);
        double result = amountInUsd * ratesToUsd.get(to);

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

        if (!ratesToUsd.containsKey(cleanCurrency)) {
            return "Error: Unsupported currency code '" + cleanCurrency + "'. Supported currencies: " + ratesToUsd.keySet();
        }

        double amountInUsd = amount / ratesToUsd.get(cleanCurrency);

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

    // --- NEW: API key validation ---
    public void validateApiKey(String requestKey) {
        if (requestKey == null || requestKey.trim().isEmpty()) {
            throw new UnauthorizedException("API Key missing from HTTP Headers!");
        }
        apiKeyRepository.findByKeyValueAndActiveTrue(requestKey.trim())
            .orElseThrow(() -> new UnauthorizedException("Invalid, inactive, or revoked API Key provided!"));
    }
}