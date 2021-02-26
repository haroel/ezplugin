//
//  PluginTopon.m
//  globalTycoon-mobile
//
//  Created by howe on 2020/7/10.
// 激励视频广告系统
//

#import "PluginTopon.h"
#import "PluginCore.h"
#import "../utils/OCExport.h"
#import "JSONUtil.h"
#import <AdSupport/AdSupport.h>


#import <AnyThinkSDK/ATAPI.h>
#import <AnyThinkSDK/ATAdManager.h>
#import <AnyThinkRewardedVideo/AnyThinkRewardedVideo.h>
#import <AppTrackingTransparency/AppTrackingTransparency.h>

@implementation PluginTopon

-(void)initPlugin:(NSDictionary*)params{
    if ( self.isInited){
        return;
    }
    NSString *appid = [params objectForKey:@"appID"];
    NSString *appkey = [params objectForKey:@"appKey"];
    self.placementID = [params objectForKey:@"placementID"];
    
    #if defined(COCOS2D_DEBUG) && (COCOS2D_DEBUG > 0)
        [ATAPI setLogEnabled:YES];//Turn on debug logs
        [ATAPI integrationChecking];
        self.isDebug = YES;
        NSLog(@"【PluginTopon】调试模式，使用topon测试号");
        appid = @"a5b0e8491845b3";
        appkey = @"7eae0567827cfe2b22874061763f30c9";
        self.placementID = @"b5b44a0d05d707";
        NSLog(@"【PluginTopon】使用Vungle奖励视频广告！%@ ",self.placementID);
    #else
        [ATAPI setLogEnabled:NO];//Turn on debug logs
        self.isDebug = NO;
    #endif
    self.adInLoading = 0;
    self.adCacheCount = 0;
    self.resultMsg = [NSMutableDictionary dictionary];

    ATAPI *api =[ATAPI sharedInstance];
    if (@available(iOS 14, *)) {
        //iOS 14
        [ATTrackingManager requestTrackingAuthorizationWithCompletionHandler:^(ATTrackingManagerAuthorizationStatus status) {
            NSString *idfa = [[[ASIdentifierManager sharedManager] advertisingIdentifier] UUIDString];
            NSLog(@"【PluginTopon】IDFA= %@",idfa);
            if (api.appID == nil){
                [api startWithAppID:appid appKey:appkey error:nil];
            }
            [self createAndLoadRewardedAD];
            
        }];
    } else {
        NSString *idfa = [[[ASIdentifierManager sharedManager] advertisingIdentifier] UUIDString];
        NSLog(@"【PluginTopon】IDFA= %@",idfa);
        if (api.appID == nil){
            [api startWithAppID:appid appKey:appkey error:nil];
        }
        [self createAndLoadRewardedAD];
    }
}

-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback{
    
    if ( [type isEqualToString:@"hasReward"] ){
        if ([[ATAdManager sharedManager] rewardedVideoReadyForPlacementID:self.placementID]) {
            //Show rv here
            [self nativeCallbackHandler:callback andParams:@"1"];
        } else {
            [self nativeCallbackHandler:callback andParams:@"0"];
        }
    }else if ( [type isEqualToString:@"loadAd"] ){
        NSLog(@"【PluginTopon】PluginTopon->excute loadAd ");
        if ([[ATAdManager sharedManager] rewardedVideoReadyForPlacementID:self.placementID]) {
            //Show rv here
            [self nativeCallbackHandler:callback andParams:@"1"];
        } else {
            [self createAndLoadRewardedAD];
            [self nativeCallbackHandler:callback andParams:@"0"];
        }
    }else if ( [type isEqualToString:@"play"] ){
        NSLog(@"【PluginTopon】PluginTopon->excute play ");
        [ self.resultMsg removeAllObjects];
        [ self.resultMsg setObject:[NSNumber numberWithBool:self.isDebug] forKey:@"isDebug"];
        [ self.resultMsg setObject:params forKey:@"rewardId"];

        if ([[ATAdManager sharedManager] rewardedVideoReadyForPlacementID:self.placementID]) {
            UIWindow * window = [[UIApplication sharedApplication] keyWindow];
            [[ATAdManager sharedManager] showRewardedVideoWithPlacementID:self.placementID inViewController:window.rootViewController delegate:self];
            self.adCacheCount--;
//            [self createAndLoadRewardedAD];
            [self nativeCallbackHandler:callback andParams:@"1"];
        } else {
            self.adCacheCount = 0;
            [self createAndLoadRewardedAD];
            [self.resultMsg setObject:@"NotLoadedYet" forKey:@"msg"];
            [self nativeEventHandler:@"error" andParams:[JSONUtil stringify:self.resultMsg] ];
            [self nativeCallbackHandler:callback andParams:@"0"];

        }
        
    }
}

