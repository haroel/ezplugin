//
//  InterceptURLProtocol.m
//  shawngames-mobile
//
//  Created by howe on 2019/8/29.
//

#import "InterceptURLProtocol.h"
#import <MobileCoreServices/UTType.h>

static NSString * const URLProtocolHandledKey = @"URLProtocolHandledKey";

static NSString * filterPrefixUrl = @"http://";
static NSString * webviewCachePath = @"";

@interface InterceptURLProtocol ()<NSURLConnectionDelegate>

@property (nonatomic,strong) NSURLSessionDataTask *task;

@end

@implementation InterceptURLProtocol
+(void)initPaths:(NSString*)prefixUrl andCache:(NSString*)cachePath{
    filterPrefixUrl = [NSString stringWithString:prefixUrl];
    [filterPrefixUrl retain];
    webviewCachePath = [NSString stringWithString:cachePath];
    [webviewCachePath retain];
}
//每个请求都会先走这个方法，判断是否要拦截这个请求
//返回YES，则URL Loading System 会创建一个对应NSURLProtocol子类的实例，即拦截请求
//返回NO，则直接跳到下一个Protocol
//此方法可以简单有效地控制拦截哪些请求，比如schme为https或者后缀是png等等
+ (BOOL)canInitWithRequest:(NSURLRequest *)request {
    if ( [request.HTTPMethod isEqualToString:@"POST"]){
        return NO;
    }
    if ( [webviewCachePath length] <1 ){
        return NO;
    }
    NSString *monitorStr = request.URL.absoluteString;
    if ( [ monitorStr hasPrefix:filterPrefixUrl ]){
        NSString *cache_dir_name = [webviewCachePath lastPathComponent];
        if ( ![monitorStr containsString:cache_dir_name]){
            return NO;
        }
        // version.json请求全部放过，不进行处理
        if ([monitorStr containsString:@"version.json"]){
            return NO;
        }
        //防止死循环
        if ([NSURLProtocol propertyForKey:URLProtocolHandledKey inRequest:request]) {
            return NO;
        }
        return YES;
    }
    return NO;
}
#pragma mark -
#pragma mark 处理POST请求相关POST  用HTTPBodyStream来处理BODY体
- (NSMutableURLRequest *)handlePostRequestBodyWithRequest:(NSMutableURLRequest *)request {
    NSMutableURLRequest * req = [request mutableCopy];
    if ([request.HTTPMethod isEqualToString:@"POST"]) {
        if (!request.HTTPBody) {
            uint8_t d[1024] = {0};
            NSInputStream *stream = request.HTTPBodyStream;
            NSMutableData *data = [[NSMutableData alloc] init];
            [stream open];
            while ([stream hasBytesAvailable]) {
                NSInteger len = [stream read:d maxLength:1024];
                if (len > 0 && stream.streamError == nil) {
                    [data appendBytes:(void *)d length:len];
                }
            }
            req.HTTPBody = [data copy];
            [stream close];
        }
    }
    return req;
}
//此方法可以重新改造request，比如重定向或修改UA
//也可以直接返回原始request
+ (NSURLRequest *)canonicalRequestForRequest:(NSURLRequest *)request {
    return request;
}
//开始加载一个请求，要重新构造一个NSURLSession或者NSURLConnection的请求
//这里也可以使用本地缓存的文件直接返回，这样就减少了网络请求量
//这里面的self.request是在创建protocol实例时Url Loading System传入的
- (void)startLoading {
    do {
        //1.获取资源文件路径 ajkyq/index.html
        NSURL *url = [self request].URL;
        NSString *resourcePath = url.path;
        // 获取缓存路径
        NSString *cache_dir_name = [webviewCachePath lastPathComponent];
        NSArray * array = [ resourcePath componentsSeparatedByString:cache_dir_name];
        if ( array.count < 2){
            break;
        }
        // 优先从缓存目录查找，然后去包内搜索
        NSString *cacheFilePath = [webviewCachePath stringByAppendingPathComponent:array[1]];
//        NSLog(@"[InterceptURLProtocol] 缓存 %@",cacheFilePath);
        NSFileManager *fm = [NSFileManager defaultManager];
        BOOL isExist = [fm fileExistsAtPath:cacheFilePath];
        if (!isExist){
            cacheFilePath = [[NSBundle mainBundle] pathForResource:[cache_dir_name stringByAppendingPathComponent:array[1]] ofType:nil];
            isExist = [fm fileExistsAtPath:cacheFilePath];
        }
        if (!isExist){
            break;
        }
//        NSLog(@"[InterceptURLProtocol] 目标 %@",cacheFilePath);

        //2.读取资源文件内容
        NSFileHandle *file = [NSFileHandle fileHandleForReadingAtPath:cacheFilePath];
        NSData *data = [file readDataToEndOfFile];
        [file closeFile];
        
        
        //3.拼接响应Response
        NSInteger dataLength = data.length;
        if (dataLength <= 0){
            break;
        }
        NSString *mimeType = [self getMIMETypeWithCAPIAtFilePath:cacheFilePath];
        NSString *httpVersion = @"HTTP/1.1";
        NSHTTPURLResponse *response = [self jointResponseWithData:data dataLength:dataLength mimeType:mimeType requestUrl:url statusCode:200 httpVersion:httpVersion];
        
        //4.响应
        [[self client] URLProtocol:self didReceiveResponse:response cacheStoragePolicy:NSURLCacheStorageNotAllowed];
        [[self client] URLProtocol:self didLoadData:data];
        [[self client] URLProtocolDidFinishLoading:self];
        return;
    } while (false);
    
    NSLog(@"【InterceptURLProtocol】 拦截失败，使用正常形式的请求 %@",[self request].URL);
    NSURLSession *session = [NSURLSession
                             sessionWithConfiguration:[NSURLSessionConfiguration defaultSessionConfiguration]
                             delegate:self delegateQueue:nil];
    self.task = [session dataTaskWithRequest:self.request];
    [self.task resume];
}

