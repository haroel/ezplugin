//
//  Sample.h
//  lanwangame-mobile
//
//  Created by lanwan on 2020/10/28.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface Sample : NSObject

@property int adInLoading;


+(void) count;

+(void) count:(NSString*)param andLevel:(int)lvel;

-(void) length;

@end

@interface Sample2 : NSObject

@property int adInLoading;


+(void) count;

+(void) count:(NSString*)param andLevel:(int)lvel;

-(void) length;

@end

NS_ASSUME_NONNULL_END
