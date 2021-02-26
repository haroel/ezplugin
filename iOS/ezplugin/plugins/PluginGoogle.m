//
//  PluginGoogle.m
//  shawngames-mobile
//
//  Created by howe on 2019/8/26.
//

#import "PluginGoogle.h"
#import <FirebaseCore/FirebaseCore.h>
#import <FirebaseAuth/FirebaseAuth.h>
#import "JSONUtil.h"

@implementation PluginGoogle

-(void)initPlugin:(NSDictionary*)params{
    // Google login
    GIDSignIn *signIn = [GIDSignIn sharedInstance];
    signIn.clientID = [FIRApp defaultApp].options.clientID;
    signIn.delegate = self;
    signIn.presentingViewController = [UIApplication sharedApplication].keyWindow.rootViewController;
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
                        NSLog(@"使用上次保存的google信息登录 %@",userInfo);
                        [self nativeEventHandler:@"logined" andParams:userInfo];
                        return;
                    }else{
                        NSLog(@"上次保存的google信息已经过期，需要重新登录！ %@",userInfo);
                    }
                }else{
                    int expirePassTime = 7 * 24 * 3600; // 7天有效期
                    [jobj setObject:@(nowtime + expirePassTime) forKey:@"localExpire" ];
                    userInfo = [JSONUtil stringify:jobj];
                    if (userInfo){
                        [userDefault setObject:userInfo forKey:saveKey];
                        NSLog(@"使用上次保存的google信息登录 %@",userInfo);
                        [self nativeEventHandler:@"logined" andParams:userInfo];
                        return;
                    }
                }
            }
        }
        GIDSignIn *signIn = [GIDSignIn sharedInstance];
        if ([signIn hasPreviousSignIn]){
            // 上次登录处理
            [signIn restorePreviousSignIn];
            GIDAuthentication *authentication = signIn.currentUser.authentication;
            if (authentication != nil || authentication.idToken != nil ){
                [self FIRAuthLogin:authentication];
            }else{
                [signIn signIn];
            }
        }else{
            [signIn signIn];
        }
        [self nativeCallbackHandler:callback andParams:@""];
    }else if ( [type isEqualToString:@"logout"]){
        [[GIDSignIn sharedInstance] signOut];

        //        NSError *signOutError;
        //        BOOL status = [[FIRAuth auth] signOut:&signOutError];
        //        if (!status) {
        //            NSLog(@"Error signing out: %@", signOutError);
        //            return;
        //        }
        [self nativeCallbackHandler:callback andParams:@""];
        NSUserDefaults *userDefault = [NSUserDefaults standardUserDefaults];
        NSString *saveKey =[NSString stringWithFormat:@"%@UserInfo",NSStringFromClass([self class])];
        [userDefault removeObjectForKey:saveKey];
    }else{
//        [FIRAnalytics logEventWithName:kFIREventSelectContent
//        parameters:@{
//                     kFIRParameterItemID:[NSString stringWithFormat:@"id-%@", self.title],
//                     kFIRParameterItemName:self.title,
//                     kFIRParameterContentType:@"image"
//                     }];
    }
}

- (void)signIn:(GIDSignIn *)signIn
didSignInForUser:(GIDGoogleUser *)user
     withError:(NSError *)error{
    if (error == nil) {
        GIDAuthentication *authentication = user.authentication;
        [self FIRAuthLogin:authentication];
    } else {
        NSLog(@"[PluginGoogle] signIn didSignInForUser errror = %@",error);
        [self nativeEventHandler:@"login_error" andParams:@"didSignInForUser error"];
    }
}

-(void)FIRAuthLogin:(GIDAuthentication*)authentication{
    FIRAuthCredential *credential = [FIRGoogleAuthProvider credentialWithIDToken:authentication.idToken
                                     accessToken:authentication.accessToken];
    [[FIRAuth auth] signInWithCredential:credential
                              completion:^(FIRAuthDataResult * _Nullable authResult,
                                           NSError * _Nullable error) {
                                  NSLog(@"【PluginGoogle】联合登录结果 %@ %@",authResult,error);
                                  // User successfully signed in. Get user data from the FIRUser object
                                  if (error || authResult == nil) {
                                      NSLog(@"[PluginGoogle] FIRAuthLogin error = %@",error);
                                      [self nativeEventHandler:@"login_error" andParams:@"FIRAuthLogin error"];
                                      return;
                                  }
                                  FIRUser *user = authResult.user;
                                  [self returnToCocos:user];
                              }];
}

// Finished disconnecting |user| from the app successfully if |error| is |nil|.
- (void)signIn:(GIDSignIn *)signIn
didDisconnectWithUser:(GIDGoogleUser *)user
     withError:(NSError *)error{
    if (error){
        NSLog(@"[PluginGoogle] signIn didDisconnectWithUser error = %@",error);
        [self nativeEventHandler:@"login_error" andParams:@"didDisconnectWithUser error"];
    }
}

-(void)returnToCocos:(FIRUser*)user{
    NSMutableDictionary * dict = [NSMutableDictionary dictionary];
    [dict setObject:user.uid forKey:@"id"];
    [dict setObject:user.displayName forKey:@"name"];
    [dict setObject:user.photoURL.absoluteString forKey:@"avatar"];
    if (user.email){
        [dict setObject:user.email forKey:@"email"];
    }
    if (user.phoneNumber){
        [dict setObject:user.phoneNumber forKey:@"phoneNumber"];
    }
    int expirePassTime = 7 * 24 * 3600; // 7天有效期
    long nowtime =  [[NSDate date] timeIntervalSince1970];
    [dict setObject:@(nowtime + expirePassTime) forKey:@"localExpire"];
    NSString *userInfo =[JSONUtil stringify:dict];
    [self nativeEventHandler:@"logined" andParams:userInfo];
    NSLog(@"google登录结果 %@",userInfo);
    
    NSUserDefaults *userDefault = [NSUserDefaults standardUserDefaults];
    NSString *saveKey =[NSString stringWithFormat:@"%@UserInfo",NSStringFromClass([self class])];
    [userDefault setObject:userInfo forKey:saveKey];
}

- (BOOL)application:(UIApplication *)application
            openURL:(NSURL *)url
            options:(NSDictionary<UIApplicationOpenURLOptionsKey,id> *)options {
    return [[GIDSignIn sharedInstance] handleURL:url];
}

@end
