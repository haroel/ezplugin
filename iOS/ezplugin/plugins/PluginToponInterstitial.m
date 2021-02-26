//
//  PluginToponInterstitial.m
//  jigsaw-mobile
//
//  Created by howe on 2020/8/20.
// 插屏广告

#import "PluginToponInterstitial.h"

#import "PluginCore.h"
#import "../utils/OCExport.h"
#import "JSONUtil.h"
#import <AdSupport/AdSupport.h>


#import <AnyThinkSDK/ATAPI.h>
#import <AnyThinkSDK/ATAdManager.h>
#import <AnyThinkRewardedVideo/AnyThinkRewardedVideo.h>
@implementation PluginToponInterstitial

-(void)initPlugin:(NSDictionary*)params{
    NSString *idfa = [[[ASIdentifierManager sharedManager] advertisingIdentifier] UUIDString];
    NSLog(@"【PluginToponInterstitial】IDFA= %@",idfa);
    if ( self.isInited){
        return;
    }
    self.placementID = [params objectForKey:@"placementID"];
    #if defined(COCOS2D_DEBUG) && (COCOS2D_DEBUG > 0)
        self.placementID = @"b5c21a055a51ab";
        self.isDebug = YES;
        NSLog(@"【PluginToponInterstitial】使用Unity插屏广告！%@ ",self.placementID);
    #else
        self.isDebug = NO;
    #endif
    ATAPI *api =[ATAPI sharedInstance];
    if (api.appID == nil){
        NSString *appid = [params objectForKey:@"appID"];
        NSString *appkey = [params objectForKey:@"appKey"];
        if (self.isDebug){
            NSLog(@"【PluginToponInterstitial】调试模式，使用topon测试号");
            appid = @"a5b0e8491845b3";
            appkey = @"7eae0567827cfe2b22874061763f30c9";
        }
        [ATAPI setLogEnabled:self.isDebug];//Turn on debug logs
        if (appid && appkey){
            [api startWithAppID:appid appKey:appkey error:nil];
        }else{
            NSLog(@"【PluginToponInterstitial】Error 请传入appID和appKey来 初始化ATAPI！");
            return;
        }
    }
    self.adInLoading = 0;
    self.adCacheCount = 0;
    self.resultMsg = [NSMutableDictionary dictionary];
    [self createAndLoadRewardedAD];
}

-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback{
    
    if ( [type isEqualToString:@"hasReward"] ){
        if ([[ATAdManager sharedManager] interstitialReadyForPlacementID:self.placementID]) {
            //Show rv here
            [self nativeCallbackHandler:callback andParams:@"1"];
        } else {
            [self nativeCallbackHandler:callback andParams:@"0"];
        }
    }else if ( [type isEqualToString:@"loadAd"] ){
        NSLog(@"【PluginToponInterstitial】PluginTopon->excute loadAd ");
        if ([[ATAdManager sharedManager] interstitialReadyForPlacementID:self.placementID]) {
            //Show rv here
            [self nativeCallbackHandler:callback andParams:@"1"];
        } else {
            [self createAndLoadRewardedAD];
            [self nativeCallbackHandler:callback andParams:@"0"];
        }
    }else if ( [type isEqualToString:@"play"] ){
        NSLog(@"【PluginToponInterstitial】PluginTopon->excute play ");
        [ self.resultMsg removeAllObjects];
        [ self.resultMsg setObject:[NSNumber numberWithBool:self.isDebug] forKey:@"isDebug"];
        [ self.resultMsg setObject:params forKey:@"rewardId"];

        if ([[ATAdManager sharedManager] interstitialReadyForPlacementID:self.placementID]) {
            UIWindow * window = [[UIApplication sharedApplication] keyWindow];
            [[ATAdManager sharedManager] showInterstitialWithPlacementID:self.placementID inViewController:window.rootViewController delegate:self];
            self.adCacheCount--;
        } else {
            self.adCacheCount = 0;
            [self createAndLoadRewardedAD];
            [self.resultMsg setObject:@"NotLoadedYet" forKey:@"msg"];
            [self nativeEventHandler:@"error" andParams:[JSONUtil stringify:self.resultMsg] ];
        }
    }
}


