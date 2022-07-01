# 流量日志服务器
OneMirror 流量日志服务器

## API
### POST 方式提交日志
`POST` `/`
- Headers \
`Authorization`: 使用`Basic`的登录，用户名为`log name`，密码为`Secret`
- Content \
要追加到日志到内容

### GET 方式提交日志 
`GET` `/`
- Headers \
`Authorization`: 使用`Basic`的登录，用户名为`log name`，密码为`Secret`
- Params \
`message` 要追加到日志到内容

### GET Websocket 方式提交日志
`GET` `/`
- Headers \
`Authorization`: 使用`Basic`的登录，用户名为`log name`，密码为`Secret`

服务端在连接完成时发送 text frame(`Hello, OneMirror traffic logger server.`)

发送 text frame 或 UTF-8 编码的 binary frame 记录日志