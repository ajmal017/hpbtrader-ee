/**
 * This class is the main view for the application. It is specified in app.js as the
 * "autoCreateViewport" property. That setting automatically applies the "viewport"
 * plugin to promote that instance of this class to the body element.
 *
 * TODO - Replace this content of this view to suite the needs of your application.
 */
Ext.define('HtrGui.view.main.Main', {
    extend: 'Ext.container.Container',
    requires: [
        'Ext.button.Button',
        'Ext.layout.container.VBox',
        'HtrGui.common.Glyphs',
        'HtrGui.view.mktdata.MktData',
        'HtrGui.view.main.MainController',
        'HtrGui.view.main.MainModel'
    ],

    xtype: 'app-main',

    controller: 'main',
    viewModel: {
        type: 'main'
    },
    scrollable: true,
    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    items: [{
        xtype: 'htr-mktdata'
    }]
});