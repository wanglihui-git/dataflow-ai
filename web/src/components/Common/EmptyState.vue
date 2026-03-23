<template>
  <div class="empty-state">
    <div class="empty-state__illustration">
      <slot name="illustration">
        <svg viewBox="0 0 200 200" class="empty-state__svg">
          <circle cx="100" cy="100" r="80" fill="#f1f3f5" />
          <rect x="60" y="70" width="80" height="60" rx="4" fill="#dee2e6" />
          <line x1="70" y1="90" x2="130" y2="90" stroke="#adb5bd" stroke-width="4" stroke-linecap="round" />
          <line x1="70" y1="110" x2="110" y2="110" stroke="#adb5bd" stroke-width="4" stroke-linecap="round" />
          <circle v-if="type === 'data'" cx="100" cy="50" r="20" fill="#e9ecef" />
          <path v-if="type === 'search'" d="M80 80 L120 120 M120 80 L80 120" stroke="#adb5bd" stroke-width="4" stroke-linecap="round" />
        </svg>
      </slot>
    </div>
    <h3 class="empty-state__title">{{ title }}</h3>
    <p v-if="description" class="empty-state__description">{{ description }}</p>
    <div v-if="$slots.actions" class="empty-state__actions">
      <slot name="actions"></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  title?: string
  description?: string
  type?: 'default' | 'data' | 'search' | 'error'
}

withDefaults(defineProps<Props>(), {
  title: '暂无数据',
  description: '',
  type: 'default'
})
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.empty-state__illustration {
  width: 160px;
  height: 160px;
  margin-bottom: 24px;
  opacity: 0.8;
}

.empty-state__svg {
  width: 100%;
  height: 100%;
}

.empty-state__title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: #343a40;
}

.empty-state__description {
  margin: 0 0 24px;
  font-size: 14px;
  color: #868e96;
  max-width: 400px;
  line-height: 1.6;
}

.empty-state__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
}
</style>