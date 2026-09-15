<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
// @ts-ignore
import enUs from '/generated/en_us.json'

const props = defineProps<{
  ids: string
}>()

const locales = enUs as Record<string, string>

// Map glob of images from docs/generated
// @ts-ignore
const images = import.meta.glob('@generated/*.png', { eager: true, query: '?url', import: 'default' })

function getItemName(id: string): string {
  // Check block translation key, item translation key, then fallback to id
  return locales[`block.searchlight.${id}`]
      || locales[`item.searchlight.${id}`]
      || locales[id]
      || id.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
}

function getIconUrl(id: string): string | undefined {
  return images[`@generated/${id}.png`]
      || images[`/generated/${id}.png`]
      || Object.entries(images).find(([k]) => k.endsWith(`/${id}.png`))?.[1]
}

const blockIds = computed(() =>
    props.ids
        .split(',')
        .map(id => id.trim()
            .replaceAll('"', '')
            .replaceAll("[", '')
            .replaceAll("]", '')
        )
        .filter(Boolean)
)

const activeIndex = ref(0)

let interval: ReturnType<typeof setInterval>

onMounted(() => {
  if (blockIds.value.length > 1) {
    interval = setInterval(() => {
      activeIndex.value =
          (activeIndex.value + 1) % blockIds.value.length
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
      <img
          v-if="getIconUrl(id)"
          :src="getIconUrl(id)"
          :alt="getItemName(id)"
          class="block-icon"
          width="32"
          height="32"
      />
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
  background: var(--vp-c-bg-soft);
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
}

.block-id.active {
  opacity: 1;
  pointer-events: auto;
}

.block-icon {
  width: 32px;
  height: 32px;
  object-fit: contain;
  image-rendering: pixelated;
  image-rendering: crisp-edges;
  flex-shrink: 0;
  margin: 0;
}

/* Tooltip bubble styling */
.block-name {
  display: none;
  position: absolute;
  bottom: calc(-100% - 8px);
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
}

/* Show tooltip when hovering over the active badge */
.block-id.active:hover .block-name {
  display: block;
}
</style>