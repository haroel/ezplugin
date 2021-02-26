//
//  PluginCore.h
//  Game
//
//  Created by howe on 2017/9/4.
//
//

#import <Foundation/Foundation.h>

@interface PluginCore : NSObject
@property(nonatomic,strong) NSString *name;
+ (instancetype)shareInstance;
// iOS应用生命周期
- (void)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions;
- (void)applicationDidBecomeActive:(UIApplication *)application;
- (void)applicationWillResignActive:(UIApplication *)application;
- (void)applicationDidEnterBackground:(UIApplication *)application;
- (void)applicationWillEnterForeground:(UIApplication *)application;
- (void)applicationWillTerminate:(UIApplication *)application;
- (void)didRegisterForRemoteNotificationsWithDeviceToken:(NSData*)deviceToken;
- (void)didReceiveRemoteNotification:(NSDictionary *)userInfo fetchCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler;
/**
 弹窗
 **/
+(void)alert:(NSString*)title andMsg:(NSString*)msg;

/**** 定义工具方法  end ****/

+(NSString*)sysInfo;

+(void)initAllPlugins:(NSString *)jsonString andCallback:(NSNumber*)callback;

+(void)initPlugin:(NSString*)pluginName andPluginData:(NSString*)params andCallback:(NSNumber*)callback;

+(void)excute:(NSString*)pluginName andAction:(NSString*)act andParams:(NSString*)params andCallBack:(NSNumber*)callback;

/** 回调js**/
-(void)nativeCallbackErrorHandler:(int)callId andParams:(NSString*)params;
-(void)nativeCallbackHandler:(int)callId andParams:(NSString*)params;
-(void)nativeEventHandler:(NSString*)pluginName andEvent:(NSString*)event andParams:(NSString*)params;

-(BOOL)handleOpenURL:(NSURL*)url;
-(BOOL)openURL:(NSURL*)url;
-(BOOL)continueUserActivity:(nonnull NSUserActivity *)userActivity;

-(BOOL)application:(UIApplication *)application
            openURL:(NSURL *)url
            options:(NSDictionary<UIApplicationOpenURLOptionsKey,id> *)options;

/** 获取全局数据 **/
-(id)getGlobalVariable:(NSString*)key;
/** 保存全局数据 **/
-(void)setGlobalVariable:(NSString*)key andValue:(id)value;
-(void)removeGlobalVariable:(NSString*)key;
-(void)cleanGlobalVariables;

@end
