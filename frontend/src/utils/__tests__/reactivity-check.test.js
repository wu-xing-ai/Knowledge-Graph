import { describe, it, expect } from 'vitest'
import { ref, reactive, nextTick } from 'vue'
import { mount } from '@vue/test-utils'

describe('Vue 响应式：流式消息必须用 reactive 才能驱动界面刷新', () => {
  function mountList(messages) {
    const Comp = {
      template: '<div><span v-for="(m, i) in messages" :key="i" class="msg">{{ m.content }}</span></div>',
      setup() { return { messages } }
    }
    return mount(Comp)
  }

  it('直接改 push 进去的普通对象属性：界面不刷新（这正是之前“卡住”的原因）', async () => {
    const messages = ref([])
    const wrapper = mountList(messages)

    const assistantMsg = { content: '' }
    messages.value.push(assistantMsg)
    await nextTick()

    assistantMsg.content += '第一段' // 普通对象，不走响应式代理
    await nextTick()
    expect(wrapper.text()).toBe('') // 界面仍是空
  })

  it('用 reactive() 创建消息对象：逐段更新能实时渲染（流式修复）', async () => {
    const messages = ref([])
    const wrapper = mountList(messages)

    const assistantMsg = reactive({ content: '' })
    messages.value.push(assistantMsg)
    await nextTick()

    assistantMsg.content += '第一段'
    await nextTick()
    expect(wrapper.text()).toBe('第一段')

    assistantMsg.content += '，第二段'
    await nextTick()
    expect(wrapper.text()).toBe('第一段，第二段')
  })
})
