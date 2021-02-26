//
//  PluginAdMob.h
//  epicslots-mobile
//
//  Created by howe on 2020/6/11.
//

#import "PluginBase.h"
#import <GoogleMobileAds/GoogleMobileAds.h>

NS_ASSUME_NONNULL_BEGIN

@interface PluginAdMob : PluginBase<GADRewardedAdDelegate>

//@property(nonatomic,strong) GADRewardedAd *__nullable rewardedAd;
@property(nonatomic,strong) NSMutableDictionary *__nullable resultMsg;

@property(nonatomic,strong) NSMutableDictionary *__nullable admobHashMap;

@property(nonatomic,copy ) NSString * adUnitID;
//@property int faileLoadedCount;
//@property BOOL hasRewardAd;
@property BOOL isDebug;
// 正在加载中的Ad广告
@property int adInLoading;


@end

NS_ASSUME_NONNULL_END
