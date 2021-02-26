//
//  HttpDownloadEntity.h
//  shawngames-mobile
//
//  Created by howe on 2019/8/29.
//

#import <Foundation/Foundation.h>


NS_ASSUME_NONNULL_BEGIN
/**
 * 0 表示开始下载
 * 1 表示正在下载，返回下载进度
 * 2 表示下载完成
 * -1 表示下载失败
 **/
typedef void(^HttpDownloadEntityCallback)(int state,NSString *params);

@interface HttpDownloadEntity : NSObject{
    @public
    NSString * downloadUrl;
    @public
    NSString * savePath;
    @public
    int retryTime;
    
    float progress;
}
@property(copy, nonatomic) HttpDownloadEntityCallback messageHandler;

/**
 * state
 * 0 表示开始下载
 * 1 表示正在下载，返回下载进度
 * 2 表示下载完成
 * -1 表示下载失败
 **/
-(void)setDownloadHandler:(HttpDownloadEntityCallback)callback;


-(HttpDownloadEntity*) initWithUrl:(NSString*)url savePath:(NSString*)localSavepath andRetry:(int)times;

@end

NS_ASSUME_NONNULL_END
