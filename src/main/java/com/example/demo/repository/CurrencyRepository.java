package com.example.demo.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.example.demo.model.CurrencyLog;

import java.util.List;

@Repository
public interface CurrencyRepository extends MongoRepository<CurrencyLog, String> {
	List<CurrencyLog> findByInputCurrencyIgnoreCase(String inputCurrency);
}