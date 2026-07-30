import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useGraphStore = defineStore('graph', () => {
  const highlightNodes = ref([])
  const graphData = ref({ nodes: [], edges: [] })

  function setHighlightNodes(ids) {
    highlightNodes.value = ids
  }

  function setGraphData(data) {
    graphData.value = data
  }

  return { highlightNodes, graphData, setHighlightNodes, setGraphData }
})
