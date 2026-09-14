<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'

const props = defineProps<{
  ids: string
}>()

const blockIds = computed(() =>
    props.ids
        .split(',')
        .map(id => id.trim())
        .filter(Boolean)
)

const activeIndex = ref(0)

let interval: ReturnType<typeof setInterval>

onMounted(() => {
  interval = setInterval(() => {
    activeIndex.value =
        (activeIndex.value + 1) % blockIds.value.length
  }, 1000)
})

onUnmounted(() => {
  clearInterval(interval)
})
</script>

<template>
  <span class="block-ids">
    <span
        v-for="(id, index) in blockIds"
        :key="id"
        class="block-id"
        :class="{ active: index === activeIndex }"
    >
      {{ id }}
    </span>
  </span>
</template>

<style scoped>
.block-ids {
  display: inline-flex;
  gap: 0.4rem;
  align-items: center;
}

.block-id {
  display: none;
  align-items: center;
  justify-content: center;

  padding: 0.35rem 0.6rem;
  border-radius: 0.4rem;

  background: var(--vp-c-bg-soft);
  border: 1px solid var(--vp-c-divider);

  opacity: 0.4;
  transform: scale(0.9);

  transition:
      opacity 0.25s ease,
      transform 0.25s ease,
      border-color 0.25s ease;
}

.block-id.active {
  display: inline-flex;
  opacity: 1;
  transform: scale(1.05);
  border-color: var(--vp-c-brand-1);
}
</style>