-(void)createAndLoadRewardedAD{
    if (![[ATAdManager sharedManager] rewardedVideoReadyForPlacementID:self.placementID]) {
        self.adInLoading++;
        NSString * userid = [OCExport getUUID];
        [[ATAdManager sharedManager] loadADWithPlacementID:self.placementID extra:@{kATAdLoadingExtraUserIDKey:userid} delegate:self];
    }
}
#pragma mark - loading delegate
-(void) didFinishLoadingADWithPlacementID:(NSString *)placementID {
    NSLog(@"【PluginTopon】RV Demo: didFinishLoadingADWithPlacementID %@",placementID);
    self.adInLoading--;
    self.adCacheCount++;
    [self nativeEventHandler:@"ready" andParams:@"1"];
}

-(void) didFailToLoadADWithPlacementID:(NSString* )placementID error:(NSError *)error {
    NSLog(@"【PluginTopon】RV Demo: %@ failed to load:%@",placementID, error);
    self.adInLoading--;
}

#pragma mark - showing delegate
-(void) rewardedVideoDidRewardSuccessForPlacemenID:(NSString *)placementID extra:(NSDictionary *)extra{
    NSLog(@"【PluginTopon】ATRewardedVideoVideoViewController::rewardedVideoDidRewardSuccessForPlacemenID:%@ extra:%@",placementID,extra);
    
    NSString * rewardType =[extra objectForKey:@"scenario_reward_name"];
    if (rewardType == nil){
        rewardType =[extra objectForKey:@"placement_reward_name"];
    }
    id rewardAmount =[extra objectForKey:@"placement_reward_number"];
    if (rewardAmount == nil){
        rewardAmount =[extra objectForKey:@"scenario_reward_number"];
    }
    [self.resultMsg setObject:rewardType forKey:@"rewardType"];
    [self.resultMsg setObject:rewardAmount forKey:@"rewardAmount"];
    [self nativeEventHandler:@"earned" andParams:[JSONUtil stringify:self.resultMsg]];
}

-(void) rewardedVideoDidStartPlayingForPlacementID:(NSString *)placementID extra:(NSDictionary *)extra {
    NSLog(@"【PluginTopon】ATRewardedVideoVideoViewController::rewardedVideoDidStartPlayingForPlacementID:%@ extra:%@", placementID, extra);
    if (self.resultMsg){
        [self nativeEventHandler:@"open" andParams:[JSONUtil stringify:self.resultMsg]];
    }
}

-(void) rewardedVideoDidEndPlayingForPlacementID:(NSString*)placementID extra:(NSDictionary *)extra {
    NSLog(@"【PluginTopon】ATRewardedVideoVideoViewController::rewardedVideoDidEndPlayingForPlacementID:%@ extra:%@", placementID, extra);
}

-(void) rewardedVideoDidFailToPlayForPlacementID:(NSString*)placementID error:(NSError*)error extra:(NSDictionary *)extra {
    NSLog(@"【PluginTopon】ATRewardedVideoVideoViewController::rewardedVideoDidFailToPlayForPlacementID:%@ error:%@ extra:%@", placementID, error, extra);
    if (self.resultMsg){
        [self.resultMsg setObject:@(error.code) forKey:@"code"];
        [self nativeEventHandler:@"error" andParams:[JSONUtil stringify:self.resultMsg]];
    }
}

-(void) rewardedVideoDidCloseForPlacementID:(NSString*)placementID rewarded:(BOOL)rewarded extra:(NSDictionary *)extra {
    NSLog(@"【PluginTopon】ATRewardedVideoVideoViewController::rewardedVideoDidCloseForPlacementID:%@, rewarded:%@ extra:%@", placementID, rewarded ? @"yes" : @"no", extra);
    if (self.resultMsg){
        id networkFirmId = [extra objectForKey:@"network_firm_id"];
        if(networkFirmId != nil){
            [self.resultMsg setObject:networkFirmId forKey:@"networkFirmId"];
        }
        NSString *adSourceId = [extra objectForKey:@"adsource_id"];
        if(adSourceId != nil){
            [self.resultMsg setObject:adSourceId forKey:@"adSourceId"];
        }
        id showId = [extra objectForKey:@"id"];
        if(showId != nil){
            [self.resultMsg setObject:showId forKey:@"showId"];
        }
        [self nativeEventHandler:@"close" andParams:[JSONUtil stringify:self.resultMsg]];
    }
    [self createAndLoadRewardedAD];
}

-(void) rewardedVideoDidClickForPlacementID:(NSString*)placementID extra:(NSDictionary *)extra {
    NSLog(@"【PluginTopon】ATRewardedVideoVideoViewController::rewardedVideoDidClickForPlacementID:%@ extra:%@", placementID, extra);
    if (self.resultMsg){
        [self nativeEventHandler:@"click" andParams:[JSONUtil stringify:self.resultMsg]];
    }
}

@end
