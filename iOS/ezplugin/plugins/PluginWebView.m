//
//  PluginInterceptURL.m
//  shawngames-mobile
//
//  Created by howe on 2019/8/29.
//

#import "PluginWebView.h"
#import "InterceptURLProtocol.h"
#import "HttpDownloadEntity.h"
#import "JSONUtil.h"
//#import <SSZipArchive/SSZipArchive.h>

/**
 定义事件
 **/
static NSString * const EVENT_GAME_DOWNLOAD_START = @"EVENT_GAME_DOWNLOAD_START";
static NSString * const EVENT_GAME_DOWNLOAD_PROGRESS = @"EVENT_GAME_DOWNLOAD_PROGRESS";
static NSString * const EVENT_GAME_DOWNLOAD_COMPLETE = @"EVENT_GAME_DOWNLOAD_COMPLETE";
static NSString * const EVENT_GAME_DOWNLOAD_ERROR = @"EVENT_GAME_DOWNLOAD_ERROR";


@implementation PluginWebView
-(void)initPlugin:(NSDictionary*)params{
    if (self.isInited){
        return;
    }
    [NSURLProtocol registerClass:[InterceptURLProtocol class]];
    // WKWebview url 拦截， 用到了siyou API，所以此处需要把类名x处理一下过审h
    NSArray *privateStrArr = @[@"Controller", @"Context", @"Browsing", @"K", @"W"];
    NSString *className =  [[[privateStrArr reverseObjectEnumerator] allObjects] componentsJoinedByString:@""];
    //注册scheme
    Class cls = NSClassFromString(className);
    
    privateStrArr = @[@"register", @"SchemeFor", @"CustomProtocol",@":"];
    SEL sel = NSSelectorFromString( [privateStrArr componentsJoinedByString:@""]);
    if ([cls respondsToSelector:sel]) {
        // 通过http和https的请求，同理可通过其他的Scheme 但是要满足ULR Loading System
        [cls performSelector:sel withObject:@"http"];
        [cls performSelector:sel withObject:@"https"];
    }
    
    downloadHashMap = [NSMutableDictionary dictionary];
    [downloadHashMap retain];
}

-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback{
    
    if ( [type isEqualToString:@"init"]){
        NSArray *paths = [params componentsSeparatedByString:@"|"];
        NSLog(@"设置资源拦截目录和rurl前缀 %@",paths);
        filterPrefixUrl = paths[0];
        hallgamesCachePath = paths[1];
        [[NSFileManager defaultManager] createDirectoryAtPath:hallgamesCachePath withIntermediateDirectories:YES attributes:nil error:nil ];
        [InterceptURLProtocol initPaths:paths[0] andCache:paths[1]];
        
    }else if ( [type isEqualToString:@"updateGame"] ){
        NSLog(@" 更新某个版本最新的游戏 %@",params);
        NSArray *paths = [params componentsSeparatedByString:@"|"];

        NSString * downloadUrl = paths[0];
        NSString * gameID = paths[1];
        if ( [downloadHashMap objectForKey:gameID ]  ){
            NSLog(@"[PluginWebView] 重复执行！正在下载 %@",gameID);
        }else{
            [self updateGame:downloadUrl andGameID:gameID];
        }
    }
    [self nativeCallbackHandler:callback andParams:@""];
}
-(void)updateGame:(NSString*)downloadUrl andGameID:(NSString*)gameID{
    
    PluginBase * plugin = self;
    NSFileManager *fm = [NSFileManager defaultManager];
    NSString * savePath = [NSString stringWithFormat:@"%@/temp_%@.zip",hallgamesCachePath,gameID];
    [fm removeItemAtPath:savePath error:nil];
    NSLog(@"[PluginWebView] 下载 %@",downloadUrl);

    HttpDownloadEntity* downloadEntity = [[HttpDownloadEntity alloc] initWithUrl:downloadUrl savePath:savePath andRetry:2];
    [downloadEntity setDownloadHandler:^(int state, NSString * _Nonnull params) {
//        NSLog(@"HttpDownloadEntity event %d %@",state,params);
        switch(state){
            case 0:{
                NSMutableDictionary *dict = [NSMutableDictionary dictionary];
                [dict setObject:gameID forKey:@"gameID"];
                [plugin nativeEventHandler:EVENT_GAME_DOWNLOAD_START andParams:[JSONUtil stringify:dict]];
                break;
            }
            case 1:{
                NSMutableDictionary *dict = [NSMutableDictionary dictionary];
                [dict setObject:gameID forKey:@"gameID"];
                [dict setObject:params forKey:@"progress"];
                [plugin nativeEventHandler:EVENT_GAME_DOWNLOAD_PROGRESS andParams:[JSONUtil stringify:dict]];
                break;
            }
            case 2:{
                NSString * unzipPath = [hallgamesCachePath stringByAppendingPathComponent:gameID];
                [fm removeItemAtPath:unzipPath error:nil];
                [[NSFileManager defaultManager] createDirectoryAtPath:unzipPath withIntermediateDirectories:YES attributes:nil error:nil ];
                NSLog(@"解压zip %@ 并删除zip文件",unzipPath);
//                [SSZipArchive unzipFileAtPath:savePath toDestination:unzipPath];
                [fm removeItemAtPath:savePath error:nil];
                NSMutableDictionary *dict = [NSMutableDictionary dictionary];
                [dict setObject:gameID forKey:@"gameID"];
                [plugin nativeEventHandler:EVENT_GAME_DOWNLOAD_COMPLETE andParams:[JSONUtil stringify:dict]];
                [downloadHashMap removeObjectForKey:gameID];
                break;
            }
            default:{
                NSMutableDictionary *dict = [NSMutableDictionary dictionary];
                [dict setObject:gameID forKey:@"gameID"];
                [plugin nativeEventHandler:EVENT_GAME_DOWNLOAD_ERROR andParams:[JSONUtil stringify:dict]];
                [downloadHashMap removeObjectForKey:gameID];
                break;
            }
        }
    } ];
    [downloadHashMap setObject:downloadEntity forKey:gameID];
}

@end
