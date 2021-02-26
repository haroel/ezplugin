//
//  PluginFacebook.m
//  shawngames-mobile
//
//  Created by howe on 2019/8/26.
//
#import "PluginCore.h"

#import "PluginFacebook.h"
#import <FBSDKCoreKit/FBSDKCoreKit.h>
#import <FBSDKLoginKit/FBSDKLoginKit.h>

#import "JSONUtil.h"


@implementation PluginFacebook
-(void)initPlugin:(NSDictionary*)params{
    if (self.isInited){
        return;
    }
    NSDictionary * launchOptions =[[PluginCore shareInstance] getGlobalVariable:@"launchOptions"];
    [[FBSDKApplicationDelegate sharedInstance] application:[UIApplication sharedApplication] didFinishLaunchingWithOptions:launchOptions];
}

-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback{
    if ([type isEqualToString:@"login"] ){
        NSUserDefaults *userDefault = [NSUserDefaults standardUserDefaults];
        NSString *saveKey =[NSString stringWithFormat:@"%@UserInfo",NSStringFromClass([self class])];
        NSString *userInfo =[userDefault objectForKey:saveKey];
        if (userInfo!= nil){
            NSDictionary *jobj11 = [JSONUtil parse:userInfo];
            if (jobj11!= nil){
                NSMutableDictionary *jobj = [NSMutableDictionary dictionaryWithDictionary:jobj11];
                long nowtime =  [[NSDate date] timeIntervalSince1970];
                if ( [jobj objectForKey:@"localExpire"] ){
                    NSNumber *expireTime1 = [jobj objectForKey:@"localExpire"];
                    long expireTime = expireTime1.longValue;
                    if (nowtime < expireTime){
                        NSLog(@"使用上次保存的facebook信息登录 %@",userInfo);
                        [self nativeEventHandler:@"logined" andParams:userInfo];
                        return;
                    }else{
                        NSLog(@"上次保存的facebook信息已经过期，需要重新登录！ %@",userInfo);
                    }
                }else{
                    int expirePassTime = 7 * 24 * 3600; // 7天有效期
                    [jobj setObject:@(nowtime + expirePassTime) forKey:@"localExpire" ];
                    userInfo = [JSONUtil stringify:jobj];
                    if (userInfo){
                        [userDefault setObject:userInfo forKey:saveKey];
                        NSLog(@"使用上次保存的facebook信息登录 %@",userInfo);
                        [self nativeEventHandler:@"logined" andParams:userInfo];
                        return;
                    }
                }
            }
        }
        if ([FBSDKAccessToken currentAccessToken] ) {
            // User is signed in.
            NSString *userID =[FBSDKAccessToken currentAccessToken].userID;
            NSLog(@"[PluginFacebook] 用户已登录 %@",userID);
            [self getUserInfoByUserID:userID];
        } else {
            // No user is signed in.
            [self doFBLogin];
        }
    }else if ( [type isEqualToString:@"logout"]){
        FBSDKLoginManager *login = [[FBSDKLoginManager alloc] init];
        [login logOut];
        [self nativeCallbackHandler:callback andParams:@""];
        NSUserDefaults *userDefault = [NSUserDefaults standardUserDefaults];
        NSString *saveKey =[NSString stringWithFormat:@"%@UserInfo",NSStringFromClass([self class])];
        [userDefault removeObjectForKey:saveKey];
        //        NSError *signOutError;
        //        BOOL status = [[FIRAuth auth] signOut:&signOutError];
        //        if (!status) {
        //            NSLog(@"Error signing out: %@", signOutError);
        //            return;
        //        }
    }else if( [type isEqualToString:@"share"]){
        FBSDKShareLinkContent *content = [[FBSDKShareLinkContent alloc] init];
        content.contentURL = [NSURL URLWithString:params];
        
        UIWindow * window = [[UIApplication sharedApplication] keyWindow];

        [FBSDKShareDialog showFromViewController:window.rootViewController
            withContent:content
               delegate:self];
    
    }
}

#pragma mark -
#pragma mark FBshare login
-(void)doFBLogin{
    NSArray * fbPermissions = @[@"public_profile",@"email"];
    UIWindow *window = [UIApplication sharedApplication].keyWindow;
    FBSDKLoginManager *login = [[FBSDKLoginManager alloc] init];
    [login logOut];
    [login logInWithPermissions:fbPermissions fromViewController:window.rootViewController handler:^(FBSDKLoginManagerLoginResult * _Nullable result, NSError * _Nullable error) {
        if (error) {
            NSLog(@"[PluginFacebook] Process error %@",error);
            [self nativeEventHandler:@"login_error" andParams:[NSString stringWithFormat:@"error:%@",error.userInfo]];
        } else if (result.isCancelled) {
            NSLog(@"[PluginFacebook] Cancelled %@",result);
            [self nativeEventHandler:@"login_cancel" andParams:@"cancel"];
        } else {
            NSLog(@"[PluginFacebook] Logged in");
            [self getUserInfoByUserID:result.token.userID];
            return;
        }
    }];
}

