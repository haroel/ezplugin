//
//  PluginBugly.m
//  epicslots-mobile
//
//  Created by howe on 2020/7/28.
//

#import "PluginBugly.h"
#import <Bugly/Bugly.h>
#include "cocos/scripting/js-bindings/jswrapper/SeApi.h"
#import "JSONUtil.h"

@implementation PluginBugly
-(void)initPlugin:(NSDictionary*)params{
    if ( self.isInited){
        return;
    }
    BuglyConfig * config = [[BuglyConfig alloc] init];
    #if defined(COCOS2D_DEBUG) && (COCOS2D_DEBUG > 0)
        config.debugMode = NO;
    #else
        config.debugMode = NO;
    #endif
    
    NSDictionary * infoDict = [[NSBundle mainBundle] infoDictionary];
    NSString *appid =[infoDict objectForKey:@"BuglyAppIDString"];
    // appid 自动从info.plist读取
    [Bugly startWithAppId:appid config:config];

    se::ScriptEngine::getInstance()->setExceptionCallback( [](const char* location, const char* message, const char* stack){
        NSString *loc = [NSString stringWithUTF8String:location];
        NSString *msg = [NSString stringWithUTF8String:message];
        NSString *ostack = [NSString stringWithUTF8String:stack];
        NSDictionary *userInfo = @{
            @"location": loc,
            @"message": msg,
            @"stack":ostack
        };
        NSError *error = [NSError errorWithDomain:@"JSRunTimeError" code:1001 userInfo:userInfo];
        [Bugly reportError:error];
    });
}

-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback{
    if ([type isEqualToString:@"userid"]){
        [Bugly setUserIdentifier:params];
//        NSArray *arr = @[@"lnj", @"lmj", @"jjj"];
//        NSLog(@"测试OC 崩溃 %@",[arr objectAtIndex:4]);
    }else if ([type isEqualToString:@"log"]){
        NSDictionary *dict = [JSONUtil parse:params];
        if (dict){
            NSString *msg =[dict objectForKey:@"msg"];
            NSString *errormsg =[dict objectForKey:@"error"];
            NSDictionary *userInfo = @{
                @"location": @"js",
                @"message": msg,
                @"error":errormsg
            };
            NSError *error = [NSError errorWithDomain:@"CCError" code:1002 userInfo:userInfo];
            [Bugly reportError:error];
        }
    }
}

@end
