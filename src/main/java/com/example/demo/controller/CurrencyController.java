package com.example.demo.controller;

import com.example.demo.model.CurrencyLog;
import com.example.demo.service.CurrencyService;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/currencies")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService currencyService;

    @PostMapping("/convert")
    public CurrencyLog convertCurrency(
            @RequestHeader("X-API-KEY") String apiKey,
            @RequestParam double amount,
            @RequestParam String from,
            @RequestParam String to) {
        currencyService.validateApiKey(apiKey);
        return currencyService.convertAndSave(amount, from, to);
    }

    @GetMapping("/history")
    public List<CurrencyLog> getAllLogs(@RequestHeader("X-API-KEY") String apiKey) {
        currencyService.validateApiKey(apiKey);
        return currencyService.getAllLogs();
    }

    @GetMapping("/warning-check")
    public String checkTransactionWarning(
            @RequestParam double amount,
            @RequestParam String currency) {
        return currencyService.getTransactionWarning(amount, currency);
    }

    @GetMapping("/history/filter")
    public List<CurrencyLog> getFilteredLogs(@RequestParam String currency) {
        return currencyService.getLogsByCurrency(currency);
    }
}