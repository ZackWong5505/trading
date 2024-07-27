package com.crypto.trading.trading.modal;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BinanceTradingCurrencyApiResp {
        private String symbol;
        private String bidPrice;
        private String bidQty;
        private String askPrice;
        private String askQty;
}
