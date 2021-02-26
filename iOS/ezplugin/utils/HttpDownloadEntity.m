//
//  HttpDownloadEntity.m
//  shawngames-mobile
//
//  Created by howe on 2019/8/29.
//

#import "HttpDownloadEntity.h"


@interface HttpDownloadEntity()<NSURLSessionDownloadDelegate>

@end

@implementation HttpDownloadEntity

-(HttpDownloadEntity*) initWithUrl:(NSString*)remoteurl savePath:(NSString*)localSavepath andRetry:(int)times{
    self = [super init];
    downloadUrl = remoteurl;
    savePath = localSavepath;
    retryTime = times;
    progress = 0;
    [self startDownload];
    return self;
}

-(void)startDownload{
    // 1.创建NSURLSession
    /*
     第一个参数: 全局的配置
     第二个参数: 让谁成为session的代理
     第三个参数: 告诉系统代理方法在哪个线程中执行
     */
    //1.1创建URL
    NSURL *url = [NSURL URLWithString:downloadUrl];
    NSURLSession *session = [NSURLSession sessionWithConfiguration:[NSURLSessionConfiguration defaultSessionConfiguration] delegate:self delegateQueue:[NSOperationQueue mainQueue]];
    // 2、利用NSURLSessionDownloadTask创建任务(task)
    NSURLSessionDownloadTask *task = [session downloadTaskWithURL:url];
    // 3、执行任务
    [task resume];
}

// 
-(void)setDownloadHandler:(HttpDownloadEntityCallback)callback{
    self.messageHandler = callback;
    if ( self.messageHandler ){
        self.messageHandler(0,@"");
    }
}
/*
 1.接收到服务器返回的数据
 bytesWritten: 当前这一次写入的数据大小
 totalBytesWritten: 已经写入到本地文件的总大小
 totalBytesExpectedToWrite : 被下载文件的总大小
 */
- (void)URLSession:(NSURLSession *)session downloadTask:(NSURLSessionDownloadTask *)downloadTask didWriteData:(int64_t)bytesWritten totalBytesWritten:(int64_t)totalBytesWritten totalBytesExpectedToWrite:(int64_t)totalBytesExpectedToWrite
{
    progress = 1.0 * totalBytesWritten / totalBytesExpectedToWrite;
    if (self.messageHandler){
        self.messageHandler(1,[NSString stringWithFormat:@"%f",progress]);
    }
}

/*
 2.下载完成
 downloadTask:里面包含请求信息，以及响应信息
 location：下载后自动帮我保存的地址
 */
- (void)URLSession:(NSURLSession *)session downloadTask:(NSURLSessionDownloadTask *)downloadTask didFinishDownloadingToURL:(NSURL *)location
{
    //location为下载好的文件路径
    //NSLog(@"didFinishDownloadingToURL, %@", location);
    
    NSString *saveDir = [savePath stringByDeletingLastPathComponent];
    [[NSFileManager defaultManager] createDirectoryAtPath:saveDir withIntermediateDirectories:YES attributes:nil error:nil ];
    [[NSFileManager defaultManager] moveItemAtURL:location toURL:[NSURL fileURLWithPath:savePath] error:nil];
    if (self.messageHandler){
        self.messageHandler(2, savePath );
    }
}

/*
 3.请求完毕
 如果有错误, 那么error有值
 */
- (void)URLSession:(NSURLSession *)session task:(NSURLSessionTask *)task didCompleteWithError:(NSError *)error
{
    if (!error) {
        NSLog(@"请求成功");
    }else{
        NSLog(@"请求失败 %@",error);
        if (retryTime > 0){
            retryTime--;
            [self startDownload];
        }else if (self.messageHandler != nil){
            self.messageHandler(-1, downloadUrl );
        }
    }
}
@end
