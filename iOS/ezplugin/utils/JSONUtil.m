//
//  JSONUtil.m
//  SlotsMarket
//
//  Created by howe on 2019/8/16.
//

#import "JSONUtil.h"

@implementation JSONUtil
+(NSString*)stringify:(id)arrayOrDict{
    if ([NSJSONSerialization isValidJSONObject:arrayOrDict]) {
        // opt必须设置为0，否则导出的json只适合阅读，无法进一步解析
        NSError *error = nil;
        NSData *data = [NSJSONSerialization dataWithJSONObject:arrayOrDict options:0 error:&error];
        if (error != nil){
            NSLog(@" stringify error %@",error);
        }
        NSString *json = [[NSString alloc]initWithData:data encoding:NSUTF8StringEncoding];
        return json;
    }
    return @"{}";
}

+(id)parse:(NSString *)jsonString
{
    NSData *jsonData= [jsonString dataUsingEncoding:NSUTF8StringEncoding];
    if (jsonData == nil){
        NSLog(@"[PluginCore] Error: NSString->NSData错误：%@",jsonString);
        return nil;
    }
    NSError *error = nil;
    id jsonObject = [NSJSONSerialization JSONObjectWithData:jsonData options:0 error:&error];
    if (jsonObject != nil && error == nil) {
        return jsonObject;
    } else {
        NSLog(@"[PluginCore] Error: json解析错误：%@",error);
        return nil;
    }
}
@end
