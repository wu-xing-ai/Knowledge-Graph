import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useGraphStore } from '../graph'

describe('graph store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('initializes with empty highlightNodes and graphData', () => {
    const store = useGraphStore()
    expect(store.highlightNodes).toEqual([])
    expect(store.graphData).toEqual({ nodes: [], edges: [] })
  })

  it('setHighlightNodes updates highlighted node ids', () => {
    const store = useGraphStore()
    store.setHighlightNodes([1, 2, 3])
    expect(store.highlightNodes).toEqual([1, 2, 3])
  })

  it('setGraphData updates graph data', () => {
    const store = useGraphStore()
    const data = { nodes: [{ id: 1 }], edges: [{ source: 1, target: 2 }] }
    store.setGraphData(data)
    expect(store.graphData).toEqual(data)
  })
})
