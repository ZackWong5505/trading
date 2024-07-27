package com.crypto.trading.trading.modal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TradingCurrencyDTO {

    private String bidPrice;
    private String bidQty;
    private String askPrice;
    private String askQty;

}
