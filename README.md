# DC-distributed-algorithm-competition

##### 拓扑结构

superNode+nomalNode

###### superNode与superNode以及nomalNode连接

*一个superNode和与之连接的nomalNode形成星形结构*

*superNode之间形成网状结构*

- 一个nomalNode只与一个superNode通过normalChannel连接

- superNode之间互相连接

  根据superNode个数N与highChannel的个数M计算如何连接(N/2<=M<=4N)

###### 关于superNode与highChannel

[N个superNode最多建立N(N-1)/2条highChannel],故如果M>N(N-1)/2则会有空余的highChannel

我们该怎么使用多余的highChannel？

- 为防止superNode之间的highChannel的消息数达到maxMessageCount，两个superNode间可以建立大于1条的highChannel
- Let's figure out

##### 路由

nomalNode只知道与之相连的superNode

superNode知道与之相连的nomalNode和superNode和其他superNode连了哪些nomalNode

nomalNode向nomalNode发信息：nomalNode发给superNode X，X通过它自己的路由表查询该怎么发

nomalNode向superNode发信息、superNode向superNode发信息、superNode向nomalNode发信息都是nomalNode向nomalNode的一部分

##### 初始化





