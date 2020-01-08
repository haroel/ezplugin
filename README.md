# ezplugin
A SDK framework for cocos jsb projects, includes iOS、Android and web。
`ezplugin`是一个适用于cocos creator接入SDK/原生OC、java代码的开发框架。 

基于此框架，在js/ts层面可以更简单、稳定、高效的调用OC/java层的代码方法而无需顾忌线程切换问题和安全问题。

`ezplugin`解决的痛点如下。
1. cocos引擎opengl线程和原生之间可能会存在线程调用问题，特别在Android上，java多线程执行非常普遍，如果在非OpenGL线程回调js，会导致引擎运行异常错误，反之也一样。`ezplugin`在底层已完美抹平这个差异
2. `ezplugin`接入的SDK插件之间完全隔离互不干扰，这意味着基于`ezplugin`的项目完全可以复用OC/Java层代码，几乎不需要做修改就可以共享给其他项目使用。
3. `ezplugin`足够轻量级，在cocos层面，它依赖了
  ```
  jsb.reflection.callStaticMethod
  ccscheduler->performFunctionInCocosThread
  
  org.cocos2dx.lib.Cocos2dxHelper.runOnGLThread
  Cocos2dxJavascriptJavaBridge.evalString
  ````
除此以外，不依赖任何第三方框架库，也不包含任何c/c++代码。OC和Java层面，插件初始化创建时依赖于OC和java的类反射，这个特性是编程语言层面的，你完全不用担心安全问题和审核问题。

4. `ezplugin`的调用习惯更简洁，也更符合node.js的异步回调风格，如下面的IAP支付（摘自本人开发项目）
    ```
          let iapPlugin = ezplugin.get("PluginIAP");
          if (iapPlugin){
              iapPlugin.excute("pay",`${productID}|${billNo}`,(error,result)=>{
                  if (error){
                    cc.log(error)
                  }
              })
          }
    ```
 
 5. `ezplugin`支持native层往cocos引擎js层发送事件。
 ```
        let gpayPlugin = ezplugin.get("PluginGooglePay");
        if (gpayPlugin){
            gpayPlugin.addEventListener( (event:string,params:string)=>{
              
            } );
        }
 ```
