//
//  OCExport.h
//  SlotsRoyale2017
//
//  Created by jfxu on 17/01/2017.
//
//


@interface OCExport : NSObject
+(NSString*)getUUID;
+(NSString*)getIMSI;
+(NSString*)getIMEI;
+(NSString*)getModel;

+(float)getOSVer;
+(NSString*)getGameVer;
+(NSString*)getPackageName;
+(int)getNetType;
+(NSString*)getMac;
+(NSString*)getMetaByKey:(NSString*)key;
+(void)requestAllPermission;
@end
