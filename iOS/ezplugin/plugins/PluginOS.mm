//
//  PluginOS.m
//  ezgame_qp
//
//  Created by howe on 2017/10/16.
//

#import "PluginOS.h"
#import "PluginCore.h"
#import <AudioToolbox/AudioToolbox.h>
#import "../utils/JSONUtil.h"
#import <RealReachability/RealReachability.h>
#import <Foundation/Foundation.h>
#include "cocos2d.h"
#import "../utils/NSData+Base64.h"
#import <ASCScreenBrightnessDetector/ASCScreenBrightnessDetector.h>

#import <StoreKit/StoreKit.h>

@implementation PluginOS

-(void)initPlugin:(NSDictionary*)params{
    if (self.isInited){
        return;
    }
    // 防止息屏
    [[UIApplication sharedApplication] setIdleTimerDisabled: YES];
    // 设置NSDocumentDirectory 去除iCloud备份
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentsDirectory = [paths objectAtIndex:0];
    NSLog(@"NSDocumentDirectory %@",documentsDirectory);
    
    NSURL* URL= [NSURL fileURLWithPath: documentsDirectory];
    NSError *error = nil;
    BOOL success = [URL setResourceValue: [NSNumber numberWithBool: YES] forKey: NSURLIsExcludedFromBackupKey error: &error];
    if(!success){
        NSLog(@"Error excluding %@ from backup %@", [URL lastPathComponent], error);
    }
    [GLobalRealReachability startNotifier];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                            selector:@selector(networkChanged:)
                                                name:kRealReachabilityChangedNotification
                                              object:nil];
    
}

- (void)networkChanged:(NSNotification *)notification
{
    RealReachability *reachability = (RealReachability *)notification.object;
    ReachabilityStatus status = [reachability currentReachabilityStatus];
    NSLog(@"currentStatus:%@",@(status));
    [self nativeEventHandler:@"network_change" andParams:[NSString stringWithFormat:@"%@",@(status)]];
}

