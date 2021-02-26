//
//  PluginSystemControll.m
//  lanwangame-mobile
//
//  Created by lanwan on 2020/10/28.
//

#import "PluginSystemControll.h"
#import "PluginCore.h"
#import <AudioToolbox/AudioToolbox.h>

@implementation PluginSystemControll

BOOL toggleVibrate = YES;
CGFloat preScreenBrightness = 0.0f;

-(void)initPlugin:(NSDictionary*)params{
    // 监听手机音量变化
    [[NSNotificationCenter defaultCenter] addObserver:self
                                            selector:@selector(volumeChanged:)
                                                name:@"AVSystemController_SystemVolumeDidChangeNotification"
                                               object:nil];
    [[UIApplication sharedApplication] beginReceivingRemoteControlEvents];
    // 监听手机消息事件（晃动）
    NSNotificationCenter *ncenter = [NSNotificationCenter defaultCenter];
    NSOperationQueue *mainQueue = [NSOperationQueue mainQueue];
    [ncenter addObserverForName:@"motionEnded" object:nil queue:mainQueue usingBlock:^(NSNotification * _Nonnull note) {
        UIEventSubtype event = [note.object intValue];
        switch (event) {
            case UIEventSubtypeMotionShake:{
                if(toggleVibrate){
                    AudioServicesPlaySystemSound(kSystemSoundID_Vibrate);
                }
                [[PluginCore shareInstance] nativeEventHandler:@"PluginOS" andEvent:@"shake" andParams:@""];
//                [self nativeEventHandler:@"UIEventSubtypeMotionShake" andParams:@""];
                break;
            }
            default:
                break;
        }
    }];
    
    // 监听手机屏幕亮度变化,并记录初始化亮度
    preScreenBrightness = [UIScreen mainScreen].brightness;
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(screenBrightnessChange) name:UIScreenBrightnessDidChangeNotification object:nil];
    
    
//    [PluginCore alert:@"手机电量状态" andMsg:@"弹窗测试1"];
    

    // 监听手机翻转
    [[UIDevice currentDevice] beginGeneratingDeviceOrientationNotifications];
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(rotateViews:)
                                                     name:UIDeviceOrientationDidChangeNotification
                                                   object:nil];
}

- (void) rotateViews:(NSObject *)sender {
    
    UIDevice* device = [sender valueForKey:@"object"];
    
    switch (device.orientation) {
        case UIDeviceOrientationUnknown: {
            // do something
            NSLog(@"状态未知");
            [[PluginCore shareInstance] nativeEventHandler:@"PluginOS" andEvent:@"screen_change" andParams:@"-2"];
            break;
        }
        case UIDeviceOrientationPortrait: {
            // do something
            NSLog(@"朝下");
            [[PluginCore shareInstance] nativeEventHandler:@"PluginOS" andEvent:@"screen_change" andParams:@"1"];
            break;
        }
        case UIDeviceOrientationPortraitUpsideDown: {
            // do something
            NSLog(@"朝上");
            [[PluginCore shareInstance] nativeEventHandler:@"PluginOS" andEvent:@"screen_change" andParams:@"3"];
            break;
        }
        case UIDeviceOrientationLandscapeLeft: {
            // do something
            NSLog(@"向左");
            [[PluginCore shareInstance] nativeEventHandler:@"PluginOS" andEvent:@"screen_change" andParams:@"2"];
            break;
        }
        case UIDeviceOrientationLandscapeRight: {
            // do something
            NSLog(@"向右");
            [[PluginCore shareInstance] nativeEventHandler:@"PluginOS" andEvent:@"screen_change" andParams:@"4"];
            break;
        }
        case UIDeviceOrientationFaceUp: {
            // do something
            NSLog(@"平躺朝上");
            [[PluginCore shareInstance] nativeEventHandler:@"PluginOS" andEvent:@"screen_change" andParams:@"-1"];
            break;
        }
        case UIDeviceOrientationFaceDown: {
            // do something
            NSLog(@"平躺朝下");
            [[PluginCore shareInstance] nativeEventHandler:@"PluginOS" andEvent:@"screen_change" andParams:@"5"];
            break;
        }
    }
}

- (void)batteryLevelChanged:(NSNotification *)noti
{
    NSLog(@"电池电量 = %.2f",[UIDevice currentDevice].batteryLevel);
    UIDeviceBatteryState bs = [[UIDevice currentDevice] batteryState];
    [self checkBattery:bs];
}

