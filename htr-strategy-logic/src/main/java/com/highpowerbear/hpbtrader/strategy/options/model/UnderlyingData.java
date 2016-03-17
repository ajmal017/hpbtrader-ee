package com.highpowerbear.hpbtrader.strategy.options.model;

import com.highpowerbear.hpbtrader.shared.common.HtrUtil;
import com.highpowerbear.hpbtrader.shared.entity.Trade;
import com.highpowerbear.hpbtrader.strategy.common.StrategyDefinitions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author robertk
 */
public class UnderlyingData {
    private static final Logger l = Logger.getLogger(StrategyDefinitions.LOGGER);

    private String underlying;
    private Integer ibRequestIdBase;
    private Double callContractChangeTriggerPrice;
    private Double putContractChangeTriggerPrice;
    private String activeCallSymbol;
    private Boolean isActiveCallSymbolPurchased = null;
    private String frontExpiryCallSymbol;
    private String nextExpiryCallSymbol;
    private String activePutSymbol;
    private Boolean isActivePutSymbolPurchased = null;
    private String frontExpiryPutSymbol;
    private String nextExpiryPutSymbol;
    private boolean callContractChangeInProgress = false;
    private Calendar lastCallContractChangeDate;
    private boolean putContractChangeInProgress = false;
    private Calendar lastPutContractChangeDate;

    public UnderlyingData(String underlying) {
        this.underlying = underlying;
    }
    
    public void markChainsReady() {
        setCallContractChangeTriggerPrice(StrategyDefinitions.INVALID_PRICE);
        setPutContractChangeTriggerPrice(StrategyDefinitions.INVALID_PRICE);
    }
    
    public boolean isChainsReady() {
        return (callContractChangeTriggerPrice != null && putContractChangeTriggerPrice != null);
    }
    
    public void purchaseMade(String optionSymbol, Trade activeTrade) {
        if (getActiveCallSymbol() != null && optionSymbol.equals(getActiveCallSymbol())) {
            isActiveCallSymbolPurchased = Boolean.TRUE;
        } else if (getActivePutSymbol() != null && optionSymbol.equals(getActivePutSymbol())) {
            isActivePutSymbolPurchased = Boolean.TRUE;
        }
    }
    
    public void purchaseReleased(String optionSymbol, Trade activeTrade) {
        if (getActiveCallSymbol() != null && optionSymbol.equals(getActiveCallSymbol())) {
            isActiveCallSymbolPurchased = Boolean.FALSE;
        } else if (getActivePutSymbol() != null && optionSymbol.equals(getActivePutSymbol())) {
            isActivePutSymbolPurchased = Boolean.FALSE;
        }
    }
    
    public List<String> getPurchasedSymbols() {
        List<String> purchasedSymbols = new ArrayList<>();
        if (Boolean.TRUE.equals(isActiveCallSymbolPurchased) && activeCallSymbol != null) {
            purchasedSymbols.add(activeCallSymbol);
        }
        if (Boolean.TRUE.equals(isActivePutSymbolPurchased) && activePutSymbol != null) {
            purchasedSymbols.add(activePutSymbol);
        }
        return purchasedSymbols;
    }
    
    private boolean isStandby(String optionSymbol) {
        return (optionSymbol != null && (optionSymbol.equals(frontExpiryCallSymbol) || optionSymbol.equals(nextExpiryCallSymbol) || optionSymbol.equals(frontExpiryPutSymbol) || optionSymbol.equals(nextExpiryPutSymbol)));
    }
    
    private boolean isActive(String optionSymbol) {
        return (optionSymbol != null && (optionSymbol.equals(activeCallSymbol) || optionSymbol.equals(activePutSymbol)));
    }
    
    private boolean isPurchased(String optionSymbol) {
        return ((optionSymbol != null) && ((optionSymbol.equals(activeCallSymbol) && Boolean.TRUE.equals(isActiveCallSymbolPurchased)) || (optionSymbol.equals(activePutSymbol) && Boolean.TRUE.equals(isActivePutSymbolPurchased))));
    }

    public String getUnderlying() {
        return underlying;
    }

    public Integer getIbRequestIdBase() {
        return ibRequestIdBase;
    }

