package com.niki914.zafiro.api.telemetry

/**
 * 业务上报的唯一入口。
 *
 * 函数参数使用契约里的 [EventDef] 与 [CustomKeyDef],调用方不判断实现是否安装。
 * 每个函数内部用 `findService<Telemetry>()` 取实现,未安装时丢弃这次调用。
 * 本期没有事件,所以没有函数。
 */
object Metrics
