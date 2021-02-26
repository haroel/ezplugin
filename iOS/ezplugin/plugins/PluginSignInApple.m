//
//  PluginSignInApple.m
//  fishing-mobile
//
//  Created by howe on 2020/7/22.
//

#import "PluginSignInApple.h"
#import <AuthenticationServices/AuthenticationServices.h>
#import "JSONUtil.h"

@implementation PluginSignInApple

-(void)initPlugin:(NSDictionary*)params{
    if ( self.isInited){
          return;
      }
    self.userInfo = [NSMutableDictionary dictionary];
    if (@available(iOS 13.0, *)) {
        // 注意 存储用户标识信息需要使用钥匙串来存储 这里使用NSUserDefaults 做的简单示例
        NSString * userIdentifier = [[NSUserDefaults standardUserDefaults] valueForKey:@"signInAppleID"];
        if (userIdentifier) {
            ASAuthorizationAppleIDProvider * appleIDProvider = [[ASAuthorizationAppleIDProvider alloc] init];
            [appleIDProvider getCredentialStateForUserID:userIdentifier
                                              completion:^(ASAuthorizationAppleIDProviderCredentialState credentialState, NSError * _Nullable error) {
                switch (credentialState) {
                    case ASAuthorizationAppleIDProviderCredentialAuthorized:
                        // 授权状态有效
                        break;
                    case ASAuthorizationAppleIDProviderCredentialRevoked:
                        // 苹果账号登录的凭据已被移除，需解除绑定并重新引导用户使用苹果登录
                        [[NSUserDefaults standardUserDefaults] removeObjectForKey:@"signInAppleID"];
                        break;
                    case ASAuthorizationAppleIDProviderCredentialNotFound:
                        // 未登录授权，直接弹出登录页面，引导用户登录
                        [[NSUserDefaults standardUserDefaults] removeObjectForKey:@"signInAppleID"];
                        break;
                    case ASAuthorizationAppleIDProviderCredentialTransferred:
                        // 授权AppleID提供者凭据转移
                        break;
                }
            }];
        }
        [self perfomExistingAccountSetupFlows];
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(handleSignInWithAppleStateChanged:)
                                                     name:ASAuthorizationAppleIDProviderCredentialRevokedNotification
                                                   object:nil];
    }
}

- (void)handleSignInWithAppleStateChanged:(NSNotification *)notification
{
    // Sign the user out, optionally guide them to sign in again
    NSLog(@"%@", notification.userInfo);
}

-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback{
    if ([type isEqualToString:@"login"] ){
        if (self.userInfo.count > 0){
            NSString *userInfo =[JSONUtil stringify:self.userInfo];
            [self nativeEventHandler:@"logined" andParams:userInfo];
            return;
        }
        [self signInWithApple];
    }else if ( [type isEqualToString:@"logout"]){
        [self.userInfo removeAllObjects];
    }
}

- (void)signInWithApple
{
    if (@available(iOS 13.0, *)) {
        NSLog(@"%s", __FUNCTION__);

        // A mechanism for generating requests to authenticate users based on their Apple ID.
            // 基于用户的Apple ID授权用户，生成用户授权请求的一种机制
            ASAuthorizationAppleIDProvider *appleIDProvider = [ASAuthorizationAppleIDProvider new];
            // Creates a new Apple ID authorization request.
            // 创建新的AppleID 授权请求
            ASAuthorizationAppleIDRequest *request = appleIDProvider.createRequest;
            // The contact information to be requested from the user during authentication.
            // 在用户授权期间请求的联系信息
            request.requestedScopes = @[ASAuthorizationScopeFullName, ASAuthorizationScopeEmail];
            // A controller that manages authorization requests created by a provider.
            // 由ASAuthorizationAppleIDProvider创建的授权请求 管理授权请求的控制器
            ASAuthorizationController *controller = [[ASAuthorizationController alloc] initWithAuthorizationRequests:@[request]];
            // A delegate that the authorization controller informs about the success or failure of an authorization attempt.
            // 设置授权控制器通知授权请求的成功与失败的代理
            controller.delegate = self;
            // A delegate that provides a display context in which the system can present an authorization interface to the user.
            // 设置提供 展示上下文的代理，在这个上下文中 系统可以展示授权界面给用户
            controller.presentationContextProvider = self;
            // starts the authorization flows named during controller initialization.
            // 在控制器初始化期间启动授权流
            [controller performRequests];
    }else{
        NSLog(@"系统不支持Apple登录");
    }
}