    public void setIbRequestIdBase(Integer ibRequestIdBase) {
        this.ibRequestIdBase = ibRequestIdBase;
    }

    public Double getCallContractChangeTriggerPrice() {
        return callContractChangeTriggerPrice;
    }

    public void setCallContractChangeTriggerPrice(Double callContractChangeTriggerPrice) {
        this.callContractChangeTriggerPrice = callContractChangeTriggerPrice;
    }

    public Double getPutContractChangeTriggerPrice() {
        return putContractChangeTriggerPrice;
    }

    public void setPutContractChangeTriggerPrice(Double putContractChangeTriggerPrice) {
        this.putContractChangeTriggerPrice = putContractChangeTriggerPrice;
    }

    public String getActiveCallSymbol() {
        return activeCallSymbol;
    }

    public void setActiveCallSymbol(String activeCallSymbol) {
        this.activeCallSymbol = activeCallSymbol;
    }

    public Boolean getIsActiveCallSymbolPurchased() {
        return isActiveCallSymbolPurchased;
    }
    
    public String getFrontExpiryCallSymbol() {
        return frontExpiryCallSymbol;
    }

    public void setFrontExpiryCallSymbol(String frontExpiryCallSymbol) {
        this.frontExpiryCallSymbol = frontExpiryCallSymbol;
    }

    public String getNextExpiryCallSymbol() {
        return nextExpiryCallSymbol;
    }

    public void setNextExpiryCallSymbol(String nextExpiryCallSymbol) {
        this.nextExpiryCallSymbol = nextExpiryCallSymbol;
    }

    public String getActivePutSymbol() {
        return activePutSymbol;
    }

    public void setActivePutSymbol(String activePutSymbol) {
        this.activePutSymbol = activePutSymbol;
    }

    public Boolean getIsActivePutSymbolPurchased() {
        return isActivePutSymbolPurchased;
    }

    public String getFrontExpiryPutSymbol() {
        return frontExpiryPutSymbol;
    }

    public void setFrontExpiryPutSymbol(String frontExpiryPutSymbol) {
        this.frontExpiryPutSymbol = frontExpiryPutSymbol;
    }

    public String getNextExpiryPutSymbol() {
        return nextExpiryPutSymbol;
    }

    public void setNextExpiryPutSymbol(String nextExpiryPutSymbol) {
        this.nextExpiryPutSymbol = nextExpiryPutSymbol;
    }

    public synchronized boolean lockCallContract() {
        boolean isSuccess = false;
        if (!callContractChangeInProgress) {
            callContractChangeInProgress = true;
            isSuccess = true;
            l.fine(underlying + " locked CALL contract");
        }
        return isSuccess;
    }
    
    public synchronized void releaseCallContract() {
        callContractChangeInProgress = false;
        l.fine(underlying + " released CALL contract");
    }
    
    public void callContractChanged() {
        lastCallContractChangeDate = HtrUtil.getNowCalendar();
    }
    
    public boolean isCallContractChangeTimoutElapsed() {
        if (lastCallContractChangeDate == null) {
            return true;
        }
        return ((System.currentTimeMillis() - lastCallContractChangeDate.getTimeInMillis()) > StrategyDefinitions.ONE_SECOND_MILLIS * StrategyDefinitions.CONTRACT_CHANGE_MIN_INTERVAL);
    }

    public synchronized boolean lockPutContract() {
        boolean isSuccess = false;
        if (!putContractChangeInProgress) {
            putContractChangeInProgress = true;
            isSuccess = true;
            l.fine(underlying + " locked PUT contract");
        }
        return isSuccess;
    }

    public synchronized void releasePutContract() {
        putContractChangeInProgress = false;
        l.fine(underlying + " released PUT contract");
    }
    
    public void putContractChanged() {
        lastPutContractChangeDate = HtrUtil.getNowCalendar();
    }
    
    public boolean isPutContractChangeTimoutElapsed() {
        if (lastPutContractChangeDate == null) {
            return true;
        }
        return ((System.currentTimeMillis() - lastPutContractChangeDate.getTimeInMillis()) > StrategyDefinitions.ONE_SECOND_MILLIS * StrategyDefinitions.CONTRACT_CHANGE_MIN_INTERVAL);
    }
}