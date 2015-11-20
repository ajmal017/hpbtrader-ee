package com.highpowerbear.hpbtrader.linear.ibclient;

import com.highpowerbear.hpbtrader.linear.common.EventBroker;
import com.highpowerbear.hpbtrader.linear.common.LinData;
import com.highpowerbear.hpbtrader.linear.common.SingletonRepo;
import com.highpowerbear.hpbtrader.linear.mktdata.MktDataController;
import com.highpowerbear.hpbtrader.linear.strategy.OrderStateHandler;
import com.highpowerbear.hpbtrader.shared.common.HtrEnums;
import com.highpowerbear.hpbtrader.shared.common.HtrSettings;
import com.highpowerbear.hpbtrader.shared.common.HtrUtil;
import com.highpowerbear.hpbtrader.shared.ibclient.AbstractIbListener;
import com.highpowerbear.hpbtrader.shared.ibclient.IbApiEnums;
import com.highpowerbear.hpbtrader.shared.entity.Bar;
import com.highpowerbear.hpbtrader.shared.entity.IbAccount;
import com.highpowerbear.hpbtrader.shared.entity.IbOrder;
import com.highpowerbear.hpbtrader.shared.entity.Series;
import com.highpowerbear.hpbtrader.shared.persistence.IbOrderDao;
import com.highpowerbear.hpbtrader.shared.persistence.SeriesDao;
import com.ib.client.Contract;
import com.ib.client.OrderState;

import java.util.Calendar;
import java.util.List;

/**
 *
 * @author rkolar
 */
public class IbListenerImpl extends AbstractIbListener {
    private LinData linData = SingletonRepo.getInstance().getLinData();
    private IbOrderDao ibOrderDao = SingletonRepo.getInstance().getIbOrderDao();
    private SeriesDao seriesDao = SingletonRepo.getInstance().getSeriesDao();
    private IbController ibController = SingletonRepo.getInstance().getIbController();
    private MktDataController mktDataController = SingletonRepo.getInstance().getMktDataController();
    private OrderStateHandler orderStateHandler = SingletonRepo.getInstance().getOrderStateHandler();
    private HeartbeatControl heartbeatControl = SingletonRepo.getInstance().getHeartbeatControl();
    private EventBroker eventBroker = SingletonRepo.getInstance().getEventBroker();

    private IbAccount ibAccount;

    public IbListenerImpl(IbAccount ibAccount) {
        this.ibAccount = ibAccount;
    }

    @Override
    public void orderStatus(int orderId, String status, int filled, int remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
        super.orderStatus(orderId, status, filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId, whyHeld);
        
        if (!(  IbApiEnums.OrderStatus.SUBMITTED.getName().equalsIgnoreCase(status) ||
                IbApiEnums.OrderStatus.CANCELLED.getName().equalsIgnoreCase(status) ||
                IbApiEnums.OrderStatus.FILLED.getName().equalsIgnoreCase(status)))
        {
            return;
        }
        IbOrder ibOrder = ibOrderDao.getIbOrderByIbPermId(ibAccount, permId);
        if (ibOrder == null) {
            return;
        }

        if (IbApiEnums.OrderStatus.SUBMITTED.getName().equalsIgnoreCase(status) && HtrEnums.IbOrderStatus.SUBMITTED.equals(ibOrder.getStatus())) {
            heartbeatControl.heartbeatReceived(ibOrder);
            eventBroker.trigger(HtrEnums.DataChangeEvent.STRATEGY_UPDATE);
        } else if (IbApiEnums.OrderStatus.SUBMITTED.getName().equalsIgnoreCase(status) && !HtrEnums.IbOrderStatus.SUBMITTED.equals(ibOrder.getStatus())) {
            heartbeatControl.heartbeatReceived(ibOrder);
            orderStateHandler.orderSubmitted(ibOrder, HtrUtil.getCalendar());
        } else if (IbApiEnums.OrderStatus.CANCELLED.getName().equalsIgnoreCase(status) && !HtrEnums.IbOrderStatus.CANCELED.equals(ibOrder.getStatus())) {
            heartbeatControl.removeHeartbeat(ibOrder);
            orderStateHandler.orderCanceled(ibOrder, HtrUtil.getCalendar());
        } else if (IbApiEnums.OrderStatus.FILLED.getName().equalsIgnoreCase(status) && remaining == 0 && !HtrEnums.IbOrderStatus.FILLED.equals(ibOrder.getStatus())) {
            heartbeatControl.removeHeartbeat(ibOrder);
            orderStateHandler.orderFilled(ibOrder, HtrUtil.getCalendar(), avgFillPrice);
        }
    }

