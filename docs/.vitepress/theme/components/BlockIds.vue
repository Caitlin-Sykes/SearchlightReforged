<script setup lang="ts">
import {computed, onMounted, onUnmounted, ref} from 'vue'
// @ts-ignore
import enUs from '/generated/en_us.json'

const props = withDefaults(defineProps<{
  ids: string | string[]
  forcedIndex?: number
}>(), {
  forcedIndex: undefined
})

const locales = (enUs || {}) as Record<string, string>

// Map glob of images from docs/generated
// @ts-ignore
const images = import.meta.glob('@generated/*.png', {eager: true, query: '?url', import: 'default'})

function getItemName(id: string): string {
  const cleanId = id.replace(/^[a-z0-9_.-]+:/i, '')
  return locales[`block.searchlight.${cleanId}`]
      || locales[`item.searchlight.${cleanId}`]
      || locales[cleanId]
      || locales[`block.searchlight.${id}`]
      || locales[`item.searchlight.${id}`]
      || locales[id]
      || cleanId.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
}

function getIconUrl(id: string): string | undefined {
  const cleanId = id.replace(/^[a-z0-9_.-]+:/i, '')
  return images[`@generated/${cleanId}.png`]
      || images[`/generated/${cleanId}.png`]
      || images[`@generated/${id}.png`]
      || images[`/generated/${id}.png`]
      || Object.entries(images).find(([k]) => k.endsWith(`/${cleanId}.png`))?.[1]
}

const blockIds = computed(() => {
  const raw = Array.isArray(props.ids) ? props.ids.join(',') : String(props.ids || '')
  return raw
      .split(',')
      .map(id => id.trim()
          .replaceAll('"', '')
          .replaceAll("[", '')
          .replaceAll("]", '')
          .replaceAll("@", '')
          .replace(/^[a-z0-9_.-]+:/i, '')
      )
      .filter(Boolean)
})

const internalIndex = ref(0)

const activeIndex = computed(() => {
  if (props.forcedIndex !== undefined) {
    return blockIds.value.length ? (props.forcedIndex % blockIds.value.length) : 0
  }
  return internalIndex.value
})

let interval: ReturnType<typeof setInterval>

onMounted(() => {
  if (props.forcedIndex === undefined && blockIds.value.length > 1) {
    interval = setInterval(() => {
      internalIndex.value = (internalIndex.value + 1) % blockIds.value.length
    }, 1200)
  }
})

onUnmounted(() => {
  if (interval) {
    clearInterval(interval)
  }
})
</script>

<template>
  <span class="block-ids">
    <span
        v-for="(id, index) in blockIds"
        :key="id"
        class="block-id"
        :class="{ active: index === activeIndex }"
        :title="getItemName(id)"
        :data-id="id"
    >
      <!-- Generated image -->
      <img
          v-if="getIconUrl(id)"
          :src="getIconUrl(id)"
          :alt="getItemName(id)"
          class="block-icon"
          width="32"
          height="32"
      />
      <span v-else class="block-fallback-name">{{ getItemName(id) }}</span>
      <span class="block-name">{{ getItemName(id) }}</span>
    </span>
  </span>
</template>

<style scoped>
.block-ids {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  vertical-align: middle;
  margin: 0 0.25rem;
  height: 48px;
  width: 48px;
  border-radius: 8px;
  border: 1px solid var(--vp-c-divider);
  background: radial-gradient(var(--vp-c-gray-3), var(--vp-c-bg-soft));
}

.block-id {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: default;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.4s ease-in-out;
  padding: 2px;
  box-sizing: border-box;
}

.block-id.active {
  opacity: 1;
  pointer-events: auto;
}

.block-icon {
  width: 32px;
  height: 32px;
  object-fit: contain;
  flex-shrink: 0;
  margin: 0;
  image-rendering: pixelated;
}

.block-fallback-name {
  font-family: 'Minecraft', monospace, sans-serif;
  font-size: 10px;
  line-height: 1.1;
  text-align: center;
  word-break: break-word;
  color: #ffffff;
  text-shadow: 1px 1px #3f3f3f;
  user-select: none;
  max-height: 100%;
  overflow: hidden;
}

/* Tooltip bubble styling */
.block-name {
  position: absolute;
  bottom: -75%;
  left: 50%;
  transform: translateX(-50%);

  white-space: nowrap;
  pointer-events: none;
  z-index: 50;

  padding: 0.25rem 0.5rem;
  border-radius: 0.35rem;
  font-size: 0.75rem;
  font-weight: 500;
  line-height: 1.2;

  color: var(--vp-c-text-1);
  background: var(--vp-c-bg-elv);
  border: 1px solid var(--vp-c-divider);
  box-shadow: var(--vp-shadow-3);
  opacity: 0;
  transition: opacity 150ms ease-in-out;
}

/* Show tooltip when hovering over the active badge */
.block-id.active:hover .block-name {
  opacity: 1;
}
</style>