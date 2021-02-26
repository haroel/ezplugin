//
//  PluginCore.m
//  Game
//
//  Created by howe on 2017/9/4.
//
//
#import "PluginCore.h"
#import "PluginBase.h"
// cocos 模块头文件
#include "platform/CCApplication.h"
#include "base/CCScheduler.h"
#include "cocos/scripting/js-bindings/jswrapper/SeApi.h"

#import "../utils/JSONUtil.h"
#import "../utils/OCExport.h"

NSMutableDictionary *_pluginHash = [[NSMutableDictionary alloc] init];
NSMutableDictionary *_globalVariables = [[NSMutableDictionary alloc] init];

@implementation PluginCore

#pragma mark -
#pragma mark Singleton

static PluginCore *mInstace = nil;

+ (instancetype)shareInstance {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        mInstace = [[super allocWithZone:NULL] init];
    });
    return mInstace;
}
+ (id)allocWithZone:(struct _NSZone *)zone {
    return [PluginCore shareInstance];
}

+ (id)copyWithZone:(struct _NSZone *)zone {
    return [PluginCore shareInstance];
}


+(void)alert:(NSString*)title andMsg:(NSString*)msg
{
    UIAlertController* alertController = [UIAlertController alertControllerWithTitle:title message:msg preferredStyle:UIAlertControllerStyleAlert];
    UIAlertAction *closeAct = [UIAlertAction actionWithTitle:@"OK" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
    }];
    [alertController addAction:closeAct];
    UIWindow * window = [[UIApplication sharedApplication] keyWindow];
    [window.rootViewController presentViewController:alertController animated:YES completion:^{
    }];
}

-(void)setGlobalVariable:(NSString*)key andValue:(id)value{
    [_globalVariables setObject:value forKey:key];
}

-(id)getGlobalVariable:(NSString*)key{
    return [_globalVariables objectForKey:key];
}
-(void)removeGlobalVariable:(NSString*)key{
    [_globalVariables removeObjectForKey:key];
}

-(void)cleanGlobalVariables{
    [_globalVariables removeAllObjects];
}

-(void)initCore:(NSDictionary*)launchOptions{
    NSDictionary * infoDict = [[NSBundle mainBundle] infoDictionary];
    NSMutableDictionary * sysInfo = [[NSMutableDictionary alloc] init];
    [sysInfo setObject:infoDict forKey:@"infoConfig"];
    [sysInfo setObject:infoDict[@"CFBundleShortVersionString"] forKey:@"appVersion"];
    [sysInfo setObject:infoDict[@"CFBundleVersion"] forKey:@"buildVersion"];
    [sysInfo setObject:infoDict[@"CFBundleIdentifier"] forKey:@"packageName"];
    [sysInfo setObject:infoDict[@"CFBundleDisplayName"] forKey:@"appName"];
    [sysInfo setObject:[NSString stringWithFormat:@"%f",[OCExport getOSVer] ] forKey:@"osVersion"];
    [sysInfo setObject:[OCExport getUUID] forKey:@"uuid"];
    
    if (launchOptions != nil){
        NSURL *urlKey = [launchOptions objectForKey:UIApplicationLaunchOptionsURLKey];
        if ( urlKey != nil ){
            [sysInfo setObject:urlKey.absoluteString forKey:@"launchURL"];
        }
        [_globalVariables setObject:[NSDictionary dictionaryWithDictionary:launchOptions] forKey:@"launchOptions"];
    }
    NSString *ret = [JSONUtil stringify:sysInfo];
    [_globalVariables setObject:ret forKey:@"sysInfo"];
    NSArray *initialPlugins =[infoDict objectForKey:@"EzpluginPlugins"];
    if (initialPlugins != nil){
        NSLog(@"PluginCore initialPlugins %@",initialPlugins);
        // 初始化插件
        for (NSString * pluginName in initialPlugins) {
            [[PluginCore shareInstance] __createPlugin:pluginName andParams:nil];
        }
    }
    NSLog(@"PluginCore initCore");
}

+(NSString*)sysInfo{
    NSString *ret = [_globalVariables objectForKey:@"sysInfo"];
    NSLog(@"PluginCore sysInfo %@",ret);
    return ret;
}

+(void)initAllPlugins:(NSString *)jsonstr andCallback:(NSNumber*)cb
{
    NSArray *arr = [JSONUtil parse:jsonstr];
    if (arr != nil){
        for (int i=0;i<arr.count;i++){
            id pluginObj = [arr objectAtIndex:i];
            NSString *pluginName = [pluginObj objectForKey:@"pluginName"];
            [[PluginCore shareInstance] __createPlugin:pluginName andParams:[pluginObj objectForKey:@"params"] ];
        }
        NSArray *keys = [_pluginHash allKeys ];
        NSString *result = [keys componentsJoinedByString:@"&"];
        NSLog(@"[PluginCore]->initAllPlugins finish! %@",result);
        [[PluginCore shareInstance] nativeCallbackHandler:[cb intValue] andParams:result];
    }else{
        NSString * result= [NSString stringWithFormat:@"jsonerror:%@",jsonstr];
        NSLog(@"[PluginCore]->initAllPlugins %@",result);
        [[PluginCore shareInstance] nativeCallbackErrorHandler:[cb intValue] andParams:result];
    }
}

