package com.highpowerbear.hpbtrader.exec.message;

import com.highpowerbear.hpbtrader.exec.ibclient.IbController;
import com.highpowerbear.hpbtrader.shared.common.HtrDefinitions;
import com.highpowerbear.hpbtrader.shared.common.HtrEnums;
import com.highpowerbear.hpbtrader.shared.common.HtrUtil;
import com.highpowerbear.hpbtrader.shared.entity.IbOrder;
import com.highpowerbear.hpbtrader.shared.persistence.IbOrderDao;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by robertk on 18.5.2016.
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = HtrDefinitions.STRATEGY_TO_EXEC_QUEUE),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "5")
})
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MqStrategyReceiverBean implements MessageListener {
    private static final Logger l = Logger.getLogger(HtrDefinitions.LOGGER);

    @Inject private IbOrderDao ibOrderDao;
    @Inject private IbController ibController;

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                String msg = ((TextMessage) message).getText();
                String corId = message.getJMSCorrelationID();
                l.info("Text message received from MQ=" + HtrDefinitions.STRATEGY_TO_EXEC_QUEUE + ", corId=" + corId + ", msg=" + msg);
                HtrEnums.MessageType messageType = HtrUtil.parseMessageType(msg);
                if (HtrEnums.MessageType.IBORDER_CREATED.equals(messageType)) {
                    Long id = Long.valueOf(corId);
                    IbOrder ibOrder = ibOrderDao.findIbOrder(id);
                    if (ibOrder != null) {
                        ibController.submitIbOrder(ibOrder);
                    } else {
                        l.warning("IB order id=" + id + " not found, ignoring");
                    }
                }
            } else {
                l.warning("Non-text message received from MQ=" + HtrDefinitions.STRATEGY_TO_EXEC_QUEUE + ", ignoring");
            }
        } catch (JMSException e) {
            l.log(Level.SEVERE, "Error", e);
        }
    }
}