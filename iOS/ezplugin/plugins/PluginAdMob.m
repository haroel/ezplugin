//
//  PluginAdMob.m
//  epicslots-mobile
//
//  Created by howe on 2020/6/11.
//

#import "PluginAdMob.h"
#import "JSONUtil.h"

@interface RewardAdVO : NSObject
    @property(nonatomic,strong) GADRewardedAd *__nullable rewardedAd;
 // 状态0表示正在加载，1表示加载完成，2表示正在播放，3表示播放完成
    @property int state;
@end
@implementation RewardAdVO
@end

@implementation PluginAdMob

-(void)initPlugin:(NSDictionary*)params{
    if ( self.isInited){
        return;
    }
    // 初始化 GADMobileAds
    [[GADMobileAds sharedInstance] startWithCompletionHandler:nil];

    #if defined(COCOS2D_DEBUG) && (COCOS2D_DEBUG > 0)
        self.adUnitID = [params objectForKey:@"debugAdUnitID"];
        self.isDebug = YES;
    // <Google> To get test ads on this device, call:
        GADMobileAds.sharedInstance.requestConfiguration.testDeviceIdentifiers = @[kGADSimulatorID, @"07b42dd6fe7f845e51b07f57cc8e2556" ];
    #else
        self.adUnitID = [params objectForKey:@"adUnitID"];
        self.isDebug = NO;
    #endif
    
    self.adInLoading = 0;
    self.admobHashMap = [NSMutableDictionary dictionary];
    self.resultMsg = [NSMutableDictionary dictionary];
}

-(GADRewardedAd*)createAndLoadRewardedAd{
    NSString *unitID = _adUnitID;
    GADRewardedAd * rewardedAd = [[GADRewardedAd alloc] initWithAdUnitID:unitID];
    self.adInLoading = self.adInLoading + 1;
    NSLog(@"PluginAdMob load rewardedAd unitID=%@ adInLoading=%d",unitID,self.adInLoading);
    [rewardedAd loadRequest:[GADRequest request] completionHandler:^(GADRequestError * _Nullable error) {
        self.adInLoading = self.adInLoading - 1;
        if (error) {
            // Handle ad failed to load case.
            NSLog(@"%@", [NSString stringWithFormat:@"PluginAdMob->onRewardedAdFailedToLoad errorCode = %ld",(long)error.code]);
        } else {
            // Ad successfully loaded.
            NSLog(@"PluginAdMob->successfully loaded! onRewardedAdLoaded %@",rewardedAd.responseInfo.responseIdentifier);
            RewardAdVO *rewardAdVO = [[RewardAdVO alloc] init];
            rewardAdVO.rewardedAd = rewardedAd;
            rewardAdVO.state = 1;
            [self.admobHashMap setObject:rewardAdVO forKey:rewardedAd.responseInfo.responseIdentifier];
            [self nativeEventHandler:@"hasRewardAd" andParams:@"1"];
        }
    }];
    return rewardedAd;
}

-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback{
    if ( [type isEqualToString:@"hasReward"] ){
        [self nativeCallbackHandler:callback andParams:(self.admobHashMap.count>0?@"1":@"0" )];
    }else if ( [type isEqualToString:@"loadAd"] ){
        NSLog(@"PluginAdMob->excute loadAd ");
        if (self.adInLoading < 1){
            [self createAndLoadRewardedAd];
        }
        [self nativeCallbackHandler:callback andParams:(self.admobHashMap.count>0?@"1":@"0" )];
    }else if ( [type isEqualToString:@"play"] ){
        [ self.resultMsg removeAllObjects];
        [ self.resultMsg setObject:[NSNumber numberWithBool:self.isDebug] forKey:@"isDebug"];
        [ self.resultMsg setObject:_adUnitID forKey:@"adUnitID"];
        [ self.resultMsg setObject:params forKey:@"rewardId"];
        if (self.admobHashMap.count < 1){
            if (self.adInLoading < 1){
                [self createAndLoadRewardedAd];
            }
            [self.resultMsg setObject:@"NotLoadedYet" forKey:@"msg"];
            [self nativeEventHandler:@"error" andParams:[JSONUtil stringify:self.resultMsg] ];
            return;
        }
        // 从当前缓存的rewardedAd中取一个播放广告
        NSArray *adkeys = [self.admobHashMap allKeys];
        for (int i = 0; i < adkeys.count;i++){
            NSString * key = [adkeys objectAtIndex:i];
            RewardAdVO *rewardAdVO = [self.admobHashMap objectForKey:key];
            if (rewardAdVO.rewardedAd.isReady && rewardAdVO.state == 1){
                rewardAdVO.state = 2;
                [self.resultMsg setObject:key forKey:@"responseIdentifier"];
                UIWindow * window = [[UIApplication sharedApplication] keyWindow];
                [rewardAdVO.rewardedAd presentFromRootViewController:window.rootViewController delegate:self];
                [self createAndLoadRewardedAd];
                return;
            }
        }
        [self.admobHashMap removeAllObjects];
        [self createAndLoadRewardedAd];
        NSLog(@"The rewarded ad wasn't ready.");
        [self.resultMsg setObject:@"NotReadyYet" forKey:@"msg"];
        [self nativeEventHandler:@"error" andParams:[JSONUtil stringify:self.resultMsg] ];
    }
}

- (void)rewardedAd:(nonnull GADRewardedAd *)rewardedAd
 userDidEarnReward:(nonnull GADAdReward *)reward{
    NSLog(@"onUserEarnedReward type= %@ amount =%@",reward.type,reward.amount);
    if (self.resultMsg){
        [self.resultMsg setObject:reward.type forKey:@"rewardType"];
        [self.resultMsg setObject:reward.amount forKey:@"rewardAmount"];
        [self nativeEventHandler:@"earned" andParams:[JSONUtil stringify:self.resultMsg]];
    }
}

/// Tells the delegate that the rewarded ad failed to present.
- (void)rewardedAd:(nonnull GADRewardedAd *)rewardedAd didFailToPresentWithError:(nonnull NSError *)error{
    NSLog(@"onRewardedAdFailedToShow msg:%@  error %ld",error.localizedDescription, (long)error.code);
    NSString * key = rewardedAd.responseInfo.responseIdentifier;
    [self.admobHashMap removeObjectForKey:key];
    if (self.resultMsg){
        [self.resultMsg setObject:@(error.code) forKey:@"code"];
        [self nativeEventHandler:@"error" andParams:[JSONUtil stringify:self.resultMsg]];
    }
}

/// Tells the delegate that the rewarded ad was presented.
- (void)rewardedAdDidPresent:(nonnull GADRewardedAd *)rewardedAd{
    if (self.resultMsg){
        [self nativeEventHandler:@"open" andParams:[JSONUtil stringify:self.resultMsg]];
    }
}

/// Tells the delegate that the rewarded ad was dismissed.
- (void)rewardedAdDidDismiss:(nonnull GADRewardedAd *)rewardedAd{
    NSString * key = rewardedAd.responseInfo.responseIdentifier;
    [self.admobHashMap removeObjectForKey:key];
    if (self.resultMsg){
        [self nativeEventHandler:@"close" andParams:[JSONUtil stringify:self.resultMsg]];
    }
}
@end
