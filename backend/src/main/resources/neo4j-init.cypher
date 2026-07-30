// 知联汇学 - Neo4j 示例数据初始化脚本
// 在 Neo4j Browser 中执行

// 创建约束
CREATE CONSTRAINT IF NOT EXISTS FOR (k:Knowledge) REQUIRE k.name IS UNIQUE;

// ===== 高中物理知识点 =====
CREATE (p1:Knowledge {name: '牛顿第二定律', type: '公式', subject: '高中物理', subjectId: 1, difficulty: 3, description: 'F=ma，力等于质量乘以加速度，是经典力学的核心公式', tags: '力学,核心公式', isCross: false, createTime: datetime()})
CREATE (p2:Knowledge {name: '万有引力定律', type: '公式', subject: '高中物理', subjectId: 1, difficulty: 4, description: 'F=GMm/r²，任意两个物体之间都存在引力', tags: '天体物理,力学', isCross: true, createTime: datetime()})
CREATE (p3:Knowledge {name: '动能定理', type: '公式', subject: '高中物理', subjectId: 1, difficulty: 3, description: '合外力做的功等于物体动能的变化量', tags: '力学,能量', isCross: true, createTime: datetime()})
CREATE (p4:Knowledge {name: '热力学第一定律', type: '概念', subject: '高中物理', subjectId: 1, difficulty: 4, description: '能量守恒定律在热现象中的应用：ΔU=Q+W', tags: '热学,能量守恒', isCross: true, createTime: datetime()})
CREATE (p5:Knowledge {name: '欧姆定律', type: '公式', subject: '高中物理', subjectId: 1, difficulty: 2, description: 'U=IR，电压等于电流乘以电阻', tags: '电学,核心公式', isCross: false, createTime: datetime()})
CREATE (p6:Knowledge {name: '流体力学基础', type: '概念', subject: '高中物理', subjectId: 1, difficulty: 3, description: '流体的压强、流速与伯努利方程', tags: '流体力学', isCross: true, createTime: datetime()})

// ===== 高中化学知识点 =====
CREATE (c1:Knowledge {name: '化学反应速率', type: '概念', subject: '高中化学', subjectId: 2, difficulty: 3, description: '化学反应进行的快慢程度，受浓度、温度、催化剂等因素影响', tags: '化学动力学', isCross: true, createTime: datetime()})
CREATE (c2:Knowledge {name: '氧化还原反应', type: '概念', subject: '高中化学', subjectId: 2, difficulty: 3, description: '有电子转移的化学反应，涉及氧化剂和还原剂', tags: '反应类型', isCross: true, createTime: datetime()})
CREATE (c3:Knowledge {name: '酸碱中和反应', type: '概念', subject: '高中化学', subjectId: 2, difficulty: 2, description: '酸和碱反应生成盐和水', tags: '反应类型,pH', isCross: true, createTime: datetime()})
CREATE (c4:Knowledge {name: '催化剂原理', type: '概念', subject: '高中化学', subjectId: 2, difficulty: 3, description: '催化剂降低反应活化能，加快反应速率，自身不参与反应', tags: '催化,动力学', isCross: true, createTime: datetime()})
CREATE (c5:Knowledge {name: '有机化学基础', type: '概念', subject: '高中化学', subjectId: 2, difficulty: 3, description: '含碳化合物的结构、性质和反应', tags: '有机化学', isCross: false, createTime: datetime()})
CREATE (c6:Knowledge {name: '燃烧反应', type: '概念', subject: '高中化学', subjectId: 2, difficulty: 2, description: '物质与氧气发生的剧烈氧化反应，释放大量热能和光能', tags: '燃烧,能量', isCross: true, createTime: datetime()})

// ===== 高中生物知识点 =====
CREATE (b1:Knowledge {name: '酶的特性', type: '概念', subject: '高中生物', subjectId: 3, difficulty: 3, description: '酶是生物催化剂，具有高效性、专一性和多样性', tags: '生物化学,酶', isCross: true, createTime: datetime()})
CREATE (b2:Knowledge {name: '光合作用', type: '概念', subject: '高中生物', subjectId: 3, difficulty: 4, description: '绿色植物利用光能将CO₂和H₂O转化为有机物并释放O₂', tags: '代谢,能量转换', isCross: true, createTime: datetime()})
CREATE (b3:Knowledge {name: '细胞呼吸', type: '概念', subject: '高中生物', subjectId: 3, difficulty: 3, description: '细胞分解有机物释放能量的过程，有氧呼吸和无氧呼吸', tags: '代谢,能量', isCross: true, createTime: datetime()})
CREATE (b4:Knowledge {name: '微生物降解', type: '概念', subject: '高中生物', subjectId: 3, difficulty: 3, description: '微生物分解有机污染物的过程，广泛应用于环境治理', tags: '微生物,环境', isCross: true, createTime: datetime()})
CREATE (b5:Knowledge {name: '生态系统', type: '概念', subject: '高中生物', subjectId: 3, difficulty: 3, description: '生物群落与无机环境构成的统一整体', tags: '生态,环境', isCross: true, createTime: datetime()})

