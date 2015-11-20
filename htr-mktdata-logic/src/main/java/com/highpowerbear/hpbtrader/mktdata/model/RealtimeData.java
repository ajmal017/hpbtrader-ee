package com.highpowerbear.hpbtrader.mktdata.model;

import com.highpowerbear.hpbtrader.shared.common.HtrConstants;
import com.highpowerbear.hpbtrader.shared.common.HtrEnums;
import com.highpowerbear.hpbtrader.shared.common.HtrSettings;
import com.highpowerbear.hpbtrader.shared.common.HtrUtil;
import com.highpowerbear.hpbtrader.shared.entity.Series;
import com.ib.client.TickType;

/**
 * Created by rkolar on 5/23/14.
 */
public class RealtimeData {
    private Series series;
    private String contractClassName;
    private int ibRequestId;

    // price fields
    private RealtimeField<Double> bid;
    private RealtimeField<Double> ask;
    private RealtimeField<Double> last;
    private RealtimeField<Double> close;
    private RealtimeField<Double> changePct;

    // size fields
    private RealtimeField<Integer> bidSize;
    private RealtimeField<Integer> askSize;
    private RealtimeField<Integer> lastSize;

    private RealtimeField<Integer> volume;

    public RealtimeData(Series series) {
        this.series = series;
        this.contractClassName = (HtrUtil.removeDot(series.getSymbol()) + "-" + series.getCurrency()).toLowerCase();
        this.ibRequestId = series.getId() * HtrSettings.IB_REQUEST_MULT + 10;
        initFields();
    }

    private void initFields() {
        bid = new RealtimeField<>(HtrConstants.INVALID_PRICE, HtrEnums.RealtimeStatus.UNCHANGED, "bid", "col-gray-bck");
        ask = new RealtimeField<>(HtrConstants.INVALID_PRICE, HtrEnums.RealtimeStatus.UNCHANGED, "ask", "col-gray-bck");
        last = new RealtimeField<>(HtrConstants.INVALID_PRICE, HtrEnums.RealtimeStatus.UNCHANGED, "last", "col-gray-bck");
        close = new RealtimeField<>(HtrConstants.INVALID_PRICE, HtrEnums.RealtimeStatus.UNCHANGED, "close", "col-gray-bck");
        changePct = new RealtimeField<>(null, HtrEnums.RealtimeStatus.POSITIVE, "changePct", "");
        bidSize = new RealtimeField<>(HtrConstants.INVALID_SIZE, HtrEnums.RealtimeStatus.UNCHANGED, "bid_size", "col-gray-bck");
        askSize = new RealtimeField<>(HtrConstants.INVALID_SIZE, HtrEnums.RealtimeStatus.UNCHANGED, "ask_size", "col-gray-bck");
        lastSize = new RealtimeField<>(HtrConstants.INVALID_SIZE, HtrEnums.RealtimeStatus.UNCHANGED, "last_size", "col-gray-bck");
        volume = new RealtimeField<>(HtrConstants.INVALID_SIZE, HtrEnums.RealtimeStatus.UNCHANGED, "volume", "col-gray-bck");
    }

    public String createUpdateMessage(int field, double price) {
        String message = "rt,";
        switch(field) {
            case TickType.BID:
                setValueStatus(bid, price);
                message += contractClassName + "," + bid.getFieldName() + "," + bid.getValue() + "," + bid.getStatus().getColorClass();
                break;
            case TickType.ASK:
                setValueStatus(ask, price);
                message += contractClassName + "," + ask.getFieldName() + "," + ask.getValue() + "," + ask.getStatus().getColorClass();
                break;
            case TickType.LAST:
                setValueStatus(last, price);
                message += contractClassName + "," + last.getFieldName() + "," + last.getValue() + "," + last.getStatus().getColorClass();
                break;
            case TickType.CLOSE:
                setValueStatus(close, price);
                message += contractClassName + "," + close.getFieldName() + "," + close.getValue() + "," + close.getStatus().getColorClass();
                break;
        }
        return (message.equals("rt,") ? null : message);
    }

