package com.highpowerbear.hpbtrader.strategy.options;

import com.highpowerbear.hpbtrader.strategy.common.StrategyDefinitions;
import com.highpowerbear.hpbtrader.strategy.common.StrategyEnums;
import com.highpowerbear.hpbtrader.strategy.common.StrategyUtil;
import com.highpowerbear.hpbtrader.strategy.options.model.MarketData;
import com.highpowerbear.hpbtrader.strategy.options.model.UnderlyingData;
import com.highpowerbear.hpbtrader.shared.common.HtrEnums;
import com.highpowerbear.hpbtrader.shared.common.HtrUtil;
import com.ib.client.TickType;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.logging.Logger;

/**
 *
 * @author robertk
 */
@Named
@ApplicationScoped
public class DataRetriever {
    private static final Logger l = Logger.getLogger(StrategyDefinitions.LOGGER);

    @Inject private OptData optData;
    @Inject private ChainsRetriever chainsRetriever;

    public void start() throws Exception {
        int i = 1;
        for (String underlying : optData.getUnderlyingDataMap().keySet()) {
            optData.getUnderlyingDataMap().get(underlying).setIbRequestIdBase(StrategyDefinitions.REQUEST_ID_MULTIPLIER * i++);
        }
        chainsRetriever.reloadOptionChains();
        HtrUtil.waitMilliseconds(StrategyDefinitions.ONE_SECOND_MILLIS * 4);
        requestRtDataForUnderlyings();
    }

    public void stop() {
        for (Integer reqId : optData.getMarketDataRequestMap().keySet()) {
            //ibController.cancelRealtimeData(reqId);
            // TODO
            HtrUtil.waitMilliseconds(StrategyDefinitions.ONE_SECOND_MILLIS / 2);
        }
    }
    
    private void requestRtDataForUnderlyings() {
        for (String underlying : optData.getUnderlyingDataMap().keySet()) {
            HtrUtil.waitMilliseconds(StrategyDefinitions.ONE_SECOND_MILLIS);
            optData.getMarketDataMap().put(underlying, new MarketData(underlying, HtrEnums.SecType.STK, underlying));
            com.ib.client.Contract ibContract = StrategyUtil.constructIbContract(underlying);
            int reqId = optData.getUnderlyingDataMap().get(underlying).getIbRequestIdBase() + StrategyEnums.OptRequestIdOffset.MKTDATA_UNDERLYING.getValue();
            optData.getMarketDataRequestMap().put(reqId, underlying);
            //ibController.requestRealtimeData(reqId, ibContract);
            // TODO
        }
    }
    
    public void updateRealtimeData(int reqId, int field, double price) {
        String symbol = optData.getMarketDataRequestMap().get(reqId);
        if (symbol == null) {
            return;
        }
        MarketData marketData = optData.getMarketDataMap().get(symbol);
        if (marketData == null) {
            return;
        }
        marketData.setField(field, price);
        if (TickType.LAST != field || StrategyUtil.isOptionSymbol(symbol)) {
            return;
        }
        UnderlyingData ud = optData.getUnderlyingDataMap().get(symbol);
        if (!ud.isChainsReady()) {
            return;
        }
        if (ud.isCallContractChangeTimoutElapsed() && triggerContractChange(price, ud.getCallContractChangeTriggerPrice())) {
            if (ud.lockCallContract()) {
                //chainsRetriever.prepareCallContracts(ud, price);
            }
        }
        if (ud.isPutContractChangeTimoutElapsed() && triggerContractChange(price, ud.getPutContractChangeTriggerPrice())) {
            if (ud.lockPutContract()) {
                //chainsRetriever.preparePutContracts(ud, price);
            }
        }
    }
    
    public void updateRealtimeData(int reqId, int field, int size) {
        String symbol = optData.getMarketDataRequestMap().get(reqId);
        if (symbol == null) {
            return;
        }
        MarketData marketData = optData.getMarketDataMap().get(symbol);
        if (marketData == null) {
            return;
        }
        marketData.setField(field, size);
    }
    
    private boolean triggerContractChange(Double currentPrice, Double lastContractChangeTriggerPrice) {
        if (StrategyDefinitions.INVALID_PRICE.equals(lastContractChangeTriggerPrice)) {
            return true;
        } else if (!StrategyUtil.roundDownToHalf(currentPrice).equals(StrategyUtil.roundDownToHalf(lastContractChangeTriggerPrice))) {
            return true;
        }
        return false;
    }
}