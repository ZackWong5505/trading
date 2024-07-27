package com.crypto.trading.trading.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "trading_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_name", nullable = false)
    private User user;

    @Column(name = "trade_time")
    private LocalDateTime tradeTime;

    @Column(name = "crypto_symbol", length = 20)
    private String cryptoSymbol;

    @Column(name = "trade_type", length = 10)
    private String tradeType;

    @Column(name = "quantity", precision = 18)
    private double quantity;

    @Column(name = "price", precision = 18)
    private double price;
}
