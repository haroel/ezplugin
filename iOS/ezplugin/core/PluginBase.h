//
//  PluginBase.h
//  Game
//
//  Created by howe on 2017/9/4.
//
//

#import <Foundation/Foundation.h>

@interface PluginBase : NSObject
{
}
//插件名
@property (nonatomic,copy ) NSString * pluginName;
@property (nonatomic) BOOL isInited;

-(void)setInited:(BOOL)val;

-(void)initPlugin:(NSDictionary*)params;

-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback;
#pragma mark -
#pragma mark objc 回调cocos js runtime
-(void)nativeCallbackErrorHandler:(int)callId andParams:(NSString*)params;
-(void)nativeCallbackHandler:(int)callId andParams:(NSString*)params;
-(void)nativeEventHandler:(NSString*)event andParams:(NSString*)params;

#pragma mark -
#pragma mark App delegate回调
-(BOOL)handleOpenURL:(NSURL*)url;
-(BOOL)openURL:(NSURL*)url;
-(BOOL)continueUserActivity:(nonnull NSUserActivity *)userActivity;
- (BOOL)application:(UIApplication *)application
            openURL:(NSURL *)url
            options:(NSDictionary<UIApplicationOpenURLOptionsKey,id> *)options;
// iOS应用生命周期
- (void)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions;
- (void)applicationDidBecomeActive:(UIApplication *)application;
- (void)applicationWillResignActive:(UIApplication *)application;
- (void)applicationDidEnterBackground:(UIApplication *)application;
- (void)applicationWillEnterForeground:(UIApplication *)application;
- (void)applicationWillTerminate:(UIApplication *)application;
- (void)didRegisterForRemoteNotificationsWithDeviceToken:(NSData*)deviceToken;
- (void)didReceiveRemoteNotification:(NSDictionary *)userInfo fetchCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler;

@end
