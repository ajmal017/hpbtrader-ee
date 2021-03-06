package com.highpowerbear.hpbtrader.strategy.message;

/**
 * Created by robertk on 11/26/2015.
 */

import com.highpowerbear.hpbtrader.shared.common.HtrDefinitions;
import com.highpowerbear.hpbtrader.shared.common.HtrEnums;
import com.highpowerbear.hpbtrader.shared.common.HtrUtil;
import com.highpowerbear.hpbtrader.shared.entity.Strategy;
import com.highpowerbear.hpbtrader.shared.persistence.StrategyDao;
import com.highpowerbear.hpbtrader.strategy.process.ProcessQueueManager;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = HtrDefinitions.MKTDATA_TO_STRATEGY_QUEUE),
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
    @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "5")
})
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MqMktDataReceiverBean implements MessageListener {
    private static final Logger l = Logger.getLogger(HtrDefinitions.LOGGER);

    @Inject private ProcessQueueManager processQueueManager;
    @Inject private StrategyDao strategyDao;

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                String msg = ((TextMessage) message).getText();
                String corId = message.getJMSCorrelationID();
                l.info("Text message received from MQ=" + HtrDefinitions.MKTDATA_TO_STRATEGY_QUEUE + ", corId=" + corId + ", msg=" + msg);
                HtrEnums.MessageType messageType = HtrUtil.parseMessageType(msg);
                if (HtrEnums.MessageType.DATABARS_CREATED.equals(messageType)) {
                    String seriesAlias = HtrUtil.parseMessageContent(msg);
                    List<Strategy> strategies = strategyDao.getStrategiesByInputSeriesAlias(seriesAlias);
                    strategies.forEach(str -> processQueueManager.queueProcessStrategy(str, seriesAlias));
                }
            } else {
                l.warning("Non-text message received from MQ=" + HtrDefinitions.MKTDATA_TO_STRATEGY_QUEUE + ", ignoring");
            }
        } catch (JMSException e) {
            l.log(Level.SEVERE, "Error", e);
        }
    }
}