import type { BuiltinFunction } from '@/types'

export const BUILTIN_FUNCTIONS: BuiltinFunction[] = [
  // ============ 字符串函数 ============
  {
    name: 'toUpperCase',
    displayName: '转大写',
    description: '将字符串转换为大写',
    category: '字符串',
    params: [
      {
        name: 'str',
        type: 'string',
        required: true,
        description: '输入字符串'
      }
    ],
    returnType: 'string'
  },
  {
    name: 'toLowerCase',
    displayName: '转小写',
    description: '将字符串转换为小写',
    category: '字符串',
    params: [
      {
        name: 'str',
        type: 'string',
        required: true,
        description: '输入字符串'
      }
    ],
    returnType: 'string'
  },
  {
    name: 'trim',
    displayName: '去除空格',
    description: '去除字符串首尾空格',
    category: '字符串',
    params: [
      {
        name: 'str',
        type: 'string',
        required: true,
        description: '输入字符串'
      }
    ],
    returnType: 'string'
  },
  {
    name: 'substring',
    displayName: '截取子串',
    description: '截取字符串的一部分',
    category: '字符串',
    params: [
      {
        name: 'str',
        type: 'string',
        required: true,
        description: '输入字符串'
      },
      {
        name: 'start',
        type: 'number',
        required: true,
        description: '起始位置'
      },
      {
        name: 'end',
        type: 'number',
        required: false,
        description: '结束位置'
      }
    ],
    returnType: 'string'
  },
  {
    name: 'replace',
    displayName: '替换',
    description: '替换字符串中的指定内容',
    category: '字符串',
    params: [
      {
        name: 'str',
        type: 'string',
        required: true,
        description: '输入字符串'
      },
      {
        name: 'search',
        type: 'string',
        required: true,
        description: '要替换的内容'
      },
      {
        name: 'replacement',
        type: 'string',
        required: true,
        description: '替换后的内容'
      }
    ],
    returnType: 'string'
  },
  {
    name: 'split',
    displayName: '分割',
    description: '按分隔符分割字符串为数组',
    category: '字符串',
    params: [
      {
        name: 'str',
        type: 'string',
        required: true,
        description: '输入字符串'
      },
      {
        name: 'separator',
        type: 'string',
        required: true,
        description: '分隔符'
      }
    ],
    returnType: 'array'
  },
  {
    name: 'join',
    displayName: '合并',
    description: '将数组用分隔符合并为字符串',
    category: '字符串',
    params: [
      {
        name: 'arr',
        type: 'array',
        required: true,
        description: '输入数组'
      },
      {
        name: 'separator',
        type: 'string',
        required: false,
        description: '分隔符，默认空字符串'
      }
    ],
    returnType: 'string'
  },
  {
    name: 'length',
    displayName: '长度',
    description: '获取字符串或数组长度',
    category: '字符串',
    params: [
      {
        name: 'value',
        type: 'any',
        required: true,
        description: '字符串或数组'
      }
    ],
    returnType: 'number'
  },

  // ============ 数学函数 ============
  {
    name: 'round',
    displayName: '四舍五入',
    description: '对数字进行四舍五入',
    category: '数学',
    params: [
      {
        name: 'num',
        type: 'number',
        required: true,
        description: '输入数字'
      },
      {
        name: 'precision',
        type: 'number',
        required: false,
        description: '保留小数位数，默认0'
      }
    ],
    returnType: 'number'
  },
  {
    name: 'floor',
    displayName: '向下取整',
    description: '向下取整',
    category: '数学',
    params: [
      {
        name: 'num',
        type: 'number',
        required: true,
        description: '输入数字'
      }
    ],
    returnType: 'number'
  },
  {
    name: 'ceil',
    displayName: '向上取整',
    description: '向上取整',
    category: '数学',
    params: [
      {
        name: 'num',
        type: 'number',
        required: true,
        description: '输入数字'
      }
    ],
    returnType: 'number'
  },
  {
    name: 'abs',
    displayName: '绝对值',
    description: '获取数字的绝对值',
    category: '数学',
    params: [
      {
        name: 'num',
        type: 'number',
        required: true,
        description: '输入数字'
      }
    ],
    returnType: 'number'
  },
  {
    name: 'add',
    displayName: '加法',
    description: '两个数字相加',
    category: '数学',
    params: [
      {
        name: 'a',
        type: 'number',
        required: true,
        description: '第一个数字'
      },
      {
        name: 'b',
        type: 'number',
        required: true,
        description: '第二个数字'
      }
    ],
    returnType: 'number'
  },
  {
    name: 'subtract',
    displayName: '减法',
    description: '两个数字相减',
    category: '数学',
    params: [
      {
        name: 'a',
        type: 'number',
        required: true,
        description: '被减数'
      },
      {
        name: 'b',
        type: 'number',
        required: true,
        description: '减数'
      }
    ],
    returnType: 'number'
  },
  {
    name: 'multiply',
    displayName: '乘法',
    description: '两个数字相乘',
    category: '数学',
    params: [
      {
        name: 'a',
        type: 'number',
        required: true,
        description: '第一个数字'
      },
      {
        name: 'b',
        type: 'number',
        required: true,
        description: '第二个数字'
      }
    ],
    returnType: 'number'
  },
  {
    name: 'divide',
    displayName: '除法',
    description: '两个数字相除',
    category: '数学',
    params: [
      {
        name: 'a',
        type: 'number',
        required: true,
        description: '被除数'
      },
      {
        name: 'b',
        type: 'number',
        required: true,
        description: '除数'
      }
    ],
    returnType: 'number'
  },

  // ============ 日期函数 ============
  {
    name: 'formatDate',
    displayName: '格式化日期',
    description: '格式化日期字符串',
    category: '日期',
    params: [
      {
        name: 'date',
        type: 'string',
        required: true,
        description: '日期字符串'
      },
      {
        name: 'format',
        type: 'string',
        required: false,
        description: '格式模式，默认 YYYY-MM-DD'
      }
    ],
    returnType: 'string'
  },
  {
    name: 'parseDate',
    displayName: '解析日期',
    description: '解析日期字符串',
    category: '日期',
    params: [
      {
        name: 'str',
        type: 'string',
        required: true,
        description: '日期字符串'
      }
    ],
    returnType: 'object'
  },
  {
    name: 'dateAdd',
    displayName: '日期加减',
    description: '日期加减指定天数',
    category: '日期',
    params: [
      {
        name: 'date',
        type: 'string',
        required: true,
        description: '日期字符串'
      },
      {
        name: 'days',
        type: 'number',
        required: true,
        description: '要加减的天数，正数为加，负数为减'
      }
    ],
    returnType: 'string'
  },
  {
    name: 'dateDiff',
    displayName: '日期差',
    description: '计算两个日期之间的天数差',
    category: '日期',
    params: [
      {
        name: 'date1',
        type: 'string',
        required: true,
        description: '第一个日期'
      },
      {
        name: 'date2',
        type: 'string',
        required: true,
        description: '第二个日期'
      }
    ],
    returnType: 'number'
  },
  {
    name: 'now',
    displayName: '当前时间',
    description: '获取当前时间',
    category: '日期',
    params: [],
    returnType: 'string'
  },

  // ============ JSON 函数 ============
  {
    name: 'jsonParse',
    displayName: '解析JSON',
    description: '将 JSON 字符串解析为对象',
    category: 'JSON',
    params: [
      {
        name: 'str',
        type: 'string',
        required: true,
        description: 'JSON 字符串'
      }
    ],
    returnType: 'object'
  },
  {
    name: 'jsonStringify',
    displayName: '转JSON字符串',
    description: '将对象转换为 JSON 字符串',
    category: 'JSON',
    params: [
      {
        name: 'obj',
        type: 'object',
        required: true,
        description: '对象'
      }
    ],
    returnType: 'string'
  },
  {
    name: 'getProperty',
    displayName: '获取属性',
    description: '从对象中获取指定属性',
    category: 'JSON',
    params: [
      {
        name: 'obj',
        type: 'object',
        required: true,
        description: '对象'
      },
      {
        name: 'key',
        type: 'string',
        required: true,
        description: '属性名'
      }
    ],
    returnType: 'any'
  },
  {
    name: 'hasProperty',
    displayName: '检查属性',
    description: '检查对象是否包含指定属性',
    category: 'JSON',
    params: [
      {
        name: 'obj',
        type: 'object',
        required: true,
        description: '对象'
      },
      {
        name: 'key',
        type: 'string',
        required: true,
        description: '属性名'
      }
    ],
    returnType: 'boolean'
  },

  // ============ 数组函数 ============
  {
    name: 'first',
    displayName: '第一个元素',
    description: '获取数组第一个元素',
    category: '数组',
    params: [
      {
        name: 'arr',
        type: 'array',
        required: true,
        description: '数组'
      }
    ],
    returnType: 'any'
  },
  {
    name: 'last',
    displayName: '最后一个元素',
    description: '获取数组最后一个元素',
    category: '数组',
    params: [
      {
        name: 'arr',
        type: 'array',
        required: true,
        description: '数组'
      }
    ],
    returnType: 'any'
  },
  {
    name: 'map',
    displayName: '映射',
    description: '对数组每个元素执行函数并返回新数组',
    category: '数组',
    params: [
      {
        name: 'arr',
        type: 'array',
        required: true,
        description: '数组'
      },
      {
        name: 'callback',
        type: 'function',
        required: true,
        description: '回调函数'
      }
    ],
    returnType: 'array'
  },
  {
    name: 'filter',
    displayName: '过滤',
    description: '过滤数组元素',
    category: '数组',
    params: [
      {
        name: 'arr',
        type: 'array',
        required: true,
        description: '数组'
      },
      {
        name: 'callback',
        type: 'function',
        required: true,
        description: '回调函数，返回 true 保留元素'
      }
    ],
    returnType: 'array'
  },

  // ============ 类型转换函数 ============
  {
    name: 'toString',
    displayName: '转字符串',
    description: '将值转换为字符串',
    category: '类型转换',
    params: [
      {
        name: 'value',
        type: 'any',
        required: true,
        description: '要转换的值'
      }
    ],
    returnType: 'string'
  },
  {
    name: 'toNumber',
    displayName: '转数字',
    description: '将值转换为数字',
    category: '类型转换',
    params: [
      {
        name: 'value',
        type: 'any',
        required: true,
        description: '要转换的值'
      }
    ],
    returnType: 'number'
  },
  {
    name: 'toBoolean',
    displayName: '转布尔值',
    description: '将值转换为布尔值',
    category: '类型转换',
    params: [
      {
        name: 'value',
        type: 'any',
        required: true,
        description: '要转换的值'
      }
    ],
    returnType: 'boolean'
  },
  {
    name: 'toArray',
    displayName: '转数组',
    description: '将值转换为数组',
    category: '类型转换',
    params: [
      {
        name: 'value',
        type: 'any',
        required: true,
        description: '要转换的值'
      }
    ],
    returnType: 'array'
  },

  // ============ 其他函数 ============
  {
    name: 'defaultValue',
    displayName: '默认值',
    description: '如果值为空则返回默认值',
    category: '其他',
    params: [
      {
        name: 'value',
        type: 'any',
        required: true,
        description: '要检查的值'
      },
      {
        name: 'default',
        type: 'any',
        required: true,
        description: '默认值'
      }
    ],
    returnType: 'any'
  },
  {
    name: 'coalesce',
    displayName: '空值合并',
    description: '返回第一个非空值',
    category: '其他',
    params: [
      {
        name: 'values',
        type: 'array',
        required: true,
        description: '值数组'
      }
    ],
    returnType: 'any'
  },
  {
    name: 'uuid',
    displayName: '生成UUID',
    description: '生成唯一标识符',
    category: '其他',
    params: [],
    returnType: 'string'
  },
  {
    name: 'md5',
    displayName: 'MD5哈希',
    description: '计算字符串的 MD5 哈希值',
    category: '其他',
    params: [
      {
        name: 'str',
        type: 'string',
        required: true,
        description: '输入字符串'
      }
    ],
    returnType: 'string'
  }
]