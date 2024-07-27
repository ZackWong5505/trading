package com.crypto.trading.trading.service;

import com.crypto.trading.trading.entity.TradingCurrency;
import com.crypto.trading.trading.entity.TradingHistory;
import com.crypto.trading.trading.entity.User;
import com.crypto.trading.trading.modal.*;
import com.crypto.trading.trading.repository.TradeHistoryRepository;
import com.crypto.trading.trading.repository.TradingCurrencyRepository;
import com.crypto.trading.trading.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TradingServiceImplTest {

    @Mock
    private ApiService apiService;

    @Mock
    private TradingCurrencyRepository tradingCurrencyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TradeHistoryRepository tradeHistoryRepository;

    @InjectMocks
    private TradingServiceImpl tradingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @InjectMocks
    ObjectMapper objectMapper;


    @Test
    void testExecuteTransaction() throws JsonProcessingException {
        // Prepare test data
        TradeRequest tradeRequest = new TradeRequest();
        tradeRequest.setUserName("testuser");
        tradeRequest.setSymbol("BTCUSDT");
        tradeRequest.setQuantity(0.1);
        tradeRequest.setBuyOrder(true);

        TradingCurrency tradingCurrency = new TradingCurrency();
        tradingCurrency.setCryptoDetails("{\"BTCUSDT\":{\"bidPrice\":\"40000.00\",\"bidQty\":\"0.1\",\"askPrice\":\"40010.00\",\"askQty\":\"0.2\"}}");
        tradingCurrency.setCreateTime(LocalDateTime.now());

        User user = new User();
        user.setUserName("testuser");
        user.setBalance(100000.0);

        when(tradingCurrencyRepository.findLatestCryptoDetails()).thenReturn(tradingCurrency);
        when(userRepository.findAllByUserName("testuser")).thenReturn(user);

        // Execute the method to be tested
        tradingService.executeTransaction(tradeRequest);

        // Verify interactions with mocks
        verify(userRepository, times(1)).save(user);
        verify(tradeHistoryRepository, times(1)).save(any(TradingHistory.class));

        // Verify user balance update
        assertEquals(100000.0 - (0.1 * 40010.00), user.getBalance(), 0.001);
    }

    @Test
    public void testGetBestCryptoPrice() throws JsonProcessingException {

        ResponseEntity<String> binanceResponse = new ResponseEntity<>("[{\"symbol\":\"BTCUSDT\",\"bidPrice\":\"40000.0\",\"bidQty\":\"40000.0\",\"askPrice\":\"40000.0\",\"askQty\":\"40000.0\"}]", HttpStatus.OK);
        ResponseEntity<String> huobiResponse = new ResponseEntity<>("{\n" +
                "    \"data\": [\n" +
                "        {\n" +
                "            \"symbol\": \"sylousdt\",\n" +
                "            \"open\": 0.001042,\n" +
                "            \"high\": 0.001065,\n" +
                "            \"low\": 0.001036,\n" +
                "            \"close\": 0.001063,\n" +
                "            \"amount\": 4.030720058137E8,\n" +
                "            \"vol\": 421303.4094530035,\n" +
                "            \"count\": 9368,\n" +
                "            \"bid\": 0.00106,\n" +
                "            \"bidSize\": 12001.9203,\n" +
                "            \"ask\": 0.001068,\n" +
                "            \"askSize\": 151264.9074\n" +
                "        }\n" +
                "    ]\n" +
                "}", HttpStatus.OK);

        when(apiService.callApi(any(), any(HttpHeaders.class))).thenReturn(binanceResponse).thenReturn(huobiResponse);

        // Execute the method
        Map<String, TradingCurrencyDTO> result = tradingService.getBestCryptoPrice();

        // Verify the results
        assertEquals(2, result.size());
        assertEquals("40000.0", result.get("BTCUSDT").getAskPrice());
    }

    @Test
    public void testGetLatestWalletBal() {
        User mockUser = new User();
        mockUser.setUserName("testUser");
        mockUser.setBalance(1000.0);
        when(userRepository.findAllByUserName("testUser")).thenReturn(mockUser);

        WalletBalanceDTO result = tradingService.getLatestWalletBal("testUser");

        assertEquals("testUser", result.getUserName());
        assertEquals(1000.0, result.getBalance());

        verify(userRepository).findAllByUserName("testUser");
    }

    @Test
    public void testGetTradingHistory() {
        // Prepare mock user and trading history
        User mockUser = new User();
        mockUser.setUserName("testUser");

        TradingHistory trade1 = new TradingHistory();
        trade1.setUser(mockUser);
        trade1.setCryptoSymbol("BTC");
        trade1.setQuantity(0.1);
        trade1.setPrice(50000.0);

        TradingHistory trade2 = new TradingHistory();
        trade2.setUser(mockUser);
        trade2.setCryptoSymbol("ETH");
        trade2.setQuantity(2.0);
        trade2.setPrice(2000.0);

        when(userRepository.findAllByUserName("testUser")).thenReturn(mockUser);
        when(tradeHistoryRepository.findAllByUser(mockUser)).thenReturn(List.of(trade1, trade2));

        // Execute the method
        List<TradingLogDTO> result = tradingService.getTradingHistory("testUser");

        // Verify the results
        assertEquals(2, result.size());
        assertEquals("BTC", result.get(0).getCryptoSymbol());
        assertEquals(0.1, result.get(0).getQuantity());
        assertEquals(50000.0, result.get(0).getPrice());
        assertEquals("ETH", result.get(1).getCryptoSymbol());
        assertEquals(2.0, result.get(1).getQuantity());
        assertEquals(2000.0, result.get(1).getPrice());

        // Verify interactions
        verify(userRepository).findAllByUserName("testUser");
        verify(tradeHistoryRepository).findAllByUser(mockUser);
    }

}