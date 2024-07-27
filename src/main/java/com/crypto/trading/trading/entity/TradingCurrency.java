package com.crypto.trading.trading.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "trading_currency")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingCurrency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "create_time", unique = true)
    private LocalDateTime createTime;

    @Column(name = "crypto_details", columnDefinition = "TEXT")
    private String cryptoDetails;
}