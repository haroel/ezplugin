//
//  PluginSystemControll.h
//  lanwangame-mobile
//
//  Created by lanwan on 2020/10/28.
//

#import "PluginBase.h"

NS_ASSUME_NONNULL_BEGIN

@interface PluginSystemControll : PluginBase{
    
}
-(void)initPlugin:(NSDictionary*)params;
-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback;
@end

NS_ASSUME_NONNULL_END
