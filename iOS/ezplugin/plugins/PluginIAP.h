//
//  PluginIAP.h
//  Game
//
//  Created by howe on 2017/9/13.
//
//

#import "PluginBase.h"
#import <ez_iap/IAPApi.h>

@interface PluginIAP : PluginBase<EZIAPDelegate>

-(void)initPlugin:(NSDictionary*)params;

-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback;


@end