-(void)getUserInfoByUserID:(NSString*)userID{
    NSLog(@" [PluginFacebook]  getUserInfoByUserID userID = %@",userID);
    NSDictionary*params= @{@"fields":@"id,name,email,age_range,first_name,last_name,link,gender,locale,picture,timezone"};
    FBSDKGraphRequest *request = [[FBSDKGraphRequest alloc]
                                  initWithGraphPath:userID
                                  parameters:params
                                  HTTPMethod:@"GET"];
    [request startWithCompletionHandler:^(FBSDKGraphRequestConnection *connection, id result, NSError *error) {
        if (error || result==nil ){
            NSLog(@"[PluginFacebook] getUserInfoByUserID errror = %@",error);
            [self nativeEventHandler:@"login_error" andParams:@"Facebook loginError"];
            return;
        }
        NSMutableDictionary *dict = [NSMutableDictionary dictionaryWithDictionary:result];
        
        int expirePassTime = 7 * 24 * 3600; // 7天有效期
        long nowtime =  [[NSDate date] timeIntervalSince1970];
        [dict setObject:@(nowtime + expirePassTime) forKey:@"localExpire"];
        
        NSString * userInfo =[JSONUtil stringify:dict];
        NSLog(@"[PluginFacebook]  getUserInfoByUserID userInfo = %@",userInfo);
        [self nativeEventHandler:@"logined" andParams:userInfo];
        
        NSUserDefaults *userDefault = [NSUserDefaults standardUserDefaults];
        NSString *saveKey =[NSString stringWithFormat:@"%@UserInfo",NSStringFromClass([self class])];
        [userDefault setObject:userInfo forKey:saveKey];
    }];
}

- (BOOL)application:(UIApplication *)application
            openURL:(NSURL *)url
            options:(NSDictionary<UIApplicationOpenURLOptionsKey,id> *)options {
    // Add any custom logic here.
    return [[FBSDKApplicationDelegate sharedInstance] application:application
                                                          openURL:url
                                                sourceApplication:options[UIApplicationOpenURLOptionsSourceApplicationKey]
                                                       annotation:options[UIApplicationOpenURLOptionsAnnotationKey]
            ];
    
}

#pragma mark -
#pragma mark FBshare callback
/**
  Sent to the delegate when the share completes without error or cancellation.
 @param sharer The FBSDKSharing that completed.
 @param results The results from the sharer.  This may be nil or empty.
 */
- (void)sharer:(id<FBSDKSharing>)sharer didCompleteWithResults:(NSDictionary<NSString *, id> *)results{
    NSString *params = @"";
    if (results){
        params = [JSONUtil stringify:results];
    }
    [self nativeEventHandler:@"share_success" andParams:params];
}

/**
  Sent to the delegate when the sharer encounters an error.
 @param sharer The FBSDKSharing that completed.
 @param error The error.
 */
- (void)sharer:(id<FBSDKSharing>)sharer didFailWithError:(NSError *)error{
    [self nativeEventHandler:@"share_error" andParams:[NSString stringWithFormat:@"%ld",(long)error.code]];

}

/**
  Sent to the delegate when the sharer is cancelled.
 @param sharer The FBSDKSharing that completed.
 */
- (void)sharerDidCancel:(id<FBSDKSharing>)sharer{
    [self nativeEventHandler:@"share_cancel" andParams:@""];
}

//-(void)FIRAuthLogin{
//    FIRAuthCredential *credential = [FIRFacebookAuthProvider
//                                     credentialWithAccessToken:[FBSDKAccessToken currentAccessToken].tokenString];
//    [[FIRAuth auth] signInWithCredential:credential
//                              completion:^(FIRAuthDataResult * _Nullable authResult,
//                                           NSError * _Nullable error) {
//                                  // User successfully signed in. Get user data from the FIRUser object
//                                  if (error || authResult == nil) {
//                                      [self nativeEventHandler:@"login_error" andParams:[NSString stringWithFormat:@"error:%@",error.userInfo]];
//                                      return;
//                                  }
//                                  FIRUser *user = authResult.user;
//                                  [self returnToCocos:user];
//                              }];
//}
//
//-(void)returnToCocos:(id)user{

//    NSMutableDictionary * dict = [NSMutableDictionary dictionary];
//    [dict setObject:user[@"id"] forKey:@"uid"];
//    [dict setObject:user[@"name"] forKey:@"displayName"];
//    [dict setObject:user[@"picture"][@"data"][@"url"] forKey:@"photoUrl"];
//
//    if (user[@"email"] != nil){
//        [dict setObject:user[@"email"] forKey:@"email"];
//    }
//    [self nativeEventHandler:@"logined" andParams:[JSONUtil stringify:user]];
//    NSLog(@"facebook登录结果 %@",dict);
//}


@end
