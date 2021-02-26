//
//  PluginFacebook.h
//  shawngames-mobile
//
//  Created by howe on 2019/8/26.
//

#import "PluginBase.h"
#import <FBSDKShareKit/FBSDKShareKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface PluginFacebook : PluginBase<FBSDKSharingDelegate>
-(void)initPlugin:(NSDictionary*)params;
    
-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback;

@end

NS_ASSUME_NONNULL_END