- (void)batteryStatusChanged:(NSNotification *)noti
{
    NSLog(@"电池状态 = %ld",[UIDevice currentDevice].batteryState);
    UIDeviceBatteryState bs = [[UIDevice currentDevice] batteryState];
    [self checkBattery:bs];

}

/**
 *  监听手机音量变化
 */
-(void)volumeChanged:(NSNotification *)notification{
    float volume =
    [[[notification userInfo]
      objectForKey:@"AVSystemController_AudioVolumeNotificationParameter"]
     floatValue];
    NSLog(@"current volume = %f", volume);
    [[PluginCore shareInstance] nativeEventHandler:@"PluginOS" andEvent:@"volume_change" andParams:[NSString stringWithFormat:@"%@",@(volume)]];
}

/**
 * 手机亮度变化
 */
-(void)screenBrightnessChange{
    CGFloat brightness = [UIScreen mainScreen].brightness;
    // 过滤掉变化小于0.01的亮度变化
    if(brightness - preScreenBrightness > 0.01){
        NSLog(@"屏幕亮度发生变化: %@", @(brightness));
        [[PluginCore shareInstance] nativeEventHandler:@"PluginOS" andEvent:@"screen_light_change" andParams:[NSString stringWithFormat:@"%@",@(brightness)]];
    }
    preScreenBrightness = brightness;
    
}

/**
 * TS调用OC
 */
-(void)excute:(NSString*)type andParams:(NSString*)params andCallback:(int)callback{
    if([type isEqualToString:@"toggleVibrate"]){
        NSLog(@"打开/关闭 震动: %@", params);
        toggleVibrate = [params isEqualToString:@"1"] ? YES : NO;
    }
    else if([type isEqualToString:@"getBatteryState"]){
        // 监听手机是否充电--替换为TS轮询方式
        [[UIDevice currentDevice] setBatteryMonitoringEnabled:true];
        NSLog(@"是否激活: %@", @([UIDevice currentDevice].batteryMonitoringEnabled));
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(batteryLevelChanged:) name:UIDeviceBatteryLevelDidChangeNotification object:nil];
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(batteryStatusChanged:) name:UIDeviceBatteryStateDidChangeNotification object:nil];
//        UIDeviceBatteryState bs = [[UIDevice currentDevice] batteryState];
//        [self checkBattery:bs];
    }
    
}

- (void) checkBattery:(UIDeviceBatteryState)bs{
    NSLog(@"轮询获取电池状态 %@", @(bs));
    switch (bs) {
        case UIDeviceBatteryStateUnknown:   // 状态未知
            [[PluginCore shareInstance] nativeEventHandler:@"PluginOS" andEvent:@"power_charge" andParams:@"-1"];
            break;
        case UIDeviceBatteryStateUnplugged:   // 电池未充电
            [[PluginCore shareInstance] nativeEventHandler:@"PluginOS" andEvent:@"power_charge" andParams:@"0"];
            break;
        case UIDeviceBatteryStateCharging:   // 电池正在充电中
            [[PluginCore shareInstance] nativeEventHandler:@"PluginOS" andEvent:@"power_charge" andParams:@"1"];
            break;
        case UIDeviceBatteryStateFull:   // 电池电充满了
            [[PluginCore shareInstance] nativeEventHandler:@"PluginOS" andEvent:@"power_charge" andParams:@"2"];
            break;
            
        default:
            break;
    }
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIDeviceBatteryLevelDidChangeNotification object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIDeviceBatteryStateDidChangeNotification object:nil];
    NSLog(@"手机电池状态变化: %@", @(bs));
}

- (void) removeAllObserver{
    // 移除设备翻转检测
    [[NSNotificationCenter defaultCenter] removeObserver:self
                                                        name:UIDeviceOrientationDidChangeNotification
                                                      object:nil];
        [[UIDevice currentDevice] endGeneratingDeviceOrientationNotifications];
    // 移除声音变化检测
    [[NSNotificationCenter defaultCenter] removeObserver:self
                                                name:@"AVSystemController_SystemVolumeDidChangeNotification"
                                               object:nil];
    [[UIApplication sharedApplication] endReceivingRemoteControlEvents];
    // 移除屏幕亮度变化检测
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIScreenBrightnessDidChangeNotification object:nil];
    
    // 移除监听手机翻转
    [[UIDevice currentDevice] endGeneratingDeviceOrientationNotifications];
        [[NSNotificationCenter defaultCenter] removeObserver:self
                                                     name:UIDeviceOrientationDidChangeNotification
                                                   object:nil];
}
@end
