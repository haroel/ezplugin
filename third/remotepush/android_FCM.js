
var admin = require("firebase-admin");
// 每个项目都不一样
const GOOGLE_APPLICATION_CREDENTIALS = "global-tycoon-firebase-adminsdk-k5ob5-39f049e187.json"

var serviceAccount = require('./firebase_server_account/' + GOOGLE_APPLICATION_CREDENTIALS);

admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    // databaseURL: "https://global-tycoon.firebaseio.com"
});
let sendNotification = () => {
    const registrationTokens = [
        // 设备码
        'djhxFYu8SLWL3nJt0uR2Tp:APA91bEicJ5l17ZHH_pjAInRHherRC7zs9rPhS-_ZPgys9WOdpZTgP-w6Vp_c_htGrShqYQr5EUJweIGeiy7x8ayWI3su-n0c9N6KCbbLbBSWvyP1TW1mWs6vFurvecLZhwZhrIH9hbw',
    ];
    const message = {
        data: { gold: '850' },
        notification: {
            title: '$FooCorp up 1.43% on the day',
            body: '$FooCorp gained 11.80 points to close at 835.67, up 1.43% on the day.'
        },
        tokens: registrationTokens,
    }
    console.log("发送消息")
    try {
        admin.messaging().sendMulticast(message).then((response) => {
            // Response is a message ID string.
            console.log('Successfully sent message:', response);
        })
            .catch((error) => {
                console.log('Error sending message:', error);
            });
    } catch (error) {
        console.error(error)
    }
}

sendNotification();