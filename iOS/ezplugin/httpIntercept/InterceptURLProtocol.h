//
//  InterceptURLProtocol.h
//  shawngames-mobile
//
//  Created by howe on 2019/8/29.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface InterceptURLProtocol : NSURLProtocol

+(void)initPaths:(NSString*)prefixUrl andCache:(NSString*)cachePath;

@end

NS_ASSUME_NONNULL_END
