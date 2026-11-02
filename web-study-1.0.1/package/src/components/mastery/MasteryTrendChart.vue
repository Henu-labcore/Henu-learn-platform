<template>
  <div class="mastery-trend">
    <div
      v-if="hasData"
      ref="chartRef"
      class="trend-canvas"
      :style="{ height: `${height}px` }"
    />

    <div
      v-else
      class="trend-empty"
      :style="{ height: `${height}px` }"
    >
      <el-icon class="empty-icon">
        <TrendCharts />
      </el-icon>

      <p>{{ emptyText }}</p>
    </div>
  </div>
</template>

<script setup>
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch
} from 'vue'

import * as echarts from 'echarts'

import { TrendCharts } from '@element-plus/icons-vue'

const props = defineProps({
  // [{ date, averageMasteryScore, highRiskCount, ... }]
  points: {
    type: Array,
    default: () => []
  },

  // 可选：多学生对比 [{ displayName, points: [{date, averageMasteryScore}] }]
  series: {
    type: Array,
    default: () => []
  },

  height: {
    type: Number,
    default: 320
  },

  showRiskBars: {
    type: Boolean,
    default: true
  },

  // >0 时启用底部拖动条，每次最多显示 windowSize 个数据点
  windowSize: {
    type: Number,
    default: 0
  },

  emptyText: {
    type: String,
    default: '暂无趋势数据，完成掌握度重算后会按天累积'
  }
})

const chartRef = ref(null)

let chart = null

const hasData = computed(() => {
  return props.points.some(
    point =>
      point.averageMasteryScore !== null
      && point.averageMasteryScore !== undefined
  )
})

const visibleWindow = computed(() => {
  const total = props.points.length
  const size = Number(props.windowSize || 0)

  if (size <= 0 || total <= size) {
    return 0
  }

  return size
})

const formatDay = date => String(date || '').slice(5)

const toNumberOrNull = value => {
  if (
    value === null
    || value === undefined
    || value === ''
  ) {
    return null
  }

  return Number(value)
}

const buildOption = () => {
  const dates = props.points.map(point => point.date)

  const averageSeries = {
    name: '平均掌握度',
    type: 'line',
    smooth: true,
    symbol: 'circle',
    symbolSize: 7,
    connectNulls: true,
    yAxisIndex: 0,

    data: props.points.map(
      point => toNumberOrNull(
        point.averageMasteryScore
      )
    ),

    lineStyle: {
      width: 3,
      color: '#2563eb'
    },

    itemStyle: {
      color: '#2563eb'
    },

    areaStyle: {
      color: new echarts.graphic.LinearGradient(
        0,
        0,
        0,
        1,
        [
          {
            offset: 0,
            color: 'rgba(37, 99, 235, 0.18)'
          },
          {
            offset: 1,
            color: 'rgba(37, 99, 235, 0.01)'
          }
        ]
      )
    },

    z: 3
  }

  const series = [averageSeries]

  if (props.showRiskBars) {
    series.push({
      name: '高风险数',
      type: 'bar',
      yAxisIndex: 1,
      barMaxWidth: 14,
      connectNulls: true,

      data: props.points.map(
        point => Number(point.highRiskCount || 0)
      ),

      itemStyle: {
        color: 'rgba(239, 68, 68, 0.35)',
        borderRadius: [4, 4, 0, 0]
      },

      z: 1
    })
  }

  const colors = [
    '#0ea5e9',
    '#f59e0b',
    '#8b5cf6',
    '#10b981',
    '#ec4899',
    '#14b8a6'
  ]

  props.series.forEach((student, index) => {
    const lookup = new Map(
      (student.points || []).map(
        point => [point.date, point.averageMasteryScore]
      )
    )

    series.push({
      name: student.displayName
        || student.name
        || `学生 ${index + 1}`,

      type: 'line',
      smooth: true,
      symbol: 'none',
      connectNulls: true,
      yAxisIndex: 0,

      data: dates.map(date => {
        const value = lookup.get(date)

        return toNumberOrNull(value)
      }),

      lineStyle: {
        width: 1.6,
        type: 'dashed',
        color: colors[index % colors.length]
      },

      itemStyle: {
        color: colors[index % colors.length]
      },

      z: 2
    })
  })

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'line'
      }
    },

    legend: {
      top: 0,
      type: 'scroll',
      itemWidth: 14,
      itemHeight: 10
    },

    grid: {
      top: 44,
      right: props.showRiskBars ? 48 : 24,
      bottom: 28,
      left: 56
    },

    xAxis: {
      type: 'category',
      boundaryGap: true,
      data: dates,

      axisLabel: {
        formatter: formatDay,
        fontSize: 11
      },

      axisTick: {
        show: false
      }
    },

    yAxis: [
      {
        type: 'value',
        min: 0,
        max: 100,
        name: '掌握度',
        nameTextStyle: {
          fontSize: 11,
          color: '#64748b'
        },

        axisLabel: {
          formatter: '{value}',
          fontSize: 11
        },

        splitLine: {
          lineStyle: {
            color: '#e2e8f0'
          }
        }
      },
      {
        type: 'value',
        name: '高风险数',
        show: props.showRiskBars,
        minInterval: 1,
        nameTextStyle: {
          fontSize: 11,
          color: '#64748b'
        },

        axisLabel: {
          fontSize: 11
        },

        splitLine: {
          show: false
        }
      }
    ],

    series
  }

  if (visibleWindow.value > 0) {
    const total = dates.length
    const windowCount = visibleWindow.value

    option.grid.bottom = 64

    option.dataZoom = [
      {
        type: 'inside',
        xAxisIndex: 0,
        startValue: total - windowCount,
        endValue: total - 1,
        zoomOnMouseWheel: false,
        moveOnMouseWheel: false
      },
      {
        type: 'slider',
        xAxisIndex: 0,
        height: 16,
        bottom: 6,
        startValue: total - windowCount,
        endValue: total - 1,
        showDetail: false,
        brushSelect: false,
        borderColor: '#e2e8f0',
        fillerColor: 'rgba(37, 99, 235, 0.12)',

        handleStyle: {
          color: '#2563eb'
        },

        dataBackground: {
          lineStyle: {
            color: '#cbd5e1'
          },

          areaStyle: {
            color: '#e2e8f0'
          }
        },

        textStyle: {
          color: '#64748b',
          fontSize: 10
        }
      }
    ]
  }

  return option
}

const render = async () => {
  if (!hasData.value) {
    chart?.dispose()
    chart = null
    return
  }

  await nextTick()

  if (!chartRef.value) {
    return
  }

  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  chart.setOption(buildOption(), true)

  chart.resize()
}

watch(
  () => [
    props.points,
    props.series,
    props.height,
    props.showRiskBars,
    props.windowSize
  ],
  () => {
    render()
  },
  { deep: true }
)

const handleResize = () => {
  chart?.resize()
}

onMounted(() => {
  window.addEventListener('resize', handleResize)

  render()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)

  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.mastery-trend {
  width: 100%;
}

.trend-canvas {
  width: 100%;
}

.trend-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  background: #f8fafc;
  color: #94a3b8;
  font-size: 13px;
}

.empty-icon {
  font-size: 26px;
  color: #cbd5e1;
}

.trend-empty p {
  margin: 0;
}
</style>
