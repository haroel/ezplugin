//
//  PluginAPNs.m
//  globalTycoon-mobile
//
//  Created by howe on 2020/9/27.
//

#import "PluginAPNs.h"

@implementation PluginAPNs

-(void)initPlugin:(NSDictionary*)params{
    if ( self.isInited){
        return;
    }
    UIApplication *application = [UIApplication sharedApplication];
    [application setApplicationIconBadgeNumber:0]; //清除角标
    if (@available(iOS 10.0, *)) {
        UNUserNotificationCenter * center = [UNUserNotificationCenter currentNotificationCenter];
        [center setDelegate:self];
        UNAuthorizationOptions type = UNAuthorizationOptionBadge|UNAuthorizationOptionSound|UNAuthorizationOptionAlert;
        [center requestAuthorizationWithOptions:type completionHandler:^(BOOL granted, NSError * _Nullable error) {
            if (granted) {
                NSLog(@"PluginAPNs 注册成功");
            }else{
                NSLog(@"PluginAPNs 注册失败 %@",error);
            }
        }];
    }else {
        UIUserNotificationType notificationTypes = UIUserNotificationTypeBadge |UIUserNotificationTypeSound |UIUserNotificationTypeAlert;
        UIUserNotificationSettings *settings = [UIUserNotificationSettings settingsForTypes:notificationTypes categories:nil];
        [application registerUserNotificationSettings:settings];
    }
    // 注册获得device Token
    [application registerForRemoteNotifications];
}
- (void)didRegisterForRemoteNotificationsWithDeviceToken:(NSData*)deviceToken{
    const unsigned *tokenBytes = [deviceToken bytes];
    NSString *hexToken = [NSString stringWithFormat:@"%08x%08x%08x%08x%08x%08x%08x%08x",
    ntohl(tokenBytes[0]), ntohl(tokenBytes[1]), ntohl(tokenBytes[2]),
    ntohl(tokenBytes[3]), ntohl(tokenBytes[4]), ntohl(tokenBytes[5]),
    ntohl(tokenBytes[6]), ntohl(tokenBytes[7])];
    NSLog(@"PluginAPNs didRegisterForRemoteNotificationsWithDeviceToken %@",hexToken);
    [self nativeEventHandler:@"device_token" andParams:hexToken];
}

/**
 *iOS10
 * Tells the delegate that a background notification has arrived. 即APP处于挂起、kill状态都会触发这个方法。触发 与alert显示情况与方法1相同：处于前台时，不alert，直接调用；挂起或kill状态，点击通知会触发
 * 实现此方法来处理传入的后台通知。当通知到达时，系统会启动您的应用程序或将其从挂起状态唤醒，您的应用程序会收到在后台运行的一小段时间。
 * 可以使用后台执行时间处理通知并下载其相关内容。一旦完成对通知的处理，就必须调用fetchCompletionHandler完成处理程序。您的应用程序有30秒的挂钟时间来处理通知并调用处理程序，时间到了系统会终止您的应用程序。请尽可能快地调用处理程序，因为系统会跟踪应用程序后台通知的运行时间、功耗和数据成本。
 * 后台通知是低优先级的，系统根据目标设备的功率考虑限制这些通知。APNs不保证设备会收到推送通知，而那些在处理后台通知时消耗大量能量或时间的应用程序可能不会收到后台时间来处理未来的通知。
 * 应用程序因为远程通知而启动或恢复，也将调用此方法。注意，此方法与方法1冲突，如果实现了此方法，则不会调用方法1
 */
- (void)didReceiveRemoteNotification:(NSDictionary *)userInfo fetchCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler{
    NSLog(@"PluginAPNs didReceiveRemoteNotification %@",userInfo);
    // 前台收到远程推送数据
    completionHandler(UIBackgroundFetchResultNewData);
}



#pragma mark -
#pragma mark UNUserNotificationCenterDelegate 回调
//接收iOS 10设备的显示通知。
//当app在前台时处理传入的通知消息。
- (void)userNotificationCenter:(UNUserNotificationCenter *)center
       willPresentNotification:(UNNotification *)notification
         withCompletionHandler:(void (^)(UNNotificationPresentationOptions))completionHandler {
    NSDictionary *userInfo = notification.request.content.userInfo;
    
    //当swizzling被禁用时，你必须让消息传递知道消息，以便进行分析
    // [FIRMessaging消息]appdid应收款emessage:userInfo];
    
    //打印消息ID。
//    if (userInfo[kGCMMessageIDKey]) {
//        NSLog(@"Message ID: %@", userInfo[kGCMMessageIDKey]);
//    }
    
    // Print full message.
    NSLog(@"FCMController 前台收到消息 didReceiveRemoteNotification userInfo = %@", userInfo);

    //将此更改为您首选的显示选项
    if (@available(iOS 10.0, *)) {
        completionHandler(UNNotificationPresentationOptionNone);
    } else {
        // Fallback on earlier versions
    }
}

//后台运行: 指的是程序已经打开, 用户看不见程序的界面, 如锁屏和按Home键.
//程序退出: 指的是程序没有运行, 或者通过双击Home键,关闭了程序.**
- (void)userNotificationCenter:(UNUserNotificationCenter *)center
didReceiveNotificationResponse:(UNNotificationResponse *)response
         withCompletionHandler:(void(^)(void))completionHandler  API_AVAILABLE(ios(10.0)){
    NSDictionary *userInfo = response.notification.request.content.userInfo;
//    if (userInfo[kGCMMessageIDKey]) {
//        NSLog(@"Message ID: %@", userInfo[kGCMMessageIDKey]);
//    }
//
    // Print full message.
    NSLog(@"FCMController  2 前台收到消息 didReceiveNotificationResponse userInfo = %@", userInfo);

    completionHandler();
}

- (void)userNotificationCenter:(UNUserNotificationCenter *)center openSettingsForNotification:(nullable UNNotification *)notification{
    NSDictionary *userInfo = notification.request.content.userInfo;
        
        //当swizzling被禁用时，你必须让消息传递知道消息，以便进行分析
        // [FIRMessaging消息]appdid应收款emessage:userInfo];
        
        //打印消息ID。
    //    if (userInfo[kGCMMessageIDKey]) {
    //        NSLog(@"Message ID: %@", userInfo[kGCMMessageIDKey]);
    //    }
        
        // Print full message.
        NSLog(@"FCMController 收到userNotificationCenter 消息 openSettingsForNotification userInfo = %@", userInfo);
    
}
@end
