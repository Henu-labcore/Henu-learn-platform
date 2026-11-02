<template>
  <div class="profile-trend">
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
  // [{ key, label, points: [{ date, score }] }]
  series: {
    type: Array,
    default: () => []
  },

  // ['2026-10-01', ...]
  dates: {
    type: Array,
    default: () => []
  },

  height: {
    type: Number,
    default: 300
  },

  emptyText: {
    type: String,
    default: '暂无画像历史，完成一次重算后会按天累积'
  }
})

const chartRef = ref(null)

let chart = null

const COLORS = {
  knowledge: '#2563eb',
  assessment: '#16a34a',
  engagement: '#f59e0b',
  stability: '#8b5cf6',
  execution: '#0ea5e9'
}

const hasData = computed(() => {
  return (props.series || []).some(item => {
    return (item.points || []).some(point => {
      return point.score !== null && point.score !== undefined
    })
  })
})

const formatDay = date => String(date || '').slice(5)

const buildOption = () => {
  const dates = props.dates

  const series = (props.series || []).map(item => {
    const lookup = new Map(
      (item.points || []).map(point => [point.date, point.score])
    )

    return {
      name: item.label,

      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      connectNulls: true,

      data: dates.map(date => {
        const value = lookup.get(date)

        return value === null || value === undefined
          ? null
          : Number(value)
      }),

      lineStyle: {
        width: 2,
        color: COLORS[item.key] || '#64748b'
      },

      itemStyle: {
        color: COLORS[item.key] || '#64748b'
      }
    }
  })

  return {
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
      top: 42,
      right: 24,
      bottom: 28,
      left: 52
    },

    xAxis: {
      type: 'category',
      data: dates,
      boundaryGap: false,

      axisLabel: {
        formatter: formatDay,
        fontSize: 11
      },

      axisTick: {
        show: false
      }
    },

    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      name: '维度分',
      nameTextStyle: {
        fontSize: 11,
        color: '#64748b'
      },

      axisLabel: {
        fontSize: 11
      },

      splitLine: {
        lineStyle: {
          color: '#e2e8f0'
        }
      }
    },

    series
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
  () => [props.series, props.dates, props.height],
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
.profile-trend {
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
