//
//  PluginOS.h
//  ezgame_qp
//
//  Created by howe on 2017/10/16.
//

#import <Foundation/Foundation.h>
#import "PluginBase.h"

@interface PluginOS : PluginBase{
}

-(void)initPlugin:(NSDictionary*)params;

-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback;


@end
