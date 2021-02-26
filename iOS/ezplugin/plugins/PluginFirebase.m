//
//  PluginFirebase.m
//  epicslots-mobile
//
//  Created by howe on 2020/6/11.
//

#import "PluginFirebase.h"
#import "PluginCore.h"

#import <FirebaseCore/FirebaseCore.h>
#import <FirebaseAnalytics/FIRAnalytics.h>

#import "JSONUtil.h"

@implementation PluginFirebase
-(void)initPlugin:(NSDictionary*)params{
    if ( self.isInited){
        return;
    }
    // 启动firebase
    [FIRApp configure];
    self.deepLinkURL = @"";
    NSDictionary * launchOptions = [[PluginCore shareInstance] getGlobalVariable:@"launchOptions"];
    if (launchOptions != nil){
        NSURL *urlKey = [launchOptions objectForKey:UIApplicationLaunchOptionsURLKey];
        if ( urlKey != nil ){
            self.deepLinkURL = urlKey.absoluteString;
        }
    }
}

-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback{
    
    if ( [type isEqualToString:@"getDynamicLink"] ){
        [self nativeCallbackHandler:callback andParams:self.deepLinkURL];
    }else if ( [type isEqualToString:@"setUserProperty"] ){
        NSDictionary *dict = [JSONUtil parse:params];
        if (dict){
            [FIRAnalytics setUserPropertyString:[dict objectForKey:@"uid" ] forName:@"uid"];
            [FIRAnalytics setUserPropertyString:[dict objectForKey:@"accountID" ] forName:@"accountID"];
            [FIRAnalytics setUserPropertyString:[dict objectForKey:@"accountPlatform" ] forName:@"accountPlatform"];
            [FIRAnalytics setUserPropertyString:[dict objectForKey:@"level" ] forName:@"level"];
        }
        [self nativeCallbackHandler:callback andParams:@""];
    }else if ( [type isEqualToString:@"log_event"] ){
        NSDictionary *dict = [JSONUtil parse:params];
        if (dict){
            NSString *event = [dict objectForKey:@"event"];
            NSString *key = [dict objectForKey:@"key"];
            NSString *value = [dict objectForKey:@"value"];
            if (key.length < 1 || value.length < 1){
                [FIRAnalytics logEventWithName:event
                                        parameters:nil];
            }else{
                [FIRAnalytics logEventWithName:event
                                   parameters:@{
                                                key: value
                                                }];
            }
        }
        [self nativeCallbackHandler:callback andParams:@""];
    }
}
- (void)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions{
    if (launchOptions != nil){
        NSURL *urlKey = [launchOptions objectForKey:UIApplicationLaunchOptionsURLKey];
        if ( urlKey != nil ){
            self.deepLinkURL = urlKey.absoluteString;
            [self nativeEventHandler:@"DynamicLinkChanged" andParams:self.deepLinkURL];
        }
    }
}
@end
