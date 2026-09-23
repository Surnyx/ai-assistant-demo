# iPhone 安装教程

## 安装前确认

- iPhone 已连接网络。
- 必须使用系统自带的 Safari 打开 Demo，微信或其他 App 的内置浏览器不能直接安装到主屏幕。
- 公网地址：`https://ai-assistant-demo-production-fa28.up.railway.app/`

## 添加到主屏幕

1. 在 iPhone 上打开 Safari。
2. 访问 `https://ai-assistant-demo-production-fa28.up.railway.app/`。
3. 等待登录页完整显示。
4. 点击 Safari 的“分享”按钮（方框上方带向上箭头）。部分新版界面需要先点击“页面菜单”，再点“分享”。
5. 在分享菜单中向上滑动，点击“添加到主屏幕”。
6. 如果出现“作为网页 App 打开”或“Open as Web App”开关，请保持开启；名称保留为“AI 助手”，点击右上角“添加”。
7. 返回主屏幕，点击紫色的“AI 助手”图标。

应用会以独立窗口启动，不显示 Safari 地址栏。首次进入或 Session 过期时，使用测试账号登录：

```text
用户名：test
密码：123456
```

## 使用说明

- 点击左上角菜单按钮打开历史会话。
- 点击“新建对话”后即可发送消息。
- iPhone 键盘中回车用于换行，发送消息请点击“发送”按钮。
- 聊天、历史记录和登录都需要网络；离线时只能打开已缓存的静态页面外壳。

## 更新或故障处理

如果主屏幕中的页面没有更新：

1. 完全关闭“AI 助手”。
2. 在 Safari 中重新打开公网地址并刷新一次。
3. 再从主屏幕启动应用。
4. 如果仍是旧版，长按主屏幕图标并删除书签，然后重新执行“添加到主屏幕”。删除图标不会删除服务器中的聊天记录。

如果分享菜单没有“添加到主屏幕”，请确认当前使用的是 Safari；也可以滚动到分享菜单底部，点击“编辑操作”把该选项加入菜单。

苹果官方步骤：[将 Safari 网站变成 iPhone 上的 App](https://support.apple.com/guide/iphone/open-as-web-app-iphea86e5236/ios)。

## 隐私与缓存

Service Worker 只缓存 HTML、CSS、JavaScript 和图标等静态资源。`/api/**` 下的登录、会话和聊天请求始终访问服务器，并带有 `Cache-Control: no-store`，不会进入 PWA 缓存。API Key 仅保存在 Railway 环境变量中，不会发送到浏览器。
