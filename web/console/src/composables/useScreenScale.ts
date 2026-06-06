import { onMounted, onUnmounted } from 'vue'

/**
 * 2K / 4K 自适应：根据视口宽度设置根字号缩放因子 --scale，全站 rem 等比放大。
 *  <1600 普通屏 → 1.00
 *  1600–2199    → 1.08
 *  2200–2999 2K → 1.15
 *  >=3000   4K  → 1.60
 */
function computeScale(w: number): number {
  if (w >= 3000) return 1.6
  if (w >= 2200) return 1.15
  if (w >= 1600) return 1.08
  return 1
}

export function useScreenScale() {
  const update = () => {
    const scale = computeScale(window.innerWidth)
    document.documentElement.style.setProperty('--scale', String(scale))
  }
  onMounted(() => {
    update()
    window.addEventListener('resize', update)
  })
  onUnmounted(() => window.removeEventListener('resize', update))
}
