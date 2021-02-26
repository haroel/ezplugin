let apn = require('apn');


// Set up apn with the APNs Auth Key
// apnProvider参数可以多个app共享并且永不过期
// https://developer.apple.com/account/resources/authkeys/review/5TNNNRLB9F
let apnProvider = new apn.Provider({
    token: {
        key: './AuthKey_5TNNNRLB9F.p8', // Path to the key p8 file
        keyId: 'xxxx', // The Key ID of the p8 file (available at https://developer.apple.com/account/ios/certificate/key)
        teamId: 'YYYYY', // The Team ID of your Apple Developer Account (available at https://developer.apple.com/account/#/membership/)
    },
    production: false // Set to true if sending a notification to a production iOS app
});

// Enter the device token from the Xcode console
let deviceToken = '.....';
let bundleID = 'com.xxxxx.yyyyy'
let sendNotification = async () => {
    for (let i = 0; i < 2; i++) {
        // Prepare a new notification
        let notification = new apn.Notification();

        // Specify your iOS app's Bundle ID (accessible within the project editor)
        notification.topic = bundleID;

        // Set expiration to 1 hour from now (in case device is offline)
        notification.expiry = Math.floor(Date.now() / 1000) + 3600;

        // Set app badge indicator
        notification.badge = 1;

        // Play ping.aiff sound when the notification is received
        notification.sound = 'ping.aiff';

        // Display the following message (the actual notification text, supports emoji)
        notification.alert = i + '我们已经帮您收集到了XX金币，快回来看看吧！ \u270C';

        // Send any extra payload data with the notification which will be accessible to your app in didReceiveRemoteNotification
        notification.payload = { id: 123 };

        // Actually send the notification
        try {
            let result = await apnProvider.send(notification, deviceToken)
            console.log("发送结果，", result)
        } catch (e) {
            console.log(e);
        }
    }
}
sendNotification();