-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback{
    
    if ([type isEqualToString:@"phoneCall" ]){
        // 拨打电话号码
        NSString *url = [NSString stringWithFormat:@"tel:%@", params];
        [[UIApplication sharedApplication] openURL:[NSURL URLWithString:url] options:@{} completionHandler:nil];
        [self nativeCallbackHandler:callback andParams:@"1"];

    }else if([type isEqualToString:@"vibrate" ]){
        // 震动
        AudioServicesPlaySystemSound(kSystemSoundID_Vibrate);
        [self nativeCallbackHandler:callback andParams:@"1"];

    }else if([type isEqualToString:@"nslog" ]){
        // 震动
        NSLog(@"nslog->%@",params);
        [self nativeCallbackHandler:callback andParams:@"1"];

    }else if([type isEqualToString:@"network" ]){
        // 查询网络状态
        ReachabilityStatus status = [GLobalRealReachability currentReachabilityStatus];
        [self nativeCallbackHandler:callback andParams:[NSString stringWithFormat:@"%@",@(status)]];

    }else if ([type isEqualToString:@"clipboard" ]){
        // 剪切板
        UIPasteboard *pasteboard = [UIPasteboard generalPasteboard];
        pasteboard.string = params;
        [self nativeCallbackHandler:callback andParams:@"1"];
    }
    else if ([type isEqualToString:@"cutImage" ]){
        
        NSDictionary *dict = [JSONUtil parse:params];
        NSString *filePath = dict[@"file"];
        CGRect rect;
        rect.origin.x = [dict[@"x"] floatValue];
        rect.origin.y = [dict[@"y"] floatValue];
        rect.size.width = [dict[@"w"] floatValue];
        rect.size.height = [dict[@"h"] floatValue];
        [self cutImage:filePath andRect:rect];
        [self nativeCallbackHandler:callback andParams:@"1"];
    }
    else if ([type isEqualToString:@"restart" ]){
        [self nativeCallbackErrorHandler:callback andParams:@"iOS不支持App自启动！"];
    }
    else if ([type isEqualToString:@"rate" ]){
        // 评分
        if([SKStoreReviewController respondsToSelector:@selector(requestReview)]) {// iOS 10.3 以上支持
           [SKStoreReviewController requestReview];
        } else { // iOS 10.3 之前的使用这个
          NSString  * nsStringToOpen = [NSString  stringWithFormat: @"itms-apps://itunes.apple.com/app/id%@?action=write-review",params];//替换为对应的APPID
        NSURL *facebookURL = [NSURL URLWithString:nsStringToOpen];
          if ([[UIApplication sharedApplication] canOpenURL:facebookURL]) {
              [[UIApplication sharedApplication] openURL:facebookURL options:@{} completionHandler:nil];
          }
        }
        [self nativeCallbackHandler:callback andParams:@"1"];
    }
    else if ([type isEqualToString:@"appStore" ] ||[type isEqualToString:@"update" ] || [type isEqualToString:@"browser" ] ){
        // 打开链接
//        [[UIApplication sharedApplication] openURL:[NSURL URLWithString:params]];
        // 打开链接
        NSArray *arr = [params componentsSeparatedByString:@"|"];

        NSURL *facebookURL = [NSURL URLWithString:arr[0]];
        if ([[UIApplication sharedApplication] canOpenURL:facebookURL]) {
            NSLog(@"canOpenURL %@",arr[0]);
            [[UIApplication sharedApplication] openURL:facebookURL options:@{} completionHandler:nil];
        } else {
            if ( arr.count > 1 ){
                facebookURL = [NSURL URLWithString:arr[1]];
                [[UIApplication sharedApplication] openURL:facebookURL options:@{} completionHandler:nil];
            }
        }
        [self nativeCallbackHandler:callback andParams:@"1"];
    }
    else if ([type isEqualToString:@"copyDir" ])
    {
        NSArray *arr = [params componentsSeparatedByString:@"|"];
        [self copyDir:arr[0] toDestination:arr[1]];
        [self nativeCallbackHandler:callback andParams:@"1"];
    }
    else if ([type isEqualToString:@"base64ToImage" ]){

        NSArray *arr = [params componentsSeparatedByString:@"|"];
        NSString *filePath = [NSTemporaryDirectory() stringByAppendingPathComponent:arr[0]];
//        [[NSBundle mainBundle] ]
//        NSString *base64str = [NSString stringWithContentsOfFile:[[NSBundle mainBundle] pathForResource:@"bb64.txt" ofType:nil] encoding:NSUTF8StringEncoding error:nil];
        NSString *base64str = arr[1];
        
        NSData *imageData = [NSData dataFromBase64String:base64str];
        if (imageData == nil){
            NSLog(@"NSData == bnil");
        }
        UIImage *image = [UIImage imageWithData:imageData];
        image = [self scaleImage:image toScale:0.75];
        imageData = UIImageJPEGRepresentation(image, 0.5);
        NSFileManager *fm = [NSFileManager defaultManager];
        [fm removeItemAtPath:filePath error:nil];
        [imageData writeToFile:filePath atomically:YES];
        [self nativeCallbackHandler:callback andParams:filePath];
    }
    else if([type isEqualToString:@"get_screen_light"]){
        CGFloat brightness = [UIScreen mainScreen].brightness;
        [self nativeCallbackHandler:callback andParams:[NSString stringWithFormat:@"%@", @(brightness)]];
        NSLog(@"获取当前屏幕亮度：%@", @(brightness));
    }
    else{
        [self nativeCallbackHandler:callback andParams:@"0"];
    }
}

/**
*  1, 按图片最大边成比例缩放图片
*
*  @param image   图片
*  @param maxSize 图片的较长那一边目标缩到的(宽度／高度)
*
*  @return        等比缩放后的图片
*/
- (UIImage *)scaleImage:(UIImage *)image maxSize:(CGFloat)maxSize {
    NSData *data = UIImageJPEGRepresentation(image, 1.0);
    if(data.length < 200 * 1024){//0.25M-0.5M(当图片小于此范围不压缩)
        return image;
    }
    CGFloat imageWidth = image.size.width;
    CGFloat imageHeight = image.size.height;
    CGFloat targetWidth = imageWidth;
    CGFloat targetHeight = imageHeight;
    CGFloat imageMaxSize = MAX(imageWidth, imageHeight);
    if (imageMaxSize > maxSize) {
        CGFloat scale = 0;
        if (imageWidth >= imageHeight) {// 宽长
            scale = maxSize / imageWidth;
            targetWidth = maxSize;
            targetHeight = imageHeight * scale;
        } else { // 高长
            scale = maxSize / imageHeight;
            targetHeight = maxSize;
            targetWidth = imageWidth * scale;
        }
        UIGraphicsBeginImageContext(CGSizeMake(targetWidth, targetHeight));
        [image drawInRect:CGRectMake(0, 0, targetWidth, targetHeight)];
        UIImage *scaledImage = UIGraphicsGetImageFromCurrentImageContext();
        UIGraphicsEndImageContext();
        return scaledImage;
    }
    return image;
}
/**
*  2, 图片支持等比缩放
*
*  @param image   图片
*  @param maxSize 缩放比例(通常0～1之间)
*
*  @return        等比缩放后的图片
*/
- (UIImage *)scaleImage:(UIImage *)image toScale:(float)scaleSize {
    UIGraphicsBeginImageContext(CGSizeMake(image.size.width *scaleSize, image.size.height * scaleSize));
    [image drawInRect:CGRectMake(0, 0, image.size.width * scaleSize, image.size.height * scaleSize)];
    UIImage *scaledImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return scaledImage;
}