+(void)initPlugin:(NSString*)pluginName andPluginData:(NSString*)jsonstr andCallback:(NSNumber*)cb
{
    id dict = [JSONUtil parse:jsonstr];
    if (dict != nil){
        PluginBase *plugin = [[PluginCore shareInstance] __createPlugin:pluginName andParams:dict];
        if (plugin){
            [[PluginCore shareInstance] nativeCallbackHandler:[cb intValue] andParams:@"1"];
            return;
        }
        NSLog(@"[PluginCore] initPlugin error plugin create failed");
        [[PluginCore shareInstance] nativeCallbackErrorHandler:[cb intValue] andParams:@"initPlugin error plugin create failed"];
    }else{
        NSString * result= [NSString stringWithFormat:@"jsonerror:%@",jsonstr];
        NSLog(@"[PluginCore] initPlugin %@",result);
        [[PluginCore shareInstance] nativeCallbackErrorHandler:[cb intValue] andParams:result];
    }
}

-(PluginBase*)__createPlugin:(NSString*)pluginName andParams:(id)dict{
    PluginBase *plugin = [_pluginHash objectForKey:pluginName];
    if ( plugin == nil ){
        Class PluginClass = NSClassFromString( pluginName );
        if (PluginClass != nil){
            plugin = [[PluginClass alloc] init];
            [plugin setPluginName:pluginName];
            [plugin setInited:NO];
            [_pluginHash setObject:plugin forKey:pluginName];
        }else{
           NSLog(@"[PluginCore] ->__createPlugin Error: %@ class not found!",pluginName);
        }
    }
    if (plugin == nil){
        return nil;
    }
    [plugin initPlugin:dict];
    [plugin setInited:YES];
    NSLog(@"[PluginCore] __createPlugin %@",pluginName);
    return plugin;
}

+(void)excute:(NSString *)pluginName andAction:(NSString *)act andParams:(NSString *)params andCallBack:(NSNumber*)cb
{
    dispatch_async(dispatch_get_main_queue(), ^{
        int callback = [cb intValue];
       PluginBase* _plugin = [_pluginHash objectForKey:pluginName];
       if (_plugin != nil){
           [_plugin excute:act andParams:params andCallback:callback];
       }else{
           NSLog(@"[PluginCore] Error: %@ plugin not found!",pluginName);
           [[PluginCore shareInstance] nativeCallbackErrorHandler:callback andParams:@"Error:Plugin not found!"];
       }
   });
}

#pragma mark -
#pragma mark App delegate回调
/**
 app显示给用户之前执行最后的初始化操作
 */
- (void)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    [self initCore:launchOptions];
    for (id key in _pluginHash) {
        PluginBase* sdk = [_pluginHash objectForKey:key];
        [sdk application:application didFinishLaunchingWithOptions:launchOptions];
    }
}

/**
 app已经切换到active状态后需要执行的操作
 */
- (void)applicationDidBecomeActive:(UIApplication *)application {
    for (id key in _pluginHash) {
        PluginBase* sdk = [_pluginHash objectForKey:key];
        [sdk applicationDidBecomeActive:application];
    }
}
/**
 app将要从前台切换到后台时需要执行的操作
 */
- (void)applicationWillResignActive:(UIApplication *)application {
    for (id key in _pluginHash) {
        PluginBase* sdk = [_pluginHash objectForKey:key];
        [sdk applicationDidBecomeActive:application];
    }

}
/**
 app已经进入后台后需要执行的操作
 */
- (void)applicationDidEnterBackground:(UIApplication *)application {
    for (id key in _pluginHash) {
        PluginBase* sdk = [_pluginHash objectForKey:key];
        [sdk applicationDidEnterBackground:application];
    }
}
/**
 app将要从后台切换到前台需要执行的操作，但app还不是active状态
 */
- (void)applicationWillEnterForeground:(UIApplication *)application {
    for (id key in _pluginHash) {
        PluginBase* sdk = [_pluginHash objectForKey:key];
        [sdk applicationWillEnterForeground:application];
    }
}
/**
 app将要结束时需要执行的操作
 */