    public String createUpdateMessage(int field, int size) {
        String message = "rt,";
        switch(field) {
            case TickType.BID_SIZE:
                setValueStatus(bidSize, size);
                message += contractClassName + "," + bidSize.getFieldName() + "," + bidSize.getValue() + "," + bidSize.getStatus().getColorClass();
                break;
            case TickType.ASK_SIZE:
                setValueStatus(askSize, size);
                message += contractClassName + "," + askSize.getFieldName() + "," + askSize.getValue() + "," + askSize.getStatus().getColorClass();
                break;
            case TickType.LAST_SIZE:
                setValueStatus(lastSize, size);
                message += contractClassName + "," + lastSize.getFieldName() + "," + lastSize.getValue() + "," + lastSize.getStatus().getColorClass();
                break;
            case TickType.VOLUME:
                setValueStatus(volume, size);
                message += contractClassName + "," + volume.getFieldName() + "," + volume.getValue() + "," + volume.getStatus().getColorClass();
                break;
        }
        return (message.equals("rt,") ? null : message);
    }

    public String createChangePctUpdateMsg() {
        String message = "rt,";
        if (!HtrEnums.SecType.CASH.equals(series.getSecType()) && !HtrConstants.INVALID_PRICE.equals(last.getValue()) && !HtrConstants.INVALID_PRICE.equals(close.getValue())) {
            double price = ((last.getValue() - close.getValue()) / close.getValue()) * 100d;
            setValueStatusChangePct(changePct, price);
        } else if (HtrEnums.SecType.CASH.equals(series.getSecType()) && !HtrConstants.INVALID_PRICE.equals(ask.getValue()) && !HtrConstants.INVALID_PRICE.equals(close.getValue())) {
            double price = ((ask.getValue() - close.getValue()) / close.getValue()) * 100d;
            setValueStatusChangePct(changePct, price);
        }
        message += contractClassName + "," + changePct.getFieldName() + "," + getChangePctStr() + "," + changePct.getStatus().getColorClass();
        return message;
    }

    private void setValueStatus(RealtimeField<Double> v, Double price) {
        v.setValueStatus(price > v.getValue() ? HtrEnums.RealtimeStatus.UPTICK : (price < v.getValue() ? HtrEnums.RealtimeStatus.DOWNTICK : HtrEnums.RealtimeStatus.UNCHANGED));
        v.setValue(price);
    }

    private void setValueStatus(RealtimeField<Integer> v, Integer size) {
        v.setValueStatus(size > v.getValue() ? HtrEnums.RealtimeStatus.UPTICK : (size < v.getValue() ? HtrEnums.RealtimeStatus.DOWNTICK : HtrEnums.RealtimeStatus.UNCHANGED));
        v.setValue(size);
    }

    private void setValueStatusChangePct(RealtimeField<Double> v, Double price) {
        v.setValueStatus(price == null || price >= 0d ? HtrEnums.RealtimeStatus.POSITIVE : HtrEnums.RealtimeStatus.NEGATIVE);
        v.setValue(price);
    }

    public Series getSeries() {
        return series;
    }

    public String getContractClassName() {
        return contractClassName;
    }

    public int getIbRequestId() {
        return ibRequestId;
    }

    public RealtimeField<Double> getBid() {
        return bid;
    }

    public RealtimeField<Double> getAsk() {
        return ask;
    }

    public RealtimeField<Double> getLast() {
        return last;
    }

    public RealtimeField<Integer> getBidSize() {
        return bidSize;
    }

    public RealtimeField<Integer> getAskSize() {
        return askSize;
    }

    public RealtimeField<Integer> getLastSize() {
        return lastSize;
    }

    public RealtimeField<Integer> getVolume() {
        return volume;
    }

    public RealtimeField<Double> getClose() {
        return close;
    }

    public RealtimeField<Double> getChangePct() {
        return changePct;
    }

    public String getChangePctStr() {
        return changePct.getValue() == null ? "N/A" : HtrUtil.round(changePct.getValue(), 2) + "%";
    }
}
