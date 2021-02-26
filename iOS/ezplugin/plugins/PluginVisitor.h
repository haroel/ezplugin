//
//  PluginVIsitor.h
//  shawngames-mobile
//
//  Created by howe on 2019/10/15.
//

#import "PluginBase.h"

NS_ASSUME_NONNULL_BEGIN

@interface PluginVIsitor : PluginBase

-(void)initPlugin:(NSDictionary*)params;

-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback;


@end

NS_ASSUME_NONNULL_END
