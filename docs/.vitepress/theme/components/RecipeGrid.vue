<script setup>
import {computed, onMounted, onUnmounted, ref, useAttrs, useSlots} from 'vue'
import BlockIds from './BlockIds.vue'

// Import generated recipes.json if present
// @ts-ignore
const recipesModules = import.meta.glob('@generated/recipes.json', {eager: true, import: 'default'})
const recipesData = (Object.values(recipesModules)[0] || {})

const props = defineProps({
  id: {
    type: String,
    default: undefined
  },
  recipe: {
    type: [Array, Object, String],
    default: undefined
  },
  slots: {
    type: [Array, Object],
    default: undefined
  },
  items: {
    type: [Array, Object],
    default: undefined
  },
  grid: {
    type: [Array, Object],
    default: undefined
  },
  result: {
    type: [Array, Object, String],
    default: undefined
  },
  resultCount: {
    type: Number,
    default: undefined
  },
  slot1: {type: [Object, String, Number, Array], default: undefined},
  slot2: {type: [Object, String, Number, Array], default: undefined},
  slot3: {type: [Object, String, Number, Array], default: undefined},
  slot4: {type: [Object, String, Number, Array], default: undefined},
  slot5: {type: [Object, String, Number, Array], default: undefined},
  slot6: {type: [Object, String, Number, Array], default: undefined},
  slot7: {type: [Object, String, Number, Array], default: undefined},
  slot8: {type: [Object, String, Number, Array], default: undefined},
  slot9: {type: [Object, String, Number, Array], default: undefined},
  s1: {type: [Object, String, Number, Array], default: undefined},
  s2: {type: [Object, String, Number, Array], default: undefined},
  s3: {type: [Object, String, Number, Array], default: undefined},
  s4: {type: [Object, String, Number, Array], default: undefined},
  s5: {type: [Object, String, Number, Array], default: undefined},
  s6: {type: [Object, String, Number, Array], default: undefined},
  s7: {type: [Object, String, Number, Array], default: undefined},
  s8: {type: [Object, String, Number, Array], default: undefined},
  s9: {type: [Object, String, Number, Array], default: undefined}
})

const vueSlots = useSlots()
const attrs = useAttrs()

// Collect all relevant recipes matching the given key
const recipesList = computed(() => {
  if (props.recipe && typeof props.recipe === 'object' && !Array.isArray(props.recipe)) {
    return [props.recipe]
  }

  const recipeKey = (typeof props.recipe === 'string' ? props.recipe : '') ||
      props.id ||
      (typeof attrs.recipe === 'string' ? attrs.recipe : '') ||
      (typeof attrs.id === 'string' ? attrs.id : '')

  if (!recipeKey || !recipesData) return []

  const cleanKey = recipeKey.replace(/^[a-z0-9_.-]+:/i, '').replace(/[-_]/g, '_')
  const baseKey = cleanKey.replace(/_(white_base|base|color|@color@)$/i, '')

  const matched = []
  const seenIds = new Set()

  // 1. Direct match
  if (recipesData[cleanKey] && !seenIds.has(recipesData[cleanKey].id)) {
    matched.push(recipesData[cleanKey])
    seenIds.add(recipesData[cleanKey].id)
  }

  // 2. Scan all recipes in recipesData matching the family baseKey
  for (const [k, rec] of Object.entries(recipesData)) {
    if (!rec || !rec.id || seenIds.has(rec.id)) continue

    const recId = rec.id.toLowerCase()
    const recGroup = (rec.group || '').toLowerCase()

    if (
        k.startsWith(baseKey) ||
        recId.startsWith(baseKey) ||
        recGroup === baseKey ||
        recId.includes(baseKey)
    ) {
      matched.push(rec)
      seenIds.add(rec.id)
    }
  }

  // Sort so base crafting is typically first, then dyeing / slabs
  matched.sort((a, b) => {
    const aBase = a.id.includes('base') ? 0 : (a.id.includes('slab') ? 2 : 1)
    const bBase = b.id.includes('base') ? 0 : (b.id.includes('slab') ? 2 : 1)
    return aBase - bBase
  })

  return matched
})

const currentRecipeIndex = ref(0)

const currentRecipe = computed(() => {
  if (recipesList.value.length > 0) {
    const idx = currentRecipeIndex.value % recipesList.value.length
    return recipesList.value[idx]
  }
  return null
})

function nextRecipe() {
  if (recipesList.value.length > 1) {
    currentRecipeIndex.value = (currentRecipeIndex.value + 1) % recipesList.value.length
  }
}

