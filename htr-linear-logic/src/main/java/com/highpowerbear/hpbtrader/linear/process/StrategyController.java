package com.highpowerbear.hpbtrader.linear.process;

import com.highpowerbear.hpbtrader.linear.common.LinData;
import com.highpowerbear.hpbtrader.linear.common.LinSettings;
import com.highpowerbear.hpbtrader.linear.strategy.BacktestResult;
import com.highpowerbear.hpbtrader.linear.strategy.StrategyLogic;
import com.highpowerbear.hpbtrader.linear.strategy.logic.LuxorStrategyLogic;
import com.highpowerbear.hpbtrader.linear.strategy.logic.MacdCrossStrategyLogic;
import com.highpowerbear.hpbtrader.linear.strategy.logic.TestStrategyLogic;
import com.highpowerbear.hpbtrader.shared.common.HtrEnums;
import com.highpowerbear.hpbtrader.shared.common.HtrSettings;
import com.highpowerbear.hpbtrader.shared.entity.*;
import com.highpowerbear.hpbtrader.shared.persistence.BarDao;
import com.highpowerbear.hpbtrader.shared.persistence.SeriesDao;
import com.highpowerbear.hpbtrader.shared.persistence.StrategyDao;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.Calendar;
import java.util.logging.Logger;

/**
 * Created by robertk on 4/13/14.
 */
@Named
@ApplicationScoped
public class StrategyController implements Serializable {
    private static final Logger l = Logger.getLogger(LinSettings.LOGGER);
    @Inject private BarDao barDao;
    @Inject private StrategyDao strategyDao;
    @Inject private SeriesDao seriesDao;
    @Inject private LinData linData;
    @Inject private Processor processor;
    @Inject private Backtester backtester;

    @PostConstruct
    public void init() {
        seriesDao.getAllSeries(false).forEach(this::swapStrategyLogic);
    }

    public void swapStrategyLogic(Series series) {
        if (series.getEnabled()) {
            Strategy activeStrategy = strategyDao.getActiveStrategy(series);
            linData.getStrategyLogicMap().put(series.getId(), createStrategyLogic(activeStrategy));
        } else {
            linData.getStrategyLogicMap().remove(series.getId());
        }
    }

    private StrategyLogic createStrategyLogic(Strategy strategy) {
        StrategyLogic strategyLogic = null;
        if (HtrEnums.StrategyType.MACD_CROSS.equals(strategy.getStrategyType())) {
            strategyLogic = new MacdCrossStrategyLogic();
        } else if (HtrEnums.StrategyType.TEST.equals(strategy.getStrategyType())) {
            strategyLogic = new TestStrategyLogic();
        } else if (HtrEnums.StrategyType.LUXOR.equals(strategy.getStrategyType())) {
            strategyLogic = new LuxorStrategyLogic();
        }
        return strategyLogic;
    }

    public void process(Series series) {
        if (!series.getEnabled()) {
            l.info("Series not enabled, bars won't be processed, seriesId=" + series.getId() + ", symbol=" + series.getSymbol());
            return;
        }
        Strategy activeStrategy = strategyDao.getActiveStrategy(series);
        StrategyLogic strategyLogic = linData.getStrategyLogicMap().get(series.getId());
        if (barDao.getNumBars(series) >= HtrSettings.BARS_REQUIRED) {
            processor.process(activeStrategy, strategyLogic);
        }
    }

    public void processManual(IbOrder manualIbOrder, Trade activeTrade, Bar bar) {
        processor.processManual(manualIbOrder, activeTrade, bar);
    }

    public BacktestResult backtest(Strategy strategy, Calendar startDate, Calendar endDate) {
        Strategy backtestStrategy = new Strategy(); // need to create new instance for backtest, copy required parameters
        strategy.deepCopy(backtestStrategy);
        backtestStrategy.resetStatistics();
        StrategyLogic backtestStrategyLogic = createStrategyLogic(backtestStrategy); // need to create new instance for backtest
        return backtester.backtest(backtestStrategy, backtestStrategyLogic, startDate, endDate);
    }
}