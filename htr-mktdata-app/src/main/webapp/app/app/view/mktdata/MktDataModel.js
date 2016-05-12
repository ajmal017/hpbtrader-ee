/**
 * Created by robertk on 4/3/2016.
 */
Ext.define('MktData.view.mktdata.MktDataModel', {
    extend: 'Ext.app.ViewModel',
    requires: [
        'MktData.model.mktdata.DataBar',
        'MktData.model.mktdata.DataSeries',
        'MktData.model.mktdata.IbAccount',
        'MktData.model.mktdata.RtData',
        'MktData.model.indicator.Ema',
        'MktData.model.indicator.Macd',
        'MktData.model.indicator.Stochastics'
    ],

    alias: 'viewmodel.htr-mktdata',

    stores: {
        ibAccounts: {
            model: 'MktData.model.mktdata.IbAccount',
            pageSize: 10
        },
        dataSeriesStore: {
            model: 'MktData.model.mktdata.DataSeries',
            pageSize: 10
        },
        dataBars: {
            model: 'MktData.model.mktdata.DataBar',
            pageSize: 10
        },
        rtDataStore: {
            model: 'MktData.model.mktdata.RtData',
            pageSize: 10
        }
    }
});