- (void)applicationWillTerminate:(UIApplication *)application {
    for (id key in _pluginHash) {
        PluginBase* sdk = [_pluginHash objectForKey:key];
        [sdk applicationWillTerminate:application];
    }
}
-(BOOL)handleOpenURL:(NSURL*)url
{
    for (id key in _pluginHash) {
        id obj = [_pluginHash objectForKey:key];
        if ( [obj handleOpenURL:url]){
            return YES;
        }
    }
    return FALSE;
}
-(BOOL)openURL:(NSURL*)url
{
    for (id key in _pluginHash) {
        id obj = [_pluginHash objectForKey:key];
        if ( [obj openURL:url]){
            return YES;
        }
    }
    return NO;
}
-(BOOL)continueUserActivity:(nonnull NSUserActivity *)userActivity
{
    for (id key in _pluginHash) {
        id obj = [_pluginHash objectForKey:key];
        if ( [obj continueUserActivity:userActivity]){
            return YES;
        }
    }
    return NO;
}

-(BOOL)application:(UIApplication *)application
           openURL:(NSURL *)url
           options:(NSDictionary<UIApplicationOpenURLOptionsKey,id> *)options{
    BOOL ret = NO;
    for (id key in _pluginHash) {
        id obj = [_pluginHash objectForKey:key];
        if ( [obj application:application openURL:url options:options]){
            ret = YES;
        }
    }
    return ret;
}

- (void)didRegisterForRemoteNotificationsWithDeviceToken:(NSData*)deviceToken{
    PluginBase *firebasePlugin = [_pluginHash objectForKey:@"PluginAPNs"];
    if (firebasePlugin != nil){
        [firebasePlugin didRegisterForRemoteNotificationsWithDeviceToken:deviceToken];
    }
}
- (void)didReceiveRemoteNotification:(NSDictionary *)userInfo
              fetchCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler{
    PluginBase *firebasePlugin = [_pluginHash objectForKey:@"PluginAPNs"];
    if (firebasePlugin != nil){
        [firebasePlugin didReceiveRemoteNotification:userInfo fetchCompletionHandler:completionHandler];
    }
}
//-(NSString*)toBase64:(NSString*)str{
//    NSData *data = [str dataUsingEncoding:NSUTF8StringEncoding];
//    NSData *base64Data = [data base64EncodedDataWithOptions:0];
//    NSString *baseString = [[NSString alloc]initWithData:base64Data encoding:NSUTF8StringEncoding];
//
//    NSString *temp = [baseString stringByReplacingOccurrencesOfString:@" " withString:@""];
//        temp = [temp stringByReplacingOccurrencesOfString:@"\r" withString:@""];
//        temp = [temp stringByReplacingOccurrencesOfString:@"\n" withString:@""];
//    return temp;
//}

#pragma mark -
#pragma mark objc 回调cocos js runtime

-(NSString*)_stringToParams:(NSString*)params{
    if (params != nil){
        if (params.length == 0){
            return params;
        }
        NSMutableArray *result = [NSMutableArray array];
        for(int index = 0; index < [params length]; index++){
            NSString *charStr = [NSString stringWithFormat:@"%d",[params characterAtIndex:index]];
            [result addObject:charStr];
        }
        return [result componentsJoinedByString:@"|"];
    }
    return @"";
}

-(void)nativeEventHandler:(NSString*)pluginName andEvent:(NSString*)event andParams:(NSString*)params
{
    NSString * _params = [self _stringToParams:params];
    NSString *str = [NSString stringWithFormat:@"ezplugin.nativeEventHandler('%@','%@','%@');",pluginName,event,_params];
    std::string jsstr([str UTF8String]);
    auto scheduler = cocos2d::Application::getInstance()->getScheduler();
    scheduler->performFunctionInCocosThread([jsstr](){
        se::ScriptEngine::getInstance()->evalString(jsstr.c_str());
    });
}

-(void)nativeCallbackHandler:(int)callId andParams:(NSString*)params
{
    if (callId == 0){
        return;
    }
    NSString * _params = [self _stringToParams:params];
    NSString *str = [NSString stringWithFormat:@"ezplugin.nativeCallbackHandler('%d','%@');",callId,_params];
    std::string jsstr([str UTF8String]);
    auto scheduler = cocos2d::Application::getInstance()->getScheduler();
    scheduler->performFunctionInCocosThread([jsstr](){
        se::ScriptEngine::getInstance()->evalString(jsstr.c_str());
    });
}

-(void)nativeCallbackErrorHandler:(int)callId andParams:(NSString*)params
{
    if (callId == 0){
        return;
    }
    NSString * _params = [self _stringToParams:params];
    NSString *str = [NSString stringWithFormat:@"ezplugin.nativeCallbackErrorHandler('%d','%@');",callId,_params];
    std::string jsstr([str UTF8String]);
    auto scheduler = cocos2d::Application::getInstance()->getScheduler();
    scheduler->performFunctionInCocosThread([jsstr](){
        se::ScriptEngine::getInstance()->evalString(jsstr.c_str());
    });
}
@end
