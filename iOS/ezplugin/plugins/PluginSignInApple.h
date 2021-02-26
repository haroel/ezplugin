//
//  PluginSignInApple.h
//  fishing-mobile
//
//  Created by howe on 2020/7/22.
//

#import "PluginBase.h"
#import <AuthenticationServices/AuthenticationServices.h>

NS_ASSUME_NONNULL_BEGIN

@interface PluginSignInApple : PluginBase<ASAuthorizationControllerDelegate,ASAuthorizationControllerPresentationContextProviding>
@property(nonatomic,strong) NSMutableDictionary *__nullable userInfo;

@end

NS_ASSUME_NONNULL_END
