//
//  HttpUtils.h
//  SlotsMarket
//
//  Created by howe on 2019/8/16.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface HttpUtils : NSObject

+(void)httpGet:(NSString*)url retry:(int)count andCallback:(void (^)(NSData * _Nullable data, NSURLResponse * _Nullable response, NSError * _Nullable error))completionHandler;

+(void)download:(NSString *)url savePath:(NSString *)savepath retry:(int)retryTimes andCallback:(void (^)(BOOL success,NSString * filePath))completeHandler;
@end

NS_ASSUME_NONNULL_END
