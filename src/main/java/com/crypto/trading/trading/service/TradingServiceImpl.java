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

    private final ObjectMapper objectMapper = new ObjectMapper();

//    private static final String BINANCE_URL = "https://api.binance.com/api/v3/ticker/bookTicker";
//    private static final String HUOBI_URL = "https://api.huobi.pro/market/tickers";

    @Autowired
    private ApiService apiService;
    @Autowired
    private TradingCurrencyRepository tradingCurrencyRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TradeHistoryRepository tradeHistoryRepository;

    @Value("${BINANCE_URL}")
    private String BINANCE_URL ;

    @Value("${HUOBI_URL}")
    private String HUOBI_URL ;

    @Scheduled(fixedRate = 10000) // 10 seconds
    public void scheduledTask() throws JsonProcessingException {
        Map<String, TradingCurrencyDTO> mapData = getBestCryptoPrice();
        String cryptoDetailsJson = objectMapper.writeValueAsString(mapData);
        TradingCurrency tradingCurrency = TradingCurrency.builder()
                .cryptoDetails(cryptoDetailsJson)
                .createTime(LocalDateTime.now())
                .build();

        log.info("[scheduledTask] tradingCurrency {}:", tradingCurrency);
        saveTradingCurrency(tradingCurrency);

    }

    public Map<String, TradingCurrencyDTO> getBestCryptoPrice() throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        List<BinanceTradingCurrencyApiResp> binanceTradingCurrencyApiRespList = new ArrayList<>();
        List<HuoBiTradingCurrencyApiResp> huoBiTradingCurrencyApiRespList = new ArrayList<>();

        //Binance
        ResponseEntity<String> binanceResponse = apiService.callApi(BINANCE_URL, headers);
        if (binanceResponse.getStatusCode().is2xxSuccessful()) {
            binanceTradingCurrencyApiRespList = objectMapper.readValue(binanceResponse.getBody(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, BinanceTradingCurrencyApiResp.class));
        }
//        log.info("[getBestCryptoPrice] binanceTradingCurrencyApiRespList {}:", binanceTradingCurrencyApiRespList);

        //Huobi
        ResponseEntity<String> huobiResponse = apiService.callApi(HUOBI_URL, headers);
        if (huobiResponse.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> huobiData = objectMapper.readValue(huobiResponse.getBody(),Map.class);

            List<Map<String, String>> huobiTicks = (List<Map<String, String>>) huobiData.get("data");

            huoBiTradingCurrencyApiRespList = objectMapper.convertValue(huobiTicks,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, HuoBiTradingCurrencyApiResp.class)
            );
        }
//        log.info("[getBestCryptoPrice] huoBiTradingCurrencyApiRespList {}:", huoBiTradingCurrencyApiRespList);

        return processAndSaveBestPrices(binanceTradingCurrencyApiRespList, huoBiTradingCurrencyApiRespList);
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

    public TradingCurrency saveTradingCurrency(TradingCurrency tradingCurrency) {
        return tradingCurrencyRepository.save(tradingCurrency);
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