function getRecipeLabel(rec, idx) {
  if (!rec || !rec.id) return `Recipe ${idx + 1}`
  const id = rec.id.toLowerCase()
  if (id.includes('white_base') || (id.endsWith('_iron') && !id.includes('color'))) return 'Base Crafting'
  if (id.includes('from_slabs')) return 'From Slabs'
  if (id.includes('copper')) return 'Copper Variant'
  if (id.includes('prismarine')) return 'Prismarine Variant'
  if (id.includes('slab') && id.includes('color')) return 'Slab Dyeing'
  if (id.includes('color')) return 'Dyeing'
  return rec.group || `Recipe ${idx + 1}`
}

// Animation and Freeze State
const activeIndex = ref(0)
const isFrozen = ref(false)

const isAnimated = computed(() => {
  const rec = currentRecipe.value
  if (!rec) return false
  const hasMultipleInSlots = (rec.slots || []).some(s => s && (s.includes(',') || s.startsWith('@[')))
  const hasMultipleInResult = rec.result && (rec.result.length > 1 || (typeof rec.result === 'string' && rec.result.includes(',')))
  return hasMultipleInSlots || hasMultipleInResult
})

let timer = null

function startTimer() {
  stopTimer()
  timer = setInterval(() => {
    if (!isFrozen.value) {
      activeIndex.value = (activeIndex.value + 1) % 16
    }
  }, 1200)
}

function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function toggleFreeze() {
  isFrozen.value = !isFrozen.value
}

function stepColor(delta) {
  activeIndex.value = (activeIndex.value + delta + 16) % 16
}

onMounted(() => {
  startTimer()
})

onUnmounted(() => {
  stopTimer()
})

function getSlotItem(index) {
  // Explicit slot props take highest priority
  const explicitKeys = [
    `slot${index}`,
    `s${index}`,
    `slot_${index}`,
    `item${index}`,
    `slot-${index}`,
    String(index)
  ]

  for (const k of explicitKeys) {
    if (props[k] !== undefined) return props[k]
    if (attrs[k] !== undefined) return attrs[k]
  }

  // Next check slots / items props
  const sources = [props.slots, props.items]
  for (const src of sources) {
    if (src) {
      if (Array.isArray(src)) {
        if (src[index - 1] !== undefined) return src[index - 1]
      } else if (typeof src === 'object') {
        if (src[index] !== undefined) return src[index]
        if (src[String(index)] !== undefined) return src[String(index)]
        if (src[`slot${index}`] !== undefined) return src[`slot${index}`]
        if (src[`s${index}`] !== undefined) return src[`s${index}`]
      }
    }
  }

  // Fallback to current recipe from recipes.json
  const recipeObj = currentRecipe.value
  if (recipeObj && recipeObj.slots && Array.isArray(recipeObj.slots)) {
    return recipeObj.slots[index - 1] || null
  }

  return null
}

const currentResultIds = computed(() => {
  if (props.result) {
    if (Array.isArray(props.result)) return props.result.join(',')
    if (typeof props.result === 'object' && props.result.ids) return String(props.result.ids)
    return String(props.result)
  }
  const rec = currentRecipe.value
  if (rec && rec.result) {
    if (Array.isArray(rec.result)) return rec.result.join(',')
    return String(rec.result)
  }
  return null
})

const currentResultCount = computed(() => {
  if (props.resultCount !== undefined) return props.resultCount
  const rec = currentRecipe.value
  if (rec && rec.resultCount) return rec.resultCount
  return 1
})

function hasCustomSlot(index) {
  return Boolean(
      vueSlots[String(index)] ||
      vueSlots[`slot${index}`] ||
      vueSlots[`slot-${index}`] ||
      vueSlots[`s${index}`]
  )
}

function getCustomSlotName(index) {
  if (vueSlots[String(index)]) return String(index)
  if (vueSlots[`slot${index}`]) return `slot${index}`
  if (vueSlots[`slot-${index}`]) return `slot-${index}`
  if (vueSlots[`s${index}`]) return `s${index}`
  return String(index)
}

function isSlotVisible(index) {
  if (hasCustomSlot(index)) return true
  const item = getSlotItem(index)
  if (item === null || item === undefined || item === false || item === '') return false
  if (Array.isArray(item) && item.length === 0) return false
  if (typeof item === 'object' && Object.keys(item).length === 0) return false
  return true
}

function getBlockIds(item) {
  if (typeof item === 'string') {
    if (!item.match(/\.(png|jpe?g|svg|webp|gif)$/i) && !item.startsWith('/') && !item.startsWith('http')) {
      return item
    }
    return null
  }
  if (Array.isArray(item)) {
    return item.join(',')
  }
  if (typeof item === 'object' && item !== null) {
    if (item.ids) return Array.isArray(item.ids) ? item.ids.join(',') : String(item.ids)
    if (item.id) return Array.isArray(item.id) ? item.id.join(',') : String(item.id)
  }
  return null
}

