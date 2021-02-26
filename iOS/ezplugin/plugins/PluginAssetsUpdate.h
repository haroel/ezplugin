//
//  PluginAssetsUpdate.h
//  SlotsMarket
//
//  Created by howe on 2019/8/16.
//

#import <Foundation/Foundation.h>
#import "PluginBase.h"

@interface PluginAssetsUpdate : PluginBase
{
    NSMutableArray *needDownloadResHashMap;
    NSMutableArray *needDeleteResHashMap;
    int downloadIndex;
}

@property (nonatomic,copy ) NSString * res_patch_url;
@property (nonatomic,copy ) NSString * tempCacheDir;
@property (nonatomic,copy ) NSString * assetsPatchDir;

-(void)initPlugin:(NSDictionary*)params;

-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback;


@end
