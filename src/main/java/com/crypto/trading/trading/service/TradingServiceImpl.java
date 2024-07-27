package com.crypto.trading.trading.service;


import com.crypto.trading.trading.constant.TradingConstant;
import com.crypto.trading.trading.entity.TradingCurrency;
import com.crypto.trading.trading.entity.TradingHistory;
import com.crypto.trading.trading.entity.User;
import com.crypto.trading.trading.exception.UserNotFoundException;
import com.crypto.trading.trading.modal.*;
import com.crypto.trading.trading.repository.TradeHistoryRepository;
import com.crypto.trading.trading.repository.TradingCurrencyRepository;
import com.crypto.trading.trading.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class TradingServiceImpl implements TradingService {


    private final ObjectMapper objectMapper;
    private final ApiService apiService;
    private final TradingCurrencyRepository tradingCurrencyRepository;
    private final UserRepository userRepository;
    private final TradeHistoryRepository tradeHistoryRepository;

    @Value("${BINANCE_URL}")
    private String BINANCE_URL ;

    @Value("${HUOBI_URL}")
    private String HUOBI_URL ;

    @Value("${scheduledTask.interval:10000}")
    private long scheduledTaskInterval;

    public TradingServiceImpl(ApiService apiService, TradingCurrencyRepository tradingCurrencyRepository,
                              UserRepository userRepository, TradeHistoryRepository tradeHistoryRepository) {
        this.apiService = apiService;
        this.tradingCurrencyRepository = tradingCurrencyRepository;
        this.userRepository = userRepository;
        this.tradeHistoryRepository = tradeHistoryRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Scheduled(fixedRateString = "${scheduledTask.interval}")
    public void scheduledTask() throws JsonProcessingException {
        Map<String, TradingCurrencyDTO> mapData = getBestCryptoPrice();
        String cryptoDetailsJson = objectMapper.writeValueAsString(mapData);
        TradingCurrency tradingCurrency = TradingCurrency.builder()
                .cryptoDetails(cryptoDetailsJson)
                .createTime(LocalDateTime.now())
                .build();

        log.info("[scheduledTask] tradingCurrency {}:", tradingCurrency);
        tradingCurrencyRepository.save(tradingCurrency);
    }

    public Map<String, TradingCurrencyDTO> getBestCryptoPrice() throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");

        // Combine API calls and process responses
        List<BinanceTradingCurrencyApiResp> binanceTradingCurrencyApiRespList = fetchBinanceData(headers);
        List<HuoBiTradingCurrencyApiResp> huoBiTradingCurrencyApiRespList = fetchHuobiData(headers);

        return processAndSaveBestPrices(binanceTradingCurrencyApiRespList, huoBiTradingCurrencyApiRespList);
    }

    private List<BinanceTradingCurrencyApiResp> fetchBinanceData(HttpHeaders headers) throws JsonProcessingException {
        ResponseEntity<String> binanceResponse = apiService.callApi(BINANCE_URL, headers);
        if (binanceResponse.getStatusCode().is2xxSuccessful()) {
            return objectMapper.readValue(binanceResponse.getBody(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, BinanceTradingCurrencyApiResp.class));
        }
        return Collections.emptyList();
    }

    private List<HuoBiTradingCurrencyApiResp> fetchHuobiData(HttpHeaders headers) throws JsonProcessingException {
        ResponseEntity<String> huobiResponse = apiService.callApi(HUOBI_URL, headers);
        if (huobiResponse.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> huobiData = objectMapper.readValue(huobiResponse.getBody(), Map.class);
            List<Map<String, String>> huobiTicks = (List<Map<String, String>>) huobiData.get("data");
            return objectMapper.convertValue(huobiTicks,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, HuoBiTradingCurrencyApiResp.class));
        }
        return Collections.emptyList();
    }

    public Map<String, TradingCurrencyDTO> processAndSaveBestPrices(List<BinanceTradingCurrencyApiResp> binanceList, List<HuoBiTradingCurrencyApiResp> huobiList) {

        Map<String, TradingCurrencyDTO> bestPrices = new HashMap<>();

        for (String symbol : Arrays.asList("ETHUSDT", "BTCUSDT")) {
            TradingCurrencyDTO bestPrice = new TradingCurrencyDTO();

            // Binance Data
            BinanceTradingCurrencyApiResp binanceData = binanceList.stream()
                    .filter(t -> t.getSymbol().equals(symbol))
                    .findFirst().orElse(null);

            // Huobi Data
            HuoBiTradingCurrencyApiResp huobiData = huobiList.stream()
                    .filter(t -> t.getSymbol().equals(symbol.toLowerCase()))
                    .findFirst().orElse(null);

            //setBianance first
            if (binanceData != null) {
                bestPrice.setBidPrice(binanceData.getBidPrice());
                bestPrice.setBidQty(binanceData.getBidQty());
                bestPrice.setAskPrice(binanceData.getAskPrice());
                bestPrice.setAskQty(binanceData.getAskQty());
            }

            //compare and get best price
            if (huobiData != null) {
                if (huobiData.getBid() > Double.parseDouble(bestPrice.getBidPrice())) {
                    bestPrice.setBidPrice(huobiData.getBid().toString());
                    bestPrice.setBidQty(huobiData.getBidSize().toString());
                }
                if (huobiData.getAsk() < Double.parseDouble(bestPrice.getAskPrice())) {
                    bestPrice.setAskPrice(huobiData.getAsk().toString());
                    bestPrice.setAskQty(huobiData.getAskSize().toString());
                }
            }

            bestPrices.put(symbol, bestPrice);
        }
        log.info("[processAndSaveBestPrices] bestPrices {}:", bestPrices);

        return bestPrices;

    }

    public LatestPriceDTO getLatestAggregatedPrice(){
        TradingCurrency tradingCurrency = tradingCurrencyRepository.findLatestCryptoDetails();
        LatestPriceDTO latestPriceDTO = LatestPriceDTO.builder()
                .id(tradingCurrency.getId())
                .timestamp(tradingCurrency.getCreateTime())
                .cryptoDetails(tradingCurrency.getCryptoDetails())
                .build();

        return latestPriceDTO;
    }

    public void executeTransaction(TradeRequest tradeRequest) throws JsonProcessingException {
        TradingCurrency tradingCurrency = tradingCurrencyRepository.findLatestCryptoDetails();

        Map<String, Map<String, TradingCurrencyDTO>> map = objectMapper.readValue(tradingCurrency.getCryptoDetails(), Map.class);

        Map<String, TradingCurrencyDTO> transactionCurrency = map.get(tradeRequest.getSymbol());

        TradingCurrencyDTO tradingCurrencyDTO = objectMapper.convertValue(transactionCurrency, TradingCurrencyDTO.class);

        double executionPrice = tradeRequest.isBuyOrder() ? Double.parseDouble(tradingCurrencyDTO.getAskPrice()) : Double.parseDouble(tradingCurrencyDTO.getBidPrice());
        double totalCost = tradeRequest.getQuantity() * executionPrice;

        User user = Optional.ofNullable(userRepository.findAllByUserName(tradeRequest.getUserName()))
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + tradeRequest.getUserName()));

        if (tradeRequest.isBuyOrder()) {
            user.setBalance(user.getBalance() - totalCost);
        } else {
            user.setBalance(user.getBalance() + totalCost);
        }

        userRepository.save(user);

        insertIntoTradehistory(user, tradeRequest, totalCost);


    }

    public void insertIntoTradehistory(User user, TradeRequest tradeRequest, double totalCost){
        TradingHistory tradingHistory = new TradingHistory();
        tradingHistory.setUser(user);
        tradingHistory.setCryptoSymbol(tradeRequest.getSymbol());
        tradingHistory.setPrice(totalCost);
        tradingHistory.setTradeType(tradeRequest.isBuyOrder() ? TradingConstant.BUY_ORDER : TradingConstant.SELL_ORDER);
        tradingHistory.setQuantity(tradeRequest.getQuantity());
        tradingHistory.setTradeTime(LocalDateTime.now());

        tradeHistoryRepository.save(tradingHistory);
    }

    @Override
    public WalletBalanceDTO getLatestWalletBal(String userName) {

        User user = userRepository.findAllByUserName(userName);
        WalletBalanceDTO walletBalanceDTO = new WalletBalanceDTO();

        BeanUtils.copyProperties(user, walletBalanceDTO);

        return walletBalanceDTO;
    }

    @Override
    public List<TradingLogDTO> getTradingHistory(String userName) {

        User user = userRepository.findAllByUserName(userName);

        List<TradingHistory> tradingHistoryList = tradeHistoryRepository.findAllByUser(user);

        return tradingHistoryList.stream()
                .map(tradingHistory -> {
                    TradingLogDTO tradingLogDTO = new TradingLogDTO();
                    BeanUtils.copyProperties(tradingHistory, tradingLogDTO);
                    return tradingLogDTO;
                })
                .collect(Collectors.toList());
    }
}
