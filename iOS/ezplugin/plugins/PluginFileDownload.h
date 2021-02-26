//
//  PluginFileDownload.h
//  epicslots-mobile
//
//  Created by howe on 2020/1/17.
//

#import "PluginBase.h"

NS_ASSUME_NONNULL_BEGIN

@interface PluginFileDownload : PluginBase

-(void)initPlugin:(NSDictionary*)params;

-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback;

@end

NS_ASSUME_NONNULL_END
