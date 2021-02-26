//
//  PluginAssetsUpdate.m
//  SlotsMarket
//
//  Created by howe on 2019/8/16.
//

#import "PluginAssetsUpdate.h"

#import "../core/PluginCore.h"
#import "../utils/JSONUtil.h"
#import "../utils/HttpUtils.h"

#include "cocos2d.h"

 static NSString * const MD5FILE_NAME = @"md5fileList.txt";

 static NSString * const EVENT_PATCH_START = @"EVENT_PATCH_START";
 static NSString * const EVENT_PATCH_PROGRESS = @"EVENT_PATCH_PROGRESS";
 static NSString * const EVENT_PATCH_FAILED = @"EVENT_PATCH_FAILED";
 static NSString * const EVENT_PATCH_FINISHED = @"EVENT_PATCH_FINISHED";
 static NSString * const EVENT_PATCH_NONEED = @"EVENT_PATCH_NONEED";

@implementation PluginAssetsUpdate

-(void)initPlugin:(NSDictionary*)params{
    if ( self.isInited){
        return;
    }
    NSURLSessionConfiguration *sessionConfig = [NSURLSessionConfiguration defaultSessionConfiguration];
    sessionConfig.timeoutIntervalForRequest = 30.0;
    sessionConfig.timeoutIntervalForResource = 60.0;
    
    self.tempCacheDir = [NSTemporaryDirectory() stringByAppendingPathComponent:@"assetsUpdateTemp"];
    
    NSFileManager *fileManager = [NSFileManager defaultManager];
    BOOL isDir = YES;
    BOOL existed = [fileManager fileExistsAtPath:self.tempCacheDir isDirectory:&isDir];
    if (existed){
        NSLog(@"【PluginAssetsUpdate】删除temp缓存目录 %@",self.tempCacheDir);
        [fileManager removeItemAtPath:self.tempCacheDir error:nil];
    }
    [fileManager createDirectoryAtPath:self.tempCacheDir withIntermediateDirectories:YES attributes:nil error:nil];
    
    std::string _gameWritePath(cocos2d::FileUtils::getInstance()->getWritablePath());

    NSString * gameWritePath = @(_gameWritePath.c_str());
    
    self.assetsPatchDir = [gameWritePath stringByAppendingPathComponent:@"assetspatch"];

    NSDictionary *infoDict= [[NSBundle mainBundle] infoDictionary];
    NSString *appVersion = [infoDict objectForKey:@"CFBundleShortVersionString"];
    NSString *appBuild = [infoDict objectForKey:@"CFBundleVersion"];
    
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSString *lastVersison = [defaults stringForKey:@"lastversion"];
    NSString *lastBuild = [defaults stringForKey:@"lastbuild"];

    existed = [fileManager fileExistsAtPath:self.assetsPatchDir isDirectory:&isDir];

    BOOL isVersionChange = lastVersison == nil || ![appVersion isEqualToString:lastVersison];
    BOOL isBuildChange = lastBuild == nil || ![appBuild isEqualToString:lastBuild];
    if ( isVersionChange || isBuildChange ){
        [defaults setObject:appVersion forKey:@"lastversion"];
        [defaults setObject:appBuild forKey:@"lastbuild"];
        if (existed){
            [fileManager removeItemAtPath:self.assetsPatchDir error:nil];
            NSLog(@"【PluginAssetsUpdate】删除 %@",self.assetsPatchDir);
        }
    }
    if (!existed){
        [fileManager createDirectoryAtPath:self.assetsPatchDir withIntermediateDirectories:YES attributes:nil error:nil];
    }
}

-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback{
    if ( [type isEqualToString:@"init"] ){
        id dict = [JSONUtil parse:params];
        if (dict != nil){
            self.res_patch_url = [dict objectForKey:@"res_patch_url"];
            [self nativeCallbackHandler:callback andParams:@"1"];
        }else{
            [self nativeCallbackErrorHandler:callback andParams:@"0"];
        }
    }else if ( [type isEqualToString:@"assetsPatch"] ){
        [self downloadMd5file:callback];
        [self nativeCallbackHandler:callback andParams:@"1"];
    }else{
        [self nativeCallbackHandler:callback andParams:@"0"];
    }
}

