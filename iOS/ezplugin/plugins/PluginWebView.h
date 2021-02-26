//
//  PluginInterceptURL.h
//  shawngames-mobile
//
//  Created by howe on 2019/8/29.
//

#import "PluginBase.h"

NS_ASSUME_NONNULL_BEGIN

@interface PluginWebView : PluginBase{
    
    NSString *filterPrefixUrl;
    NSString *hallgamesCachePath;
    NSMutableDictionary * downloadHashMap;
}
-(void)initPlugin:(NSDictionary*)params;

-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback;
@end

NS_ASSUME_NONNULL_END