-(void)cutImage:(NSString*)path andRect:( CGRect )rect{
    // 大图 bigImage
    // 定义 myImageRect ，截图的区域
    CGRect myImageRect = rect;
    UIImage * bigImage= [UIImage imageNamed:path];
    CGImageRef imageRef = bigImage. CGImage ;
    CGImageRef subImageRef = CGImageCreateWithImageInRect (imageRef, myImageRect);
    CGSize size;
    size. width = rect. size . width ;
    size. height = rect. size . height ;
    UIGraphicsBeginImageContext (size);
    CGContextRef context = UIGraphicsGetCurrentContext ();
    CGContextDrawImage (context, myImageRect, subImageRef);
    UIImage * smallImage = [ UIImage imageWithCGImage :subImageRef];
    UIGraphicsEndImageContext ();
    //png格式
//    NSData *imagedata=UIImagePNGRepresentation(smallImage);
    //JEPG格式
    NSData *imagedata=UIImageJPEGRepresentation(smallImage, 0.8);
    [imagedata writeToFile:path atomically:YES];
    NSLog(@"图片保存在%@",path);
//    UIImageWriteToSavedPhotosAlbum(smallImage,self,@selector(image:didFinishSavingWithError:contextInfo:),nil);

}
//保存相片的回调方法
//- (void)image:(UIImage *)image didFinishSavingWithError:(NSError *)error contextInfo:(void *)contextInfo
//{
//    if (error) {
//        NSLog(@"保存失败");
//    } else {
//        NSLog(@"成功保存到相册");
//    }
//}

-(void) copyDir:(NSString*) dir toDestination:(NSString*)dir2{
    
    NSFileManager *fm = [NSFileManager defaultManager];
    NSArray<NSString *> *files = [fm subpathsAtPath:dir];
    if (![fm fileExistsAtPath:dir2] ){
        [fm createDirectoryAtPath:dir2 withIntermediateDirectories:YES attributes:nil error:nil];
    }
    int num = 0;
    for (NSString *file in files){
        NSString *fullpath = [dir stringByAppendingPathComponent:file];
        BOOL isDir = FALSE;
        if ([fm fileExistsAtPath:fullpath isDirectory:&isDir]){
        }
        if (isDir){
            continue;
        }
        NSString *destFullpath = [dir2 stringByAppendingPathComponent:file];
        if ([fm fileExistsAtPath:destFullpath isDirectory:&isDir]){
            NSError *error = nil;
            [fm removeItemAtPath:destFullpath error:&error];
            if (error){
                NSLog(@"removeItemAtPath:%@",error);
            }
        }
        NSString *ddDir = [destFullpath stringByDeletingLastPathComponent];
        if (![fm fileExistsAtPath:ddDir]){
            [fm createDirectoryAtPath:ddDir withIntermediateDirectories:YES attributes:nil error:nil];
        }
        NSError *error = nil;
        [fm moveItemAtPath:fullpath toPath:destFullpath error:&error];
        if (error){
            NSLog(@"moveItemAtPath:%@",error);
        }else{
            NSLog(@"移动成功：%@",destFullpath);
            num++;
        }
    }
    [fm removeItemAtPath:dir error:nil];
    NSLog(@"一共 %d 文件拷贝成功",num);
}


/**
 *  监听手机晃动
 */

@end
