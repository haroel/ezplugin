let config = {};

config["web"] = [
];

config["ios"] = [
    {
        pluginName: "PluginIAP",
        params: {
            autoVerify: true
        }
    },
    {
        pluginName: "PluginOS"
    },
    {
        pluginName: "PluginSystemControll"
    },
    {
        pluginName: "PluginAssetsUpdate"
    },
    {
        pluginName: "PluginFacebook"
    },
    {
        pluginName: "PluginGoogle"
    },
    // {
    //     pluginName:"PluginVisitor"
    // },
    {
        pluginName: "PluginWebView"
    },
    {
        pluginName: "PluginBugly"
    },
    {
        pluginName: "PluginFileDownload"
    },
    // {
    //     pluginName: "PluginAdMob",
    //     params: {
    //         adUnitID: "ca-app-pub-4155862935870750/2808096516",
    //         debugAdUnitID: "ca-app-pub-3940256099942544/1712485313" // Google专有测试广告
    //     }
    // }
    {
        pluginName: "PluginTopon",
        params: {
            appID: "xxx",
            appKey: "yyyy",
            placementID: "zzzz"
        }
    },

    {
        pluginName: "PluginToponInterstitial",
        params: {
            appID: "xxxx",
            appKey: "yyyy",
            placementID: "zzzz"
        }
    },
    {
        pluginName: "PluginUpdater"
    }
];

config["android"] = [
    {
        pluginName: "PluginOS"
    },
    {
        pluginName: "PluginLog"
    },
    {
        pluginName: "PluginPermission"
    },
    {
        pluginName: "PluginAssetsUpdate"
    },
    // {
    //     pluginName: "PluginFacebook"
    // },
    // {
    //     pluginName: "PluginGoogle"
    // },
    // {
    //     pluginName:"PluginVisitor"
    // },
    // {
    //     pluginName: "PluginWebView"
    // },
    {
        pluginName: "PluginGooglePay"
    },
    {
        pluginName: "PluginBugly"
    },
    // {
    //     pluginName: "PluginAdMob",
    //     params: {
    //         adUnitID: "ca-app-pub-4155862935870750/2808096516",
    //         debugAdUnitID: "ca-app-pub-3940256099942544/5224354917" // Google专有测试广告
    //     }
    // }

    {
        pluginName: "PluginTopon",
        params: {
            appID: "xxxx",
            appKey: "yyyy",
            placementID: "zzzzz"
        }
    },
    {
        pluginName: "PluginToponInterstitial",
        params: {
            appID: "xxxx",
            appKey: "yyyyy",
            placementID: "zzzzz"
        }
    },
    {
        pluginName: "PluginUpdater"
    }
];



module.exports = function (channel) {
    return config[channel];
}