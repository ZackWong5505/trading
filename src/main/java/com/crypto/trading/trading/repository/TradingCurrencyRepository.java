package com.crypto.trading.trading.repository;

import com.crypto.trading.trading.entity.TradingCurrency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TradingCurrencyRepository extends JpaRepository<TradingCurrency, UUID> {

    @Query("SELECT t FROM TradingCurrency t WHERE t.createTime = (SELECT MAX(t2.createTime) FROM TradingCurrency t2)")
    TradingCurrency findLatestCryptoDetails();
}
