package com.crypto.trading.trading.controller;


import com.crypto.trading.trading.modal.LatestPriceDTO;
import com.crypto.trading.trading.modal.TradeRequest;
import com.crypto.trading.trading.modal.TradingLogDTO;
import com.crypto.trading.trading.modal.WalletBalanceDTO;
import com.crypto.trading.trading.service.TradingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@Slf4j
public class TradingController {
    @Autowired
    private TradingService tradingService;

    @GetMapping("/latest-price")
    public ResponseEntity<LatestPriceDTO> getLatestAggregatedPrice() {
        try {
            Optional<LatestPriceDTO> latestPrice = Optional.ofNullable(tradingService.getLatestAggregatedPrice());
            return latestPrice.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching latest aggregated price", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/execute")
    public ResponseEntity executeTrade(@RequestBody TradeRequest tradeRequest) {
        try {
            tradingService.executeTransaction(tradeRequest);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (Exception e) {
            log.error("Error executeTrade transaction", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/balance/{userName}")
    public ResponseEntity<WalletBalanceDTO> getLatestWalletBal(@PathVariable String userName) {
        try {
            WalletBalanceDTO walletBalanceDTO = tradingService.getLatestWalletBal(userName);
            return ResponseEntity.ok(walletBalanceDTO);
        } catch (Exception e) {
            log.error("Error fetching latest wallet balance", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/history/{userName}")
    public ResponseEntity<List<TradingLogDTO>> getTradingHistory(@PathVariable String userName) {
        try {
            List<TradingLogDTO> tradingLogDTO = tradingService.getTradingHistory(userName);
            return ResponseEntity.ok(tradingLogDTO);
        } catch (Exception e) {
            log.error("Error fetching history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
