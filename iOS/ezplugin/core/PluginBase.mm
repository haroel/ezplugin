//
//  PluginBase.m
//  Game
//
//  Created by howe on 2017/9/4.
//
//

#import "PluginBase.h"
#import "PluginCore.h"

@implementation PluginBase

-(void)setInited:(BOOL)val{
    self.isInited = val;
}

-(void)initPlugin:(NSDictionary*)params{
    
}

-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback{
    
}

-(BOOL)handleOpenURL:(NSURL*)url{
    return NO;
}
-(BOOL)openURL:(NSURL*)url{
    return NO;
}
-(BOOL)continueUserActivity:(nonnull NSUserActivity *)userActivity{
    return NO;
}
- (BOOL)application:(UIApplication *)application
            openURL:(NSURL *)url
            options:(NSDictionary<UIApplicationOpenURLOptionsKey,id> *)options{
    return NO;
}

// iOS应用生命周期
- (void)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions{
    
}
- (void)applicationDidBecomeActive:(UIApplication *)application{
    
}
- (void)applicationWillResignActive:(UIApplication *)application{
    
}
- (void)applicationDidEnterBackground:(UIApplication *)application{
    
}
- (void)applicationWillEnterForeground:(UIApplication *)application{
    
}
- (void)applicationWillTerminate:(UIApplication *)application{
    
}
- (void)didRegisterForRemoteNotificationsWithDeviceToken:(NSData*)deviceToken{
    
}
- (void)didReceiveRemoteNotification:(NSDictionary *)userInfo fetchCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler{
    
}
-(void)nativeCallbackErrorHandler:(int)callId andParams:(NSString*)params{
    [[PluginCore shareInstance] nativeCallbackErrorHandler:callId andParams:params];
}
-(void)nativeCallbackHandler:(int)callId andParams:(NSString*)params{
    [[PluginCore shareInstance] nativeCallbackHandler:callId andParams:params];
}
-(void)nativeEventHandler:(NSString*)event andParams:(NSString*)params{
    [[PluginCore shareInstance] nativeEventHandler:self.pluginName andEvent:event andParams:params];
}

@end
