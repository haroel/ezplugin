//
//  HttpUtils.m
//  SlotsMarket
//
//  Created by howe on 2019/8/16.
//

#import "HttpUtils.h"

@implementation HttpUtils

+(void)httpGet:(NSString *)url retry:(int)retryTimes andCallback:(void (^)(NSData * _Nullable, NSURLResponse * _Nullable, NSError * _Nullable))completionHandler{
    
    NSURL *nsurl = [NSURL URLWithString:url ];
    // 通过URL初始化task,在block内部可以直接对返回的数据进行处理
    NSURLSessionTask *task = [ [NSURLSession sharedSession] dataTaskWithURL:nsurl
                                                          completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
                                                              if (error && retryTimes > 0){
                                                                  NSLog(@"%@ -> %@", url, error);
                                                                  [HttpUtils httpGet:url retry:(retryTimes-1) andCallback:completionHandler];
                                                                  return;
                                                              }
                                                              dispatch_async(dispatch_get_main_queue(), ^{
                                                                  completionHandler(data,response,error);
                                                              });
                                                            }];
    [task resume];
}

+(void)download:(NSString *)url savePath:(NSString *)savepath retry:(int)retryTimes andCallback:(void (^)(BOOL success,NSString * filePath))completeHandler{
    
    NSURLSession *session = [NSURLSession sharedSession];
    NSURL *nsurl = [NSURL URLWithString:url] ;
    NSURLSessionDownloadTask *task = [session downloadTaskWithURL:nsurl completionHandler:^(NSURL *location, NSURLResponse *response, NSError *error) {
        if ( error!=nil && retryTimes > 0){
            NSLog(@"%@ -> %@", url, error);
            [HttpUtils download:url savePath:savepath retry:(retryTimes-1) andCallback:completeHandler];
            return;
        }

        if ( error == nil){
            // location是沙盒中tmp文件夹下的一个临时url,文件下载后会存到这个位置,由于tmp中的文件随时可能被删除,所以我们需要自己需要把下载的文件挪到需要的地方
            // 剪切文件
            NSString *saveDir = [savepath stringByDeletingLastPathComponent];
            [[NSFileManager defaultManager] createDirectoryAtPath:saveDir withIntermediateDirectories:YES attributes:nil error:nil ];
            [[NSFileManager defaultManager] moveItemAtURL:location toURL:[NSURL fileURLWithPath:savepath] error:nil];
            // 把结果同步到主线程
            dispatch_async(dispatch_get_main_queue(), ^{
                completeHandler(YES,savepath);
            });
        }else{
            // 把结果同步到主线程
            dispatch_async(dispatch_get_main_queue(), ^{
                completeHandler(NO,savepath);
            });
        }
    }];
    // 启动任务
    [task resume];
}

@end
