package com.crypto.trading.trading.repository;

import com.crypto.trading.trading.entity.TradingHistory;
import com.crypto.trading.trading.entity.User;
import com.crypto.trading.trading.modal.TradingLogDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TradeHistoryRepository extends JpaRepository<TradingHistory, UUID> {
    List<TradingHistory> findAllByUser(User user);
}