// ===== 航天拓展知识点 =====
CREATE (a1:Knowledge {name: '火箭推进原理', type: '交叉应用案例', subject: '航天拓展', subjectId: 4, difficulty: 4, description: '基于牛顿第三定律，火箭通过高速喷射气体获得反向推力', tags: '火箭,物理力学', isCross: true, createTime: datetime()})
CREATE (a2:Knowledge {name: '航天燃料化学', type: '交叉应用案例', subject: '航天拓展', subjectId: 4, difficulty: 4, description: '液氢液氧、偏二甲肼等航天燃料的化学反应与能量释放', tags: '燃料,化学反应', isCross: true, createTime: datetime()})
CREATE (a3:Knowledge {name: '卫星轨道力学', type: '概念', subject: '航天拓展', subjectId: 4, difficulty: 5, description: '卫星绕地球运动的轨道计算，涉及万有引力与圆周运动', tags: '轨道,天体力学', isCross: true, createTime: datetime()})
CREATE (a4:Knowledge {name: '空间站生命保障', type: '概念', subject: '航天拓展', subjectId: 4, difficulty: 4, description: '空间站中的氧气循环、水循环和废物处理系统', tags: '生命保障,环境控制', isCross: true, createTime: datetime()})

// ===== 环境科普知识点 =====
CREATE (e1:Knowledge {name: '水污染类型', type: '概念', subject: '环境科普', subjectId: 5, difficulty: 2, description: '物理污染、化学污染、生物污染的分类与特征', tags: '水污染,分类', isCross: false, createTime: datetime()})
CREATE (e2:Knowledge {name: '生物净化技术', type: '交叉应用案例', subject: '环境科普', subjectId: 5, difficulty: 3, description: '利用微生物和植物降解水中有机污染物', tags: '净化,生物降解', isCross: true, createTime: datetime()})
CREATE (e3:Knowledge {name: '化学氧化法', type: '交叉应用案例', subject: '环境科普', subjectId: 5, difficulty: 3, description: '利用氧化剂（如臭氧、氯气）分解水中有机物', tags: '净化,化学氧化', isCross: true, createTime: datetime()})
CREATE (e4:Knowledge {name: '物理过滤技术', type: '概念', subject: '环境科普', subjectId: 5, difficulty: 2, description: '通过物理方法（沉淀、过滤、吸附）去除水中悬浮物', tags: '净化,物理方法', isCross: true, createTime: datetime()})

// ===== 创建知识点间的交叉关系 =====
// 航天交叉：火箭-物理
CREATE (a1)-[:CROSS_RELATE {relationType: '基于原理', description: '火箭推进基于牛顿第三定律'}]->(p1)
CREATE (a1)-[:CROSS_RELATE {relationType: '应用', description: '万有引力定律用于计算卫星轨道'}]->(p2)
CREATE (a3)-[:CROSS_RELATE {relationType: '基于原理', description: '卫星轨道计算基于万有引力定律'}]->(p2)

// 航天交叉：燃料-化学
CREATE (a2)-[:CROSS_RELATE {relationType: '化学反应', description: '航天燃料燃烧是氧化还原反应'}]->(c6)
CREATE (a2)-[:CROSS_RELATE {relationType: '能量转换', description: '燃料燃烧释放化学能转化为动能'}]->(c2)
CREATE (a2)-[:CROSS_RELATE {relationType: '推动', description: '燃料燃烧产生推力符合牛顿定律'}]->(a1)

// 航天-生物：生命保障
CREATE (a4)-[:CROSS_RELATE {relationType: '涉及', description: '空间站氧气循环涉及光合作用原理'}]->(b2)

// 生化交叉：酶-催化剂
CREATE (b1)-[:CROSS_RELATE {relationType: '共通原理', description: '酶催化与化学催化剂都降低活化能'}]->(c4)

// 环境交叉：生物降解-化学氧化
CREATE (e2)-[:CROSS_RELATE {relationType: '协同应用', description: '生物降解与化学氧化常联合使用处理污水'}]->(e3)
CREATE (e2)-[:CROSS_RELATE {relationType: '原理', description: '微生物降解依赖酶催化反应'}]->(b1)
CREATE (e3)-[:CROSS_RELATE {relationType: '原理', description: '化学氧化基于氧化还原反应原理'}]->(c2)

// 环境-物理：过滤技术
CREATE (e4)-[:CROSS_RELATE {relationType: '原理', description: '物理过滤涉及流体力学原理'}]->(p6)

// 生物-环境交叉
CREATE (b4)-[:CROSS_RELATE {relationType: '应用', description: '微生物降解用于水污染治理'}]->(e2)
CREATE (b5)-[:CROSS_RELATE {relationType: '关联', description: '生态系统稳定性影响环境治理效果'}]->(e1)

// 物理-化学交叉：能量
CREATE (p4)-[:CROSS_RELATE {relationType: '共通原理', description: '热力学定律适用于化学反应中的能量变化'}]->(c1)
CREATE (p3)-[:CROSS_RELATE {relationType: '共通原理', description: '动能定理与化学反应能量守恒相关'}]->(c6)

// 生物代谢交叉
CREATE (b2)-[:CROSS_RELATE {relationType: '对立过程', description: '光合作用与细胞呼吸是互逆的代谢过程'}]->(b3)
CREATE (b3)-[:CROSS_RELATE {relationType: '类型', description: '细胞呼吸本质是有机物的氧化反应'}]->(c2)
