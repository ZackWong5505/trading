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
public class HuoBiTradingCurrencyApiResp {
        private String symbol;
        private Double open;
        private Double high;
        private Double low;
        private Double close;
        private Double amount;
        private Double vol;
        private Integer count;
        private Double bid;
        private Double bidSize;
        private Double ask;
        private Double askSize;
}
