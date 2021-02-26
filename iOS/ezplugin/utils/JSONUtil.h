//
//  JSONUtil.h
//  SlotsMarket
//
//  Created by howe on 2019/8/16.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface JSONUtil : NSObject
/**** 定义工具方法  begin ****/

+(id)parse:(NSString *)jsonString;

/*
 - Top level object is an NSArray or NSDictionary
 - All objects are NSString, NSNumber, NSArray, NSDictionary, or NSNull
 - All dictionary keys are NSStrings
 - NSNumbers are not NaN or infinity
 */
+(NSString*)stringify:(id)arrayOrDict;
@end

NS_ASSUME_NONNULL_END
