package com.crypto.trading.trading.modal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;


@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class LatestPriceDTO {

    private Long id;
    private LocalDateTime timestamp;
    private Serializable cryptoDetails;

}