#pragma mark - ASAuthorizationControllerDelegate
//授权成功的回调
/**
 当授权成功后，我们可以通过这个拿到用户的 userID、email、fullName、authorizationCode、identityToken 以及 realUserStatus 等信息。
 */
-(void)authorizationController:(ASAuthorizationController *)controller didCompleteWithAuthorization:(ASAuthorization *)authorization API_AVAILABLE(ios(13.0)) {
    
    if ([authorization.credential isKindOfClass:[ASAuthorizationAppleIDCredential class]]) {
        
        // 用户登录使用ASAuthorizationAppleIDCredential
        ASAuthorizationAppleIDCredential *credential = authorization.credential;
        
        //苹果用户唯一标识符，该值在同一个开发者账号下的所有 App 下是一样的，开发者可以用该唯一标识符与自己后台系统的账号体系绑定起来。
        NSString *userIdentifier = credential.user;
        NSString *state = credential.state;
        NSPersonNameComponents *fullName = credential.fullName;
        //苹果用户信息，邮箱
        NSString *email = credential.email;
        NSString *authorizationCode = [[NSString alloc] initWithData:credential.authorizationCode encoding:NSUTF8StringEncoding]; // refresh token
        /**
         验证数据，用于传给开发者后台服务器，然后开发者服务器再向苹果的身份验证服务端验证本次授权登录请求数据的有效性和真实性，详见 Sign In with Apple REST API。如果验证成功，可以根据 userIdentifier 判断账号是否已存在，若存在，则返回自己账号系统的登录态，若不存在，则创建一个新的账号，并返回对应的登录态给 App。
         */
        NSString *identityToken = [[NSString alloc] initWithData:credential.identityToken encoding:NSUTF8StringEncoding];
        /**
         用于判断当前登录的苹果账号是否是一个真实用户
         取值有：unsupported、unknown、likelyReal。
         */
        ASUserDetectionStatus realUserStatus = credential.realUserStatus;
        NSLog(@"state: %@", state);
        NSLog(@"userID: %@", userIdentifier);
        NSLog(@"fullName: %@", fullName);
        NSLog(@"email: %@", email);
        NSLog(@"authorizationCode: %@", authorizationCode);
        NSLog(@"identityToken: %@", identityToken);
        NSLog(@"realUserStatus: %@", @(realUserStatus));
        
        // 存储userId到keychain中，代码省略
        [self.userInfo setObject:userIdentifier forKey:@"id"];
        if (fullName.nickname != nil){
            [self.userInfo setObject:fullName.nickname forKey:@"name"];
        }else if (fullName.givenName != nil){
            [self.userInfo setObject:fullName.givenName forKey:@"name"];
        }else if (fullName.familyName != nil){
            [self.userInfo setObject:fullName.familyName forKey:@"name"];
        }
        NSString *userInfo =[JSONUtil stringify:self.userInfo];
        [self nativeEventHandler:@"logined" andParams:userInfo];
        [[NSUserDefaults standardUserDefaults] setObject:userIdentifier forKey:@"signInAppleID"];
               
    } else if ([authorization.credential isKindOfClass:[ASPasswordCredential class]]) {
        // 用户登录使用现有的密码凭证
        ASPasswordCredential *passwordCredential = authorization.credential;
        // 密码凭证对象的用户标识 用户的唯一标识
        NSString *userIdentifier = passwordCredential.user;
        // 密码凭证对象的密码
        NSString *password = passwordCredential.password;
        NSLog(@"userIdentifier: %@", userIdentifier);
        NSLog(@"password: %@", password);
        [self.userInfo setObject:userIdentifier forKey:@"id"];
        NSString *userInfo =[JSONUtil stringify:self.userInfo];
        [self nativeEventHandler:@"logined" andParams:userInfo];
        [[NSUserDefaults standardUserDefaults] setObject:userIdentifier forKey:@"signInAppleID"];
    }
    
}
//失败的回调
-(void)authorizationController:(ASAuthorizationController *)controller didCompleteWithError:(NSError *)error API_AVAILABLE(ios(13.0)) {
    NSLog(@"%s", __FUNCTION__);
    NSLog(@"%@", error);
    NSString *errorMsg = nil;
    switch (error.code) {
        case ASAuthorizationErrorCanceled:
            errorMsg = @"用户取消了授权请求";
            [self nativeEventHandler:@"login_cancel" andParams:error.localizedDescription];
            return;
            break;
        case ASAuthorizationErrorFailed:
            errorMsg = @"授权请求失败";
            break;
        case ASAuthorizationErrorInvalidResponse:
            errorMsg = @"授权请求响应无效";
            break;
        case ASAuthorizationErrorNotHandled:
            errorMsg = @"未能处理授权请求";
            break;
        case ASAuthorizationErrorUnknown:
            errorMsg = @"授权请求失败未知原因";
            break;
    }
    NSLog(@"错误信息：%@", errorMsg);
    [self nativeEventHandler:@"login_error" andParams:error.localizedDescription];
}