-(void)downloadMd5file:(int)callback{
    
    NSFileManager *fileManager = [NSFileManager defaultManager];
    
    NSString * localMd5FileListContent = nil;
    NSString * localMd5FileListPath = [self.assetsPatchDir stringByAppendingPathComponent:MD5FILE_NAME];
    BOOL existed = [fileManager fileExistsAtPath:localMd5FileListPath];
    if (existed){
        localMd5FileListContent = [NSString stringWithContentsOfFile:localMd5FileListPath encoding:NSUTF8StringEncoding error:nil];
    }else{
        localMd5FileListPath = [[[NSBundle mainBundle] bundlePath] stringByAppendingPathComponent:MD5FILE_NAME];
        localMd5FileListContent = [NSString stringWithContentsOfFile:localMd5FileListPath encoding:NSUTF8StringEncoding error:nil];
    }
    NSDate* date = [NSDate dateWithTimeIntervalSinceNow:0];//获取当前时间0秒后的时间
    NSString * remoteMd5FileListUrl = [NSString stringWithFormat:@"%@/%@?t=%f",self.res_patch_url,MD5FILE_NAME,[date timeIntervalSince1970]];
    NSLog(@"【PluginAssetsUpdate】请求远程服务器的md5文件列表配置，remoteMd5FileListUrl=%@",remoteMd5FileListUrl);
    [HttpUtils httpGet:remoteMd5FileListUrl retry:2 andCallback:^(NSData *data, NSURLResponse *response, NSError *error) {
        if (error){
            NSLog(@"%@",error);
            [self nativeEventHandler:EVENT_PATCH_FAILED andParams:[error localizedFailureReason]];
            return;
        }
        NSString * remoteMd5FileListContent = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
        if ([remoteMd5FileListContent isEqualToString:localMd5FileListContent]){
            [self nativeEventHandler:EVENT_PATCH_NONEED andParams:@"0"];
            return;
        }
        [self compareLocalAndRemote:localMd5FileListContent andRemoteMd5FileListContent:remoteMd5FileListContent];
        [self nativeEventHandler:EVENT_PATCH_START andParams: [NSString stringWithFormat:@"%lu",(unsigned long)needDownloadResHashMap.count]];
        NSLog(@"【PluginAssetsUpdate】需要下载的文件 %@",needDownloadResHashMap);
        downloadIndex = 0;
        [self downloadFile];
    } ];
}