//取消一个请求
- (void)stopLoading {
    if ( self.task != nil){
        [self.task cancel];
        self.task = nil;
    }
}
#pragma mark - 拼接响应Response
-(NSHTTPURLResponse *)jointResponseWithData:(NSData *)data dataLength:(NSInteger)dataLength mimeType:(NSString *)mimeType requestUrl:(NSURL *)requestUrl statusCode:(NSInteger)statusCode httpVersion:(NSString *)httpVersion
{
    NSDictionary *dict = @{@"Content-type":mimeType,
                           @"Content-length":[NSString stringWithFormat:@"%ld",(long)dataLength]};
    NSHTTPURLResponse *response = [[NSHTTPURLResponse alloc] initWithURL:requestUrl statusCode:statusCode HTTPVersion:httpVersion headerFields:dict];
    return response;
}


#pragma mark - 获取mimeType
-(NSString *)getMIMETypeWithCAPIAtFilePath:(NSString *)path
{
    if (![[[NSFileManager alloc] init] fileExistsAtPath:path]) {
        return @"text/html";
    }
    
    CFStringRef UTI = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension, (__bridge CFStringRef)[path pathExtension], NULL);
    CFStringRef MIMEType = UTTypeCopyPreferredTagWithClass (UTI, kUTTagClassMIMEType);
    CFRelease(UTI);
    if (!MIMEType) {
        return @"application/octet-stream";
    }
    return (__bridge NSString *)(MIMEType);
}

//请求回调
//每个NSURLProtocol子类都有一个client对象来处理response，这个client可以理解为就是Url Loading System， 交给他全权负责
- (void)URLSession:(NSURLSession *)session dataTask:(NSURLSessionDataTask *)dataTask didReceiveResponse:(NSURLResponse *)response completionHandler:(void (^)(NSURLSessionResponseDisposition))completionHandler {
    [[self client] URLProtocol:self didReceiveResponse:response cacheStoragePolicy:NSURLCacheStorageAllowed];
    
    completionHandler(NSURLSessionResponseAllow);
}

- (void)URLSession:(NSURLSession *)session dataTask:(NSURLSessionDataTask *)dataTask didReceiveData:(NSData *)data {
    [[self client] URLProtocol:self didLoadData:data];
}

// 要根据error状态调用不同的client方法，否则会出现错误
- (void)URLSession:(NSURLSession *)session task:(NSURLSessionTask *)task didCompleteWithError:(nullable NSError *)error {
    if (error) {
        [self.client URLProtocol:self didFailWithError:error];
    } else {
        [self.client URLProtocolDidFinishLoading:self];
    }
}


#pragma mark - NSURLConnectionDelegate

- (void) connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response {
    [self.client URLProtocol:self didReceiveResponse:response cacheStoragePolicy:NSURLCacheStorageNotAllowed];
}

- (void) connection:(NSURLConnection *)connection didReceiveData:(NSData *)data {
    [self.client URLProtocol:self didLoadData:data];
}

- (void) connectionDidFinishLoading:(NSURLConnection *)connection {
    [self.client URLProtocolDidFinishLoading:self];
}

- (void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error {
    [self.client URLProtocol:self didFailWithError:error];
}

@end
