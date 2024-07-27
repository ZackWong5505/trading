package com.crypto.trading.trading.service;


import com.crypto.trading.trading.modal.LatestPriceDTO;
import com.crypto.trading.trading.modal.TradeRequest;
import com.crypto.trading.trading.modal.TradingLogDTO;
import com.crypto.trading.trading.modal.WalletBalanceDTO;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;

public interface TradingService {
    LatestPriceDTO getLatestAggregatedPrice();
    void executeTransaction(TradeRequest tradeRequest) throws JsonProcessingException;
    WalletBalanceDTO getLatestWalletBal(String userName);
    List<TradingLogDTO> getTradingHistory(String userName);

}