-(NSString*)checkSafeString:(NSString*)str{
    NSString * headerData = [str stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
    [headerData stringByReplacingOccurrencesOfString:@"\r" withString:@""];
    return [headerData stringByReplacingOccurrencesOfString:@"\n" withString:@""];
}
-(void)compareLocalAndRemote:(NSString*)localMd5FileListContent andRemoteMd5FileListContent:(NSString*)remoteMd5FileListContent{
    NSMutableDictionary *localMd5HashMap = [[NSMutableDictionary alloc] init];
    for (NSString *line in [localMd5FileListContent componentsSeparatedByString:@"\n"] ){
        NSArray *datas = [line componentsSeparatedByString:@"|"];
        if (datas.count < 2){
            continue;
        }
        NSString * fileName = [self checkSafeString:datas[1]];
        NSString * fileUUID = [self checkSafeString:datas[0]];
        if ( [ fileName isEqual:@"" ] || [ fileUUID isEqual:@"" ] ){
            continue;
        }
        [localMd5HashMap setObject:fileName forKey:fileUUID];
    }
    
    NSMutableDictionary *remoteMd5HashMap = [[NSMutableDictionary alloc] init];
    for (NSString *line in [remoteMd5FileListContent componentsSeparatedByString:@"\n"] ){
        NSArray *datas = [line componentsSeparatedByString:@"|"];
        if (datas.count < 2){
            continue;
        }
        NSString * fileName = [self checkSafeString:datas[1]];
        NSString * fileUUID = [self checkSafeString:datas[0]];
        if ( [ fileName isEqual:@"" ] || [ fileUUID isEqual:@"" ] ){
            continue;
        }
        [remoteMd5HashMap setObject:fileName forKey:fileUUID];
    }
    needDeleteResHashMap = [[NSMutableArray alloc] init];
    needDownloadResHashMap = [[NSMutableArray alloc] init];
    cocos2d::FileUtils *fu =cocos2d::FileUtils::getInstance();
    for (NSString* remoteMd5Key in remoteMd5HashMap) {
        if ([localMd5HashMap objectForKey:remoteMd5Key ] == nil){
            [needDownloadResHashMap addObject:[remoteMd5HashMap objectForKey:remoteMd5Key]];
        }else{
            NSString * localFileName = [localMd5HashMap objectForKey:remoteMd5Key ];
            if (!fu->isFileExist(localFileName.UTF8String)){
                NSLog(@"【PluginAssetsUpdate】本地不存在，需要下载 %@",localFileName);
                [needDownloadResHashMap addObject:[remoteMd5HashMap objectForKey:remoteMd5Key]];
            }
        }
    }
    
    for (NSString* localMd5Key in localMd5HashMap) {
        if ([remoteMd5HashMap objectForKey:localMd5Key ] == nil){
            [needDeleteResHashMap addObject:[localMd5HashMap objectForKey:localMd5Key]];
        }
    }
    
}
-(void)downloadFile{
    if (needDownloadResHashMap.count == 0){
        [self nativeEventHandler:EVENT_PATCH_NONEED andParams:@"0"];
        return;
    }
    [self nativeEventHandler:EVENT_PATCH_PROGRESS andParams:[NSString stringWithFormat:@"%d|%lu",downloadIndex,(unsigned long)needDownloadResHashMap.count]];

    NSString *downloadFilePath = nil;
    if (downloadIndex >= needDownloadResHashMap.count){
        [self exchangeFiles];
        [self nativeEventHandler:EVENT_PATCH_FINISHED andParams:[NSString stringWithFormat:@"%lu",(unsigned long)needDownloadResHashMap.count]];
        return;
    }else{
        downloadFilePath = [needDownloadResHashMap objectAtIndex:downloadIndex];
    }

    NSString * downloadUrl = [NSString stringWithFormat:@"%@/%@",self.res_patch_url,downloadFilePath];
    NSString * savePath = [NSString stringWithFormat:@"%@/%@",self.tempCacheDir,downloadFilePath];
    int retry = 2;
    if ([downloadFilePath containsString:@".js"] || [downloadFilePath containsString:@".jsc"] ){
        retry = 5;
    }
    NSLog(@"【PluginAssetsUpdate】downloadUrl=%@",downloadUrl);
    [HttpUtils download:downloadUrl savePath:savePath retry:retry andCallback:^(BOOL success,NSString *localSavePath) {
        if (success){
            NSLog(@"PluginAssetsUpdate 下载成功 %@ %@",downloadUrl,localSavePath);
        }else{
            NSLog(@"PluginAssetsUpdate 下载失败 %@ %@",downloadUrl,localSavePath);
        }
        downloadIndex++;
        [self downloadFile];
    }];
    
}
-(void)exchangeFiles{
    NSFileManager *fileManager = [NSFileManager defaultManager];

    for (NSString* deleteFilePath in needDeleteResHashMap) {
        NSString * localFille = [NSString stringWithFormat:@"%@/%@",self.assetsPatchDir,deleteFilePath];
        [fileManager removeItemAtPath:localFille error:nil];
    }
    NSLog(@"PluginAssetsUpdate 把缓存temp下的文件转移到cache目录下 ");
    for (NSString * needFile in needDownloadResHashMap) {
        NSString * tempFile = [NSString stringWithFormat:@"%@/%@",self.tempCacheDir,needFile];
        if ([fileManager fileExistsAtPath:tempFile]){
            NSString * destFilePath = [NSString stringWithFormat:@"%@/%@",self.assetsPatchDir,needFile];
            NSError * error = nil;
            NSString *saveDir = [destFilePath stringByDeletingLastPathComponent];
            [[NSFileManager defaultManager] createDirectoryAtPath:saveDir withIntermediateDirectories:YES attributes:nil error:&error ];
            if (error!=nil){
                NSLog(@"exchangeFiles createDirectoryAtPath %@",error);
            }
            if (![fileManager moveItemAtPath:tempFile toPath:destFilePath error:&error]){
                NSLog(@"PluginAssetsUpdate 转移失败 %@",error);
            }
        }else{
            NSString * md5CacheFile = [NSString stringWithFormat:@"%@/%@",self.assetsPatchDir,MD5FILE_NAME];
            [fileManager removeItemAtPath:md5CacheFile error:nil];
        }
    }
}
@end
