//
//  PluginTopon.h
//  globalTycoon-mobile
//
//  Created by howe on 2020/7/10.
//

#import "PluginBase.h"
#import <AnyThinkRewardedVideo/ATRewardedVideoDelegate.h>

NS_ASSUME_NONNULL_BEGIN

@interface PluginTopon : PluginBase<ATRewardedVideoDelegate>
@property(nonatomic,copy ) NSString * placementID;
@property(nonatomic,strong) NSMutableDictionary *__nullable resultMsg;

@property BOOL isDebug;
// 正在加载中的Ad广告
@property int adInLoading;

@property int adCacheCount;

@end

NS_ASSUME_NONNULL_END