-(void)createAndLoadRewardedAD{
    self.adInLoading++;
    NSString * userid = [OCExport getUUID];
    [[ATAdManager sharedManager] loadADWithPlacementID:self.placementID extra:@{kATAdLoadingExtraUserIDKey:userid,kATInterstitialExtraUsesRewardedVideo:@YES} delegate:self];

}
#pragma mark - loading delegate
-(void) didFinishLoadingADWithPlacementID:(NSString *)placementID {
    NSLog(@"【PluginToponInterstitial】RV Demo: didFinishLoadingADWithPlacementID %@",placementID);
    self.adInLoading--;
    self.adCacheCount++;
    
    // [self excute:@"play" andParams:@"1111" andCallback:0];
}

-(void) didFailToLoadADWithPlacementID:(NSString* )placementID error:(NSError *)error {
    NSLog(@"【PluginToponInterstitial】RV Demo: %@ failed to load:%@",placementID, error);
    self.adInLoading--;
}

#pragma mark - showing delegate
-(void) interstitialDidShowForPlacementID:(NSString *)placementID extra:(NSDictionary *)extra {
    NSLog(@"【PluginToponInterstitial】ATInterstitialViewController::interstitialDidShowForPlacementID:%@ extra:%@", placementID, extra);
    [self nativeEventHandler:@"open" andParams:[JSONUtil stringify:self.resultMsg]];
}

-(void) interstitialFailedToShowForPlacementID:(NSString*)placementID error:(NSError*)error extra:(NSDictionary *)extra {
    NSLog(@"【PluginToponInterstitial】ATInterstitialViewController::interstitialFailedToShowForPlacementID:%@ error:%@ extra:%@", placementID, error, extra);
    if (error != nil){
        [self.resultMsg setObject:@(error.code) forKey:@"code"];
        [self nativeEventHandler:@"error" andParams:[JSONUtil stringify:self.resultMsg]];
    }
}

-(void) interstitialDidFailToPlayVideoForPlacementID:(NSString*)placementID error:(NSError*)error extra:(NSDictionary*)extra {
    NSLog(@"【PluginToponInterstitial】ATInterstitialViewController::interstitialDidFailToPlayVideoForPlacementID:%@ error:%@ extra:%@", placementID, error, extra);
    if (error != nil && self.resultMsg){
        [self.resultMsg setObject:@(error.code) forKey:@"code"];
        [self nativeEventHandler:@"error" andParams:[JSONUtil stringify:self.resultMsg]];
    }
}

-(void) interstitialDidStartPlayingVideoForPlacementID:(NSString*)placementID extra:(NSDictionary *)extra {
    NSLog(@"【PluginToponInterstitial】ATInterstitialViewController::interstitialDidStartPlayingVideoForPlacementID:%@ extra:%@", placementID, extra);
}

-(void) interstitialDidEndPlayingVideoForPlacementID:(NSString*)placementID extra:(NSDictionary *)extra {
    NSLog(@"【PluginToponInterstitial】ATInterstitialViewController::interstitialDidEndPlayingVideoForPlacementID:%@ extra:%@", placementID, extra);
//    [self.resultMsg setObject:rewardType forKey:@"rewardType"];
//    [self.resultMsg setObject:rewardAmount forKey:@"rewardAmount"];
//    [self nativeEventHandler:@"earned" andParams:[JSONUtil stringify:self.resultMsg]];
}

-(void) interstitialDidCloseForPlacementID:(NSString*)placementID extra:(NSDictionary *)extra {
    NSLog(@"【PluginToponInterstitial】ATInterstitialViewController::interstitialDidCloseForPlacementID:%@ extra:%@", placementID, extra);
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

-(void) interstitialDidClickForPlacementID:(NSString*)placementID extra:(NSDictionary *)extra {
    NSLog(@"【PluginToponInterstitial】ATInterstitialViewController::interstitialDidClickForPlacementID:%@ extra:%@", placementID, extra);
    if (self.resultMsg){
        [self nativeEventHandler:@"click" andParams:[JSONUtil stringify:self.resultMsg]];
    }
}
@end