function getImageSrc(item) {
  if (typeof item === 'string') {
    if (item.match(/\.(png|jpe?g|svg|webp|gif)$/i) || item.startsWith('/') || item.startsWith('http')) {
      return item
    }
  }
  if (typeof item === 'object' && item !== null) {
    return item.src || item.image || item.icon || null
  }
  return null
}

function getItemText(item) {
  if (typeof item === 'number') return String(item)
  if (typeof item === 'object' && item !== null) {
    return item.text || item.label || item.name || item.title || null
  }
  return null
}

function getItemCount(item) {
  if (typeof item === 'object' && item !== null) {
    return item.count || item.amount || item.qty || null
  }
  return null
}
</script>

<template>
  <div class="recipe_wrapper">
    <!-- Toolbar for recipe tabs and freeze controls -->
    <div v-if="recipesList.length > 1 || isAnimated" class="recipe_toolbar">
      <div v-if="recipesList.length > 1" class="recipe_tabs">
        <button
            v-for="(rec, idx) in recipesList"
            :key="rec.id || idx"
            class="recipe_tab_btn"
            :class="{ active: idx === currentRecipeIndex }"
            @click="currentRecipeIndex = idx"
        >
          {{ getRecipeLabel(rec, idx) }}
        </button>
      </div>
      <div v-if="isAnimated" class="recipe_controls">
        <button
            class="recipe_ctrl_btn"
            :class="{ active: isFrozen }"
            :title="isFrozen ? 'Resume recipe cycle' : 'Freeze recipe cycle'"
            @click="toggleFreeze"
        >
          {{ isFrozen ? 'Resume' : 'Freeze' }}
        </button>
        <button
            v-if="isFrozen"
            class="recipe_ctrl_btn step_btn"
            title="Previous variation"
            @click="stepColor(-1)"
        >
          ◀
        </button>
        <button
            v-if="isFrozen"
            class="recipe_ctrl_btn step_btn"
            title="Next variation"
            @click="stepColor(1)"
        >
          ▶
        </button>
      </div>
    </div>

    <!-- Main Crafting GUI Container -->
    <div class="recipe_body">
      <div
          class="recipe_container"
          :title="recipesList.length > 1 ? 'Right-click to swap recipe' : undefined"
          @contextmenu.prevent="nextRecipe"
      >
        <!-- 3x3 Crafting Grid -->
        <div class="recipe_crafting_grid">
          <div
              v-for="i in 9"
              :key="i"
              class="recipe_slot"
          >
            <slot
                v-if="hasCustomSlot(i)"
                :name="getCustomSlotName(i)"
                :item="getSlotItem(i)"
                :index="i"
            />
            <template v-else-if="isSlotVisible(i)">
              <BlockIds
                  v-if="getBlockIds(getSlotItem(i))"
                  :ids="getBlockIds(getSlotItem(i))"
                  :forced-index="activeIndex"
              />
              <img
                  v-else-if="getImageSrc(getSlotItem(i))"
                  :src="getImageSrc(getSlotItem(i))"
                  :alt="getSlotItem(i)?.alt || getSlotItem(i)?.name || ''"
                  class="recipe_item_img"
              />
              <span v-else-if="getItemText(getSlotItem(i))" class="recipe_item_text">
                {{ getItemText(getSlotItem(i)) }}
              </span>
              <span v-else class="recipe_item_text">
                {{ getSlotItem(i) }}
              </span>
              <span v-if="getItemCount(getSlotItem(i))" class="recipe_item_count">
                {{ getItemCount(getSlotItem(i)) }}
              </span>
            </template>
          </div>
        </div>

        <!-- Crafting Arrow -->
        <div class="recipe_arrow">
          <svg viewBox="0 0 24 16" width="28" height="20" class="arrow_svg">
            <!-- Arrow outline and body -->
            <path d="M2 6h12v4H2z" fill="#555555"/>
            <path d="M14 2l8 6-8 6V2z" fill="#555555"/>
            <!-- Highlight -->
            <path d="M2 6h12v1H2zm12-4l7 6-7 6V12l5-4-5-4z" fill="#ffffff"/>
            <path d="M2 9h12v1H2zm12 3l6-4-6-4v1l4 3-4 3z" fill="#373737"/>
          </svg>
        </div>

        <!-- Result Slot -->
        <div class="recipe_slot result_slot">
          <slot name="result" :item="currentResultIds" :count="currentResultCount">
            <BlockIds
                v-if="currentResultIds"
                :ids="currentResultIds"
                :forced-index="activeIndex"
            />
            <span v-if="currentResultCount > 1" class="recipe_item_count">
              {{ currentResultCount }}
            </span>
          </slot>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.recipe_wrapper {
  margin: 1.25rem 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.recipe_toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.5rem;
  max-width: 360px;
  width: 100%;
}

