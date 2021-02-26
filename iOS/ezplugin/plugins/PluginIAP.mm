//
//  PluginIAP.m
//  Game
//
//  Created by howe on 2017/9/13.
//
//

#import "PluginIAP.h"
#import "PluginCore.h"
#import "JSONUtil.h"

@implementation PluginIAP

-(void)initPlugin:(NSDictionary*)params
{
    if (self.isInited){
        return;
    }
    id vinfo = [params objectForKey:@"autoVerify"];
    if (vinfo){
        // 是否自动开启验证
        [IAPApi Instance].autoVerify = [vinfo boolValue];
    }
    [IAPApi Instance].delegate = self;
#if defined(COCOS2D_DEBUG) && (COCOS2D_DEBUG > 0)
    [IAPApi Instance].debugMode = YES;
#else
    [IAPApi Instance].debugMode = NO;
#endif
    
}


-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback{
    if ([type isEqualToString:@"pay"]){
        NSArray *arr = [params componentsSeparatedByString:@"|"];
        [[IAPApi Instance] buy:arr[0] billNo:arr[1]];
    }else if ([type isEqualToString:@"list"]){
        NSArray *arr = [params componentsSeparatedByString:@"|"];
        [[IAPApi Instance] getProductList:arr];
    }else if ([type isEqualToString:@"verify"]){
        IAPApi *api = [IAPApi Instance];
        [api verifyReceipt:params andDebug:NO withResultHandler:^(NSError *error, NSDictionary *response) {
            if (error){
                [self nativeCallbackErrorHandler:callback andParams:[error localizedDescription] ];
            }else{
                [self nativeCallbackHandler:callback andParams:[JSONUtil stringify:response]];
            }
        }];
    }
}

#pragma mark -
#pragma mark EZIAPDelegate
- (void) payResult:(NSDictionary*)payInfo{
    // 支付成功，下一步需做订单校验，如果autoVerify = true，则自动完成
    [self nativeEventHandler:@"pay_success" andParams:[JSONUtil stringify:payInfo]];
}

- (void) verifyResult:(NSString*)billNO andProductID:(NSString*)productID andResult:(NSDictionary*)verfyInfo{
    NSMutableDictionary *dict = [NSMutableDictionary dictionaryWithDictionary:verfyInfo];
    [dict setObject:billNO forKey:@"billNO"];
    [dict setObject:productID forKey:@"productID"];
    [self nativeEventHandler:@"verify_result" andParams:[JSONUtil stringify:dict]];
}

- (void) productList:(NSArray*)list{
    [self nativeEventHandler:@"list_success" andParams:[JSONUtil stringify:list]];
}

- (void) IAPFailed:(NSString*)billNO andProductID:(NSString*)productID widthError:(NSError*)error{
    NSMutableDictionary *ret = [NSMutableDictionary dictionary];
    if (error){
        NSLog(@"[PluginIAP] %@ %@ error = %@",productID,billNO,error);
        NSString *desc = [error localizedDescription];
        switch (error.code) {
            case SKErrorPaymentCancelled:
            {
                desc = @"The payment has been cancelled.";
                break;
            }
            case SKErrorPaymentInvalid:
            {
                desc = @"Purchase identifier was invalid.";
                break;
            }
            case SKErrorStoreProductNotAvailable:
            {
                desc = @"Product is not available in the current storefront.";
                break;
            }
            default:
                break;
        }
        [PluginCore alert:@"Message" andMsg:desc];
        [ret setObject:desc forKey:@"msg"];
    }
    [ret setObject:billNO forKey:@"billNO"];
    [ret setObject:productID forKey:@"productID"];
    [self nativeEventHandler:@"pay_error" andParams:[JSONUtil stringify:ret]];
}

@end
