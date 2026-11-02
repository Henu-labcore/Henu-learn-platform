<template>
  <div class="mastery-heatmap">
    <div
      v-if="hasData"
      ref="chartRef"
      class="heatmap-canvas"
      :style="{ height: `${height}px` }"
    />

    <div
      v-else
      class="heatmap-empty"
      :style="{ height: `${height}px` }"
    >
      <el-icon class="empty-icon">
        <DataAnalysis />
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

import { DataAnalysis } from '@element-plus/icons-vue'

const props = defineProps({
  xAxis: {
    type: Array,
    default: () => []
  },

  yAxis: {
    type: Array,
    default: () => []
  },

  // 每项：{ x, y, value, riskLevel }
  // x/y 用轴上的名称（日期字符串、知识点名、学生名）
  cells: {
    type: Array,
    default: () => []
  },

  height: {
    type: Number,
    default: 360
  },

  emptyText: {
    type: String,
    default: '暂无历史数据，完成一次掌握度重算后即可看到'
  }
})

const chartRef = ref(null)

let chart = null

const RISK_TEXT = {
  HIGH: '高风险',
  MEDIUM: '中风险',
  LOW: '低风险',
  UNKNOWN: '未学'
}

const hasData = computed(() => {
  return props.xAxis.length > 0
    && props.yAxis.length > 0
    && props.cells.length > 0
})

const formatScore = value => {
  if (
    value === null
    || value === undefined
    || value === ''
    || Number.isNaN(Number(value))
  ) {
    return ''
  }

  return Number(value).toFixed(1)
}

const buildSeriesData = () => {
  const xIndex = new Map(
    props.xAxis.map((item, index) => [item, index])
  )

  const yIndex = new Map(
    props.yAxis.map((item, index) => [item, index])
  )

  return props.cells
    .map(cell => {
      const x = xIndex.get(cell.x)

      const y = yIndex.get(cell.y)

      if (x === undefined || y === undefined) {
        return null
      }

      const risk = String(
        cell.riskLevel || 'UNKNOWN'
      ).toUpperCase()

      // 未学 / 无记录用灰色 "-"，避免被 0 分误读为"学得极差"
      const missing =
        risk === 'UNKNOWN'
        || cell.value === null
        || cell.value === undefined

      const value = missing
        ? '-'
        : Number(cell.value)

      return {
        value: [x, y, value],
        riskLevel: risk,

        itemStyle: missing
          ? {
            color: '#e2e8f0'
          }
          : undefined
      }
    })
    .filter(Boolean)
}

const buildOption = () => {
  const seriesData = buildSeriesData()

  return {
    tooltip: {
      position: 'top',

      formatter(params) {
        const row = params.data

        if (!row) {
          return ''
        }

        const xName = props.xAxis[row.value[0]]

        const yName = props.yAxis[row.value[1]]

        const risk = RISK_TEXT[row.riskLevel]
          || row.riskLevel

        const score = row.value[2] === '-'
          ? '无记录（未学）'
          : `${formatScore(row.value[2])} 分`

        return [
          `<strong>${yName}</strong> × ${xName}`,
          `掌握度：${score}`,
          `风险等级：${risk}`
        ].join('<br/>')
      }
    },

    grid: {
      top: 16,
      right: 24,
      bottom: props.xAxis.length > 12 ? 72 : 56,
      left: 132,
      containLabel: false
    },

    xAxis: {
      type: 'category',
      data: props.xAxis,

      splitArea: {
        show: true
      },

      axisLabel: {
        interval: props.xAxis.length > 20
          ? 'auto'
          : 0,

        rotate: props.xAxis.length > 10
          ? 30
          : 0,

        fontSize: 11
      },

      axisTick: {
        show: false
      }
    },

    yAxis: {
      type: 'category',
      data: props.yAxis,

      splitArea: {
        show: true
      },

      axisLabel: {
        fontSize: 12,

        width: 120,

        overflow: 'truncate'
      },

      axisTick: {
        show: false
      }
    },

    visualMap: {
      min: 0,
      max: 100,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: 0,
      itemHeight: 120,
      text: ['高', '低'],
      textStyle: {
        fontSize: 11
      },

      inRange: {
        color: [
          '#ef4444',
          '#f97316',
          '#facc15',
          '#84cc16',
          '#16a34a'
        ]
      }
    },

    series: [
      {
        name: '掌握度',
        type: 'heatmap',
        data: seriesData,

        label: {
          show: true,
          fontSize: 10,

          formatter(params) {
            return params.value[2] === '-'
              ? ''
              : formatScore(params.value[2])
          }
        },

        itemStyle: {
          borderColor: '#ffffff',
          borderWidth: 1,
          borderRadius: 2
        },

        emphasis: {
          itemStyle: {
            shadowBlur: 8,
            shadowColor: 'rgba(15, 23, 42, 0.35)'
          }
        }
      }
    ]
  }
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
  () => [props.xAxis, props.yAxis, props.cells, props.height],
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
.mastery-heatmap {
  width: 100%;
}

.heatmap-canvas {
  width: 100%;
}

.heatmap-empty {
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

.heatmap-empty p {
  margin: 0;
}
</style>