.recipe_tabs {
  display: flex;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.recipe_tab_btn {
  background: var(--vp-c-bg-mute);
  border: 1px solid var(--vp-c-divider);
  color: var(--vp-c-text-2);
  padding: 0.2rem 0.6rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
}

.recipe_tab_btn:hover {
  background: var(--vp-c-bg-soft);
  color: var(--vp-c-text-1);
}

.recipe_tab_btn.active {
  background: var(--vp-c-brand-soft);
  color: var(--vp-c-brand-1);
  border-color: var(--vp-c-brand-1);
  font-weight: 600;
}

.recipe_controls {
  display: flex;
  gap: 0.25rem;
  margin-left: auto;
}

.recipe_ctrl_btn {
  background: var(--vp-c-bg-mute);
  border: 1px solid var(--vp-c-divider);
  color: var(--vp-c-text-2);
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 500;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  transition: all 0.15s ease;
}

.recipe_ctrl_btn:hover {
  background: var(--vp-c-bg-soft);
  color: var(--vp-c-text-1);
}

.recipe_ctrl_btn.active {
  background: #f38baa;
  color: #1d1d21;
  border-color: #b94e6d;
  font-weight: 600;
}

.recipe_ctrl_btn.step_btn {
  padding: 0.2rem 0.4rem;
}

.recipe_body {
  display: flex;
  width: 100%;
  justify-content: center;
}

.recipe_container {
  --slot-size: 56px;
  --slot-gap: 6px;
  --container-padding: 12px;
  --border-width: 4px;
  --slot-border-width: 3px;
  --font-size: 20px;

  display: inline-flex;
  align-items: center;
  gap: 14px;
  padding: var(--container-padding);
  background-color: #c6c6c6;
  border-top: var(--border-width) solid #ffffff;
  border-left: var(--border-width) solid #ffffff;
  border-right: var(--border-width) solid #555555;
  border-bottom: var(--border-width) solid #555555;
  box-shadow: 0 0 0 2px #000000;
  border-radius: 5px;
  image-rendering: pixelated;
  user-select: none;
}

.recipe_crafting_grid {
  display: grid;
  grid-template-columns: repeat(3, var(--slot-size));
  grid-template-rows: repeat(3, var(--slot-size));
  gap: var(--slot-gap);
}

.recipe_arrow {
  display: flex;
  align-items: center;
  justify-content: center;
}

.arrow_svg {
  filter: drop-shadow(1px 1px 0px rgba(0, 0, 0, 0.2));
}

.recipe_slot {
  width: var(--slot-size);
  height: var(--slot-size);
  background-color: #8b8b8b;
  border-top: var(--slot-border-width) solid #373737;
  border-left: var(--slot-border-width) solid #373737;
  border-right: var(--slot-border-width) solid #ffffff;
  border-bottom: var(--slot-border-width) solid #ffffff;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  font-family: 'Minecraft', monospace, sans-serif;
  font-size: var(--font-size);
  font-weight: bold;
  color: #ffffff;
  text-shadow: 2px 2px #3f3f3f;
  user-select: none;
  transition: background-color 0.1s ease;
}

.recipe_slot.result_slot {
  width: calc(var(--slot-size) + 8px);
  height: calc(var(--slot-size) + 8px);
}

.recipe_slot:hover {
  background-color: #a0a0a0;
}

.recipe_container :deep(.block-ids) {
  width: 100%;
  height: 100%;
  border: none;
  background: transparent;
  margin: 0;
  border-radius: 0;
}

.recipe_item_img {
  width: 36px;
  height: 36px;
  object-fit: contain;
  image-rendering: pixelated;
  user-select: none;
  pointer-events: none;
}

.recipe_item_text {
  font-family: 'Minecraft', monospace, sans-serif;
  font-size: var(--font-size);
  font-weight: bold;
  color: #ffffff;
  text-shadow: 2px 2px #3f3f3f;
  user-select: none;
}

.recipe_item_count {
  position: absolute;
  right: 2px;
  bottom: 0px;
  font-family: 'Minecraft', monospace, sans-serif;
  font-size: 16px;
  font-weight: bold;
  color: #ffffff;
  text-shadow: 2px 2px #3f3f3f;
  line-height: 1;
  pointer-events: none;
  z-index: 10;
}
</style>