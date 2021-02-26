//
//  PluginFileDownload.m
//  epicslots-mobile
//
//  Created by howe on 2020/1/17.
//

#import "PluginFileDownload.h"

#import "../core/PluginCore.h"
#import "../utils/JSONUtil.h"
#import "../utils/HttpUtils.h"

@implementation PluginFileDownload
-(void)initPlugin:(NSDictionary*)params{
    if ( self.isInited){
        return;
    }
}

-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback{
    if ( [type isEqualToString:@"download"] ){
        id dict = [JSONUtil parse:params];
        if (dict != nil){
            NSString * downloadUrl =[dict objectForKey:@"requestURL"];
            NSString * storagePath =[dict objectForKey:@"storagePath"];
            [self updateGame:downloadUrl andStoragePath:storagePath andCallback:(int)callback];
        }else{
            [self nativeCallbackErrorHandler:callback andParams:@"0"];
        }
    }
}

-(void)updateGame:(NSString*)downloadUrl andStoragePath:(NSString*)storagePath andCallback:(int)callback{
    NSFileManager * manager = [NSFileManager defaultManager];
    if ( [manager fileExistsAtPath:storagePath] ){
        NSDictionary *dict = [manager attributesOfItemAtPath:storagePath error:nil];
        if ([dict fileSize]>1){
            [self nativeCallbackHandler:callback andParams:storagePath];
            return;
        }
    }
    [manager removeItemAtPath:storagePath error:nil];
    [HttpUtils download:downloadUrl savePath:storagePath retry:0 andCallback:^(BOOL success,NSString *localSavePath) {
        if (success){
            NSLog(@"PluginFileDownload 下载成功 %@ %@",downloadUrl,storagePath);
            [self nativeCallbackHandler:callback andParams:storagePath];
        }else{
            NSLog(@"PluginFileDownload 下载失败 %@ %@",downloadUrl,localSavePath);
            [self nativeCallbackErrorHandler:callback andParams:storagePath];
        }
    }];
}

-(NSInteger)getSizeOfFilePath:(NSString *)filePath{
    /** 定义记录大小 */
    NSInteger totalSize = 0;
    /** 创建一个文件管理对象 */
    NSFileManager * manager = [NSFileManager defaultManager];
    /**获取文件下的所有路径包括子路径 */
    NSArray * subPaths = [manager subpathsAtPath:filePath];
    /** 遍历获取文件名称 */
    for (NSString * fileName in subPaths) {
        /** 拼接获取完整路径 */
        NSString * subPath = [filePath stringByAppendingPathComponent:fileName];
        /** 判断是否是隐藏文件 */
        if ([fileName hasPrefix:@".DS"]) {
            continue;
        }
        /** 判断是否是文件夹 */
        BOOL isDirectory;
        [manager fileExistsAtPath:subPath isDirectory:&isDirectory];
        if (isDirectory) {
            continue;
        }
        /** 获取文件属性 */
        NSDictionary *dict = [manager attributesOfItemAtPath:subPath error:nil];
        /** 累加 */
        totalSize += [dict fileSize];
        
    }
    /** 返回 */
    return totalSize;
}
@end