    @Override
    public void openOrder(int orderId, Contract contract, com.ib.client.Order order, OrderState orderState) {
        super.openOrder(orderId, contract, order, orderState);
        
        IbOrder dbIbOrder = ibOrderDao.getIbOrderByIbOrderId(ibAccount, orderId);
        if (dbIbOrder != null && dbIbOrder.getIbPermId() == null) {
            dbIbOrder.setIbPermId(order.m_permId);
            ibOrderDao.updateIbOrder(dbIbOrder);
            eventBroker.trigger(HtrEnums.DataChangeEvent.STRATEGY_UPDATE);
        }
    }

    @Override
    public void nextValidId(int orderId) {
        super.nextValidId(orderId);
        ibController.setNextValidOrderId(ibAccount, orderId);
    }

    @Override
    public void historicalData(int reqId, String date, double open, double high, double low, double close, int volume, int count, double WAP, boolean hasGaps) {
        //super.historicalData(reqId, date, open, high, low, close, volume, count, WAP, hasGaps);
        
        Series s = seriesDao.findSeries(reqId / HtrSettings.IB_REQUEST_MULT);
        if (date.startsWith("finish")) {
            // remove last bar if it is not finished yet
            List<Bar> barsReceived = linData.getBarsReceivedMap().get(s.getId());
            int numBars = barsReceived.size();
            Bar lastBar = barsReceived.get(numBars - 1);
            if (lastBar.getTimeInMillisBarClose() > HtrUtil.getCalendar().getTimeInMillis()) {
                barsReceived.remove(numBars - 1);
            }
            mktDataController.processBars(s);
            return;
        }
        Bar q = new Bar();
        Calendar c = HtrUtil.getCalendar();
        c.setTimeInMillis(Long.valueOf(date) * 1000 + s.getInterval().getMillis()); // date-time stamp of the end of the bar
        if (HtrEnums.Interval.INT_60_MIN.equals(s.getInterval())) {
            c.set(Calendar.MINUTE, 0); // needed in case of bars started at 9:30 (END 10:00 not 10:30) or 17:15 (END 18:00 not 18:15)
        }
        q.setqDateBarClose(c);
        q.setqOpen(open);
        q.setHigh(high);
        q.setLow(low);
        q.setqClose(close);
        q.setVolume(volume == -1 ? 0 : volume);
        q.setCount(count);
        q.setWap(WAP);
        q.setHasGaps(hasGaps);
        q.setSeries(s);
        linData.getBarsReceivedMap().get(s.getId()).add(q);
    }

    @Override
    public void tickPrice(int tickerId, int field, double price, int canAutoExecute) {
        mktDataController.updateRealtimeData(tickerId, field, price);
    }

    @Override
    public void tickSize(int tickerId, int field, int size) {
        mktDataController.updateRealtimeData(tickerId, field, size);
    }

    @Override
    public void tickGeneric(int tickerId, int tickType, double value) {
        mktDataController.updateRealtimeData(tickerId, tickType, value);
    }

    @Override
    public void tickString(int tickerId, int tickType, String value) {
    }

    @Override
    public void tickOptionComputation(int tickerId, int field, double impliedVol, double delta, double optPrice, double pvDividend, double gamma, double vega, double theta, double undPrice) {
    }
}
