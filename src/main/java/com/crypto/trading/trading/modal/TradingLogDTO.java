package com.crypto.trading.trading.modal;

import com.crypto.trading.trading.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TradingLogDTO {

    private Long id;
    private User user;
    private LocalDateTime tradeTime;
    private String cryptoSymbol;
    private String tradeType;
    private double quantity;
    private double price;
}
