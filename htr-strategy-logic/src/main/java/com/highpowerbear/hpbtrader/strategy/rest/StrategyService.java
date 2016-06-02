package com.highpowerbear.hpbtrader.strategy.rest;

import com.highpowerbear.hpbtrader.shared.common.HtrDefinitions;
import com.highpowerbear.hpbtrader.shared.common.HtrUtil;
import com.highpowerbear.hpbtrader.shared.entity.*;
import com.highpowerbear.hpbtrader.shared.model.RestList;
import com.highpowerbear.hpbtrader.shared.model.TimeFrame;
import com.highpowerbear.hpbtrader.shared.persistence.StrategyDao;
import com.highpowerbear.hpbtrader.shared.persistence.TradeDao;
import com.highpowerbear.hpbtrader.strategy.linear.ProcessContext;
import com.highpowerbear.hpbtrader.strategy.linear.StrategyController;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Calendar;
import java.util.List;

/**
 * Created by robertk on 11/14/2015.
 */
@ApplicationScoped
@Path("strategies")
public class StrategyService {

    @Inject private StrategyDao strategyDao;
    @Inject private TradeDao tradeDao;
    @Inject private StrategyController strategyController;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public RestList<Strategy> getStrategies() {
        List<Strategy> strategies =  strategyDao.getStrategiesByInputSeriesAlias();
        return new RestList<>(strategies, (long) strategies.size());
    }

    @DELETE
    @Path("{strategyid}")
    public Response deleteStrategy(@PathParam("strategyid") Integer strategyId) {
        Strategy strategy = strategyDao.findStrategy(strategyId);
        if (strategy == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        strategyDao.deleteStrategy(strategy);
        return Response.ok().build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("{strategyid}/backtest")
    public Response backtestStrategy(@PathParam("strategyid") Integer strategyId, TimeFrame timeFrame) {
        Strategy strategy = strategyDao.findStrategy(strategyId);
        if (strategy == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Calendar fromDate = timeFrame.getFromDate() == null ? HtrUtil.getCalendarMonthsOffset(HtrDefinitions.BACKTEST_DEFAULT_MONTHS) : timeFrame.getFromDate();
        Calendar toDate = timeFrame.getToDate() == null ? HtrUtil.getCalendar() : timeFrame.getToDate();
        strategyController.queueBacktestStrategy(strategy, new TimeFrame(fromDate, toDate));
        return Response.ok().build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{strategyid}/backtest/finished")
    public Response isBacktestFinished(@PathParam("strategyid") Integer strategyId, TimeFrame timeFrame) {
        Strategy strategy = strategyDao.findStrategy(strategyId);
        if (strategy == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.ok(strategyController.isBacktestFinished(strategy)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("{strategyid}/manualorder")
    public Response manualOrder(@PathParam("strategyid") Integer strategyId, IbOrder ibOrder) {
        Strategy strategy = strategyDao.findStrategy(strategyId);
        if (strategy == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        ibOrder.setStrategy(strategy);
        strategyController.manualOrder(ibOrder);
        return Response.ok().build();
    }

    @GET
    @Path("{strategyid}/strategylogs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStrategyLogs(@PathParam("strategyid") Integer strategyId, @QueryParam("start") Integer start, @QueryParam("limit") Integer limit) {
        return getStrategyLogs(strategyId, false, start, limit);
    }

    @GET
    @Path("{strategyid}/backtest/strategylogs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBacktestStrategyLogs(@PathParam("strategyid") Integer strategyId, @QueryParam("start") Integer start, @QueryParam("limit") Integer limit) {
        return getStrategyLogs(strategyId, true, start, limit);
    }

    @GET
    @Path("{strategyid}/iborders")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIbOrders(@PathParam("strategyid") Integer strategyId, @QueryParam("start") Integer start, @QueryParam("limit") Integer limit) {
        return getIbOrders(strategyId, false, start, limit);
    }

    @GET
    @Path("{strategyid}/backtest/iborders")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBacktestIbOrders(@PathParam("strategyid") Integer strategyId, @QueryParam("start") Integer start, @QueryParam("limit") Integer limit) {
        return getIbOrders(strategyId, true, start, limit);
    }

    @GET
    @Path("{strategyid}/trades")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTrades(@PathParam("strategyid") Integer strategyId, @QueryParam("start") Integer start, @QueryParam("limit") Integer limit) {
        return getTrades(strategyId, false, start, limit);
    }

    @GET
    @Path("{strategyid}/backtest/trades")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBacktestTrades(@PathParam("strategyid") Integer strategyId, @QueryParam("start") Integer start, @QueryParam("limit") Integer limit) {
        return getTrades(strategyId, true, start, limit);
    }

    @GET
    @Path("trade/{tradeid}/tradelogs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTradeLogs(@PathParam("tradeid") Long tradeId, @QueryParam("start") Integer start, @QueryParam("limit") Integer limit) {
        return getTradeLogs(tradeId, false, start, limit);
    }

    @GET
    @Path("{tradeid}/backtest/tradelogs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBacktestTradeLogs(@PathParam("tradeid") Long tradeId, @QueryParam("start") Integer start, @QueryParam("limit") Integer limit) {
        return getTradeLogs(tradeId, false, start, limit);
    }

    private Response getStrategyLogs(Integer strategyId, boolean backtest, Integer start, Integer limit) {
        ProcessContext ctx = getProcessContext(strategyId, backtest);
        if (ctx == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        List<StrategyLog> strategyLogs = ctx.getPagedStrategyLogs(start, limit);
        Long numStrategyLogs = ctx.getNumStrategyLogs();
        return Response.ok(new RestList<>(strategyLogs, numStrategyLogs)).build();
    }

    private Response getIbOrders(Integer strategyId, boolean backtest, Integer start, Integer limit) {
        ProcessContext ctx = getProcessContext(strategyId, backtest);
        if (ctx == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        List<IbOrder> ibOrders = ctx.getPagedIbOrders(start, limit);
        Long numIbOrders = ctx.getNumIbOrders();
        return Response.ok(new RestList<>(ibOrders, numIbOrders)).build();
    }

    private Response getTrades(Integer strategyId, boolean backtest, Integer start, Integer limit) {
        ProcessContext ctx = getProcessContext(strategyId, backtest);
        if (ctx == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        List<Trade> trades = ctx.getPagedTrades(start, limit);
        Long numTrades = ctx.getNumTrades();
        return Response.ok(new RestList<>(trades, numTrades)).build();
    }

    private Response getTradeLogs(Long tradeId, boolean backtest, Integer start, Integer limit) {
        Trade trade = tradeDao.findTrade(tradeId);
        if (trade == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        ProcessContext ctx = getProcessContext(trade.getStrategy().getId(), backtest);
        if (ctx == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        List<TradeLog> tradeLogs = ctx.getPagedTradeLogs(trade, start, limit);
        Long numTradeLogs = ctx.getNumTradeLogs(trade);
        return Response.ok(new RestList<>(tradeLogs, numTradeLogs)).build();
    }

    private ProcessContext getProcessContext(Integer strategyId, boolean backtest) {
        Strategy strategy = strategyDao.findStrategy(strategyId);
        if (strategy == null) {
            return null;
        }
        return backtest ? strategyController.getBacktestContextMap().get(strategy) : strategyController.getProcessContextMap().get(strategy);
    }
}
