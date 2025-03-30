<template>
  <div :class="prefixCls">
    <template v-if="tooltip && fullLength > length">
      <a-tooltip>
        <template #title>{{ fullStr }}</template>
        <span>{{ displayStr }}</span>
      </a-tooltip>
    </template>
    <span v-else>{{ displayStr }}</span>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Tooltip as ATooltip } from 'ant-design-vue'

const props = defineProps({
  prefixCls: {
    type: String,
    default: 'ant-pro-ellipsis'
  },
  tooltip: {
    type: Boolean,
    default: false
  },
  length: {
    type: Number,
    required: true
  },
  lines: {
    type: Number,
    default: 1
  },
  fullWidthRecognition: {
    type: Boolean,
    default: false
  }
})

const slots = defineSlots()
const fullStr = computed(() => slots.default?.()?.[0]?.children || '')
const fullLength = computed(() => getStrFullLength(fullStr.value))
const displayStr = computed(() => {
  if (fullLength.value <= props.length) return fullStr.value
  return cutStrByFullLength(fullStr.value, props.length) + '...'
})

const getStrFullLength = (str = '') =>
  str.split('').reduce((pre, cur) => {
    const charCode = cur.charCodeAt(0)
    if (charCode >= 0 && charCode <= 128) {
      return pre + 1
    }
    return pre + 2
  }, 0)

const cutStrByFullLength = (str = '', maxLength) => {
  let showLength = 0
  return str.split('').reduce((pre, cur) => {
    const charCode = cur.charCodeAt(0)
    if (charCode >= 0 && charCode <= 128) {
      showLength += 1
    } else {
      showLength += 2
    }
    if (showLength <= maxLength) {
      return pre + cur
    }
    return pre
  }, '')
}
</script>