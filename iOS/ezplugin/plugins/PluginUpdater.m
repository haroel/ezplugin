//
//  PluginUpdater.m
//  globalTycoon-mobile
//
//  Created by howe on 2020/10/27.
//

#import "PluginUpdater.h"
#import <ez_updater/EZUpdater.h>
@implementation PluginUpdater

-(void)initPlugin:(NSDictionary*)params{
    
}

-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback{
    if ([type isEqual:@"update"]){
        [[EZUpdater Instance] checkAppStoreVersion];
    }
}

@end
