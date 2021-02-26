//
//  PluginToponInterstitial.h
//  jigsaw-mobile
//
//  Created by howe on 2020/8/20.
// 插屏广告
//

#import "PluginBase.h"
#import <AnyThinkInterstitial/AnyThinkInterstitial.h>

NS_ASSUME_NONNULL_BEGIN

@interface PluginToponInterstitial : PluginBase<ATInterstitialDelegate>

@property(nonatomic,copy ) NSString * placementID;
@property(nonatomic,strong) NSMutableDictionary *__nullable resultMsg;

@property BOOL isDebug;
// 正在加载中的Ad广告
@property int adInLoading;

@property int adCacheCount;

@end


NS_ASSUME_NONNULL_END
