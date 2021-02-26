/*
 * @CreateTime: Aug 6, 2019 11:42 AM 
 * @Author: undefined 
 * @Contact: undefined 
* @Last Modified By: howe
* @Last Modified Time: Jun 5, 2020 5:52 PM
 * @Description: Modify Here, Please  
 * 插件对象
 */

import { PluginCallbackFunction, PluginEntityData, PluginEventFunction } from "./ezdefine";
import { PluginBridgeCore } from "./PluginBridgeCore";

export class PluginEntity {

    private _pluginCore: PluginBridgeCore = null;
    private _data: PluginEntityData = null;
    public get data(): PluginEntityData {
        return this._data;
    }

    constructor(pluginData: PluginEntityData, pluginCore: PluginBridgeCore) {
        this._data = pluginData;
        this._pluginCore = pluginCore;
    }

    /**
     * 调用sdk插件方法，同步调用
     * @param action 插件动作名
     * @param params 参数
     * @param callback 回调函数 （optional可选）
     * @return {number}  回调id 返回-1表示该方法无法执行
     */
    excute(action: string, params: string | object, callback?: PluginCallbackFunction): number {
        return this._pluginCore.excute(this._data.pluginName, action, params, false, callback)
    }

    /**
    * UI线程中调用插件方法(一般使用在Android系统)
    * @param {string}   action     插件动作名
    * @param {string}   params     参数    （optional可选）
    * @param {function} callback   回调函数 （optional可选）
    * @return {number}  回调id 返回-1表示该方法无法执行
    */
    excuteInUIThread(action: string, params: string, callback?: PluginCallbackFunction): number {
        return this._pluginCore.excute(this._data.pluginName, action, params, true, callback)
    }

    /**
    * 注册插件事件
    * @param {function} callback 事件回调方法
    * @return {number} 事件id
    */
    addEventListener(callback: PluginEventFunction): number {
        return this._pluginCore.addEventListener(this.data.pluginName, callback);
    }

    /**
     * 删除插件事件
     * @param {string|number}   eventIdOrPluginName 插件名称或事件ID
     */
    removeEventListener(eventIdOrPluginName?: string | number): void {
        if (!eventIdOrPluginName || typeof eventIdOrPluginName === "string") {
            this._pluginCore.removeEventListener(this.data.pluginName);
            return;
        }
        if (typeof eventIdOrPluginName === 'number') {
            this._pluginCore.removeEventListener(eventIdOrPluginName);
        }
    }
}
