//
//  PluginFirebase.h
//  epicslots-mobile
//
//  Created by howe on 2020/6/11.
//

#import "PluginBase.h"

NS_ASSUME_NONNULL_BEGIN

@interface PluginFirebase : PluginBase

@property (nonatomic,copy ) NSString * deepLinkURL;

-(void)initPlugin:(NSDictionary*)params;

-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback;
- (void)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions;
@end

NS_ASSUME_NONNULL_END