#pragma mark - ASAuthorizationControllerPresentationContextProviding
//告诉代理应该在哪个window 展示授权界面给用户
-(ASPresentationAnchor)presentationAnchorForAuthorizationController:(ASAuthorizationController *)controller API_AVAILABLE(ios(13.0)) {
    UIWindow * window = [[UIApplication sharedApplication] keyWindow];
    return window;
}


- (void)perfomExistingAccountSetupFlows {
    if (@available(iOS 13.0, *)) {
        NSLog(@"%s", __FUNCTION__);
        // A mechanism for generating requests to authenticate users based on their Apple ID.
        // 基于用户的Apple ID授权用户，生成用户授权请求的一种机制
        ASAuthorizationAppleIDProvider *appleIDProvider = [ASAuthorizationAppleIDProvider new];
        // An OpenID authorization request that relies on the user’s Apple ID.
        // 授权请求依赖于用于的AppleID
        ASAuthorizationAppleIDRequest *authAppleIDRequest = [appleIDProvider createRequest];
        // A mechanism for generating requests to perform keychain credential sharing.
        // 为了执行钥匙串凭证分享生成请求的一种机制
        ASAuthorizationPasswordRequest *passwordRequest = [[ASAuthorizationPasswordProvider new] createRequest];
        
        NSMutableArray <ASAuthorizationRequest *>* mArr = [NSMutableArray arrayWithCapacity:2];
        if (authAppleIDRequest) {
            [mArr addObject:authAppleIDRequest];
        }
        if (passwordRequest) {
            [mArr addObject:passwordRequest];
        }
        // ASAuthorizationRequest：A base class for different kinds of authorization requests.
        // ASAuthorizationRequest：对于不同种类授权请求的基类
        NSArray <ASAuthorizationRequest *>* requests = [mArr copy];
        
        // A controller that manages authorization requests created by a provider.
        // 由ASAuthorizationAppleIDProvider创建的授权请求 管理授权请求的控制器
        // Creates a controller from a collection of authorization requests.
        // 从一系列授权请求中创建授权控制器
        ASAuthorizationController *authorizationController = [[ASAuthorizationController alloc] initWithAuthorizationRequests:requests];
        // A delegate that the authorization controller informs about the success or failure of an authorization attempt.
        // 设置授权控制器通知授权请求的成功与失败的代理
        authorizationController.delegate = self;
        // A delegate that provides a display context in which the system can present an authorization interface to the user.
        // 设置提供 展示上下文的代理，在这个上下文中 系统可以展示授权界面给用户
        authorizationController.presentationContextProvider = self;
        // starts the authorization flows named during controller initialization.
        // 在控制器初始化期间启动授权流
        [authorizationController performRequests];
    }
}

@end
