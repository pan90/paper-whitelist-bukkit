## 指令设计

查询自己的白名单信息
`/whitelist`

添加白名单（管理员）
`/whitelist add <游戏名或UUID>`

删除白名单（管理员）
`/whitelist remove <游戏名或UUID>`

查询任意玩家白名单信息（管理员）
`/whitelist get <游戏名或UUID>`

生成白名单验证码
`/whitelist code`

分页查询所有白名单（管理员）
`/whitelist list [页码]`

根据关键词分页查询白名单（管理员）
`/whitelist search <关键词> [页码]`

## Web API 路由设计（for PaperJetty）

### 分页查询

get `/api/whitelist?page=1&size=10`

### 搜索查询

get `/api/whitelist?search=测试&page=1&size=10`

### 添加白名单，提供id，名字（可选），备注（可选）

post `/api/whitelist`

### 删除白名单，delete请求，提供id

delete `/api/whitelist/{id}`

### 查询某个玩家

get `/api/whitelist/{id}`

### 取出验证码

delete `/api/whitelist-code/{code}`