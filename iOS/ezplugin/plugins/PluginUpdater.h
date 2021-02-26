//
//  PluginUpdater.h
//  globalTycoon-mobile
//
//  Created by howe on 2020/10/27.
//

#import "PluginBase.h"

NS_ASSUME_NONNULL_BEGIN

@interface PluginUpdater : PluginBase

-(void)initPlugin:(NSDictionary*)params;

-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback;

@end

NS_ASSUME_NONNULL_END
