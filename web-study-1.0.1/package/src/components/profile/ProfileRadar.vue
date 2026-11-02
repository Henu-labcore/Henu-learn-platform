<template>
  <div class="profile-radar">
    <div
      v-if="hasDimensions"
      ref="chartRef"
      class="radar-canvas"
      :style="{ height: `${height}px` }"
    />

    <div
      v-else
      class="radar-empty"
      :style="{ height: `${height}px` }"
    >
      <el-icon class="empty-icon">
        <DataAnalysis />
      </el-icon>

      <p>暂无画像维度数据</p>
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
  // [{ key, label, score, evidence }]
  dimensions: {
    type: Array,
    default: () => []
  },

  height: {
    type: Number,
    default: 320
  }
})

const chartRef = ref(null)

let chart = null

// 无数据的维度不再隐藏，保留轴并标记为灰色
const dimensions = computed(() => props.dimensions || [])

const hasDimensions = computed(() => dimensions.value.length >= 3)

const isMissing = dimension => {
  return dimension.score === null || dimension.score === undefined
}

const buildOption = () => {
  const views = dimensions.value

  const axisValues = views.map(() => 60)

  const excellentValues = views.map(() => 80)

  const profileValues = views.map(dimension => {
    return isMissing(dimension)
      ? 0
      : Number(dimension.score)
  })

  const missingNames = new Set(
    views
      .filter(isMissing)
      .map(dimension => dimension.label)
  )

  return {
    tooltip: {
      trigger: 'item',

      formatter() {
        return views
          .map(dimension => {
            const scoreText = isMissing(dimension)
              ? '<span style="color:#94a3b8">暂无数据</span>'
              : `${dimension.score} 分`

            return [
              `<strong>${dimension.label}</strong>：${scoreText}`,
              `<span style="color:#94a3b8">${dimension.evidence}</span>`
            ].join('<br/>')
          })
          .join('<br/><br/>')
      }
    },

    radar: {
      indicator: views.map(dimension => ({
        name: dimension.label,
        max: 100
      })),

      radius: '80%',
      center: ['50%', '50%'],

      axisName: {
        formatter(name) {
          return missingNames.has(name)
            ? `{missing|${name}}`
            : `{normal|${name}}`
        },

        rich: {
          normal: {
            color: '#475569',
            fontSize: 12
          },

          missing: {
            color: '#cbd5e1',
            fontSize: 12
          }
        }
      },

      splitLine: {
        lineStyle: {
          color: '#e2e8f0'
        }
      },

      splitArea: {
        areaStyle: {
          color: ['#f8fafc', '#ffffff']
        }
      }
    },

    series: [
      {
        name: '参考线',
        type: 'radar',
        silent: true,
        symbol: 'none',
        tooltip: {
          show: false
        },
        lineStyle: {
          color: '#cbd5e1',
          width: 1,
          type: 'dashed'
        },
        data: [
          {
            value: excellentValues
          },
          {
            value: axisValues,
            lineStyle: {
              color: '#f59e0b',
              width: 1.5,
              type: 'dashed'
            }
          }
        ]
      },
      {
        name: '我的画像',
        type: 'radar',
        symbolSize: 6,

        data: [
          {
            value: profileValues,
            name: '我的画像'
          }
        ],

        lineStyle: {
          color: '#2563eb',
          width: 3
        },

        itemStyle: {
          color: '#2563eb'
        },

        areaStyle: {
          color: 'rgba(37, 99, 235, 0.22)'
        }
      }
    ]
  }
}

const render = async () => {
  if (!hasDimensions.value) {
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
  () => [props.dimensions, props.height],
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
.profile-radar {
  width: 100%;
}

.radar-canvas {
  width: 100%;
}

.radar-empty {
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

.radar-empty p {
  margin: 0;
}
</style>
