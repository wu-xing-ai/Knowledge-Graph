import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('../request', () => {
  const mockRequest = {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
  return { default: mockRequest }
})

import request from '../request'
import * as userApi from '../user'
import * as knowledgeApi from '../knowledge'
import * as graphApi from '../graph'
import * as qaApi from '../qa'
import * as exerciseApi from '../exercise'

describe('API modules', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('user api calls correct endpoints', () => {
    userApi.login({ username: 'a', password: 'b' })
    expect(request.post).toHaveBeenCalledWith('/user/login', { username: 'a', password: 'b' })

    userApi.register({ username: 'a' })
    expect(request.post).toHaveBeenCalledWith('/user/register', { username: 'a' })

    userApi.getUserInfo()
    expect(request.get).toHaveBeenCalledWith('/user/info')

    userApi.updateUserStatus(1, 0)
    expect(request.put).toHaveBeenCalledWith('/user/status', { userId: 1, status: 0 })
  })

  it('knowledge api calls correct endpoints', () => {
    knowledgeApi.getSubjects('单一学科')
    expect(request.get).toHaveBeenCalledWith('/knowledge/subject/list', { params: { type: '单一学科' } })

    knowledgeApi.addSubject({ name: 'X' })
    expect(request.post).toHaveBeenCalledWith('/knowledge/subject/add', { name: 'X' })

    knowledgeApi.deleteSubject(1)
    expect(request.delete).toHaveBeenCalledWith('/knowledge/subject/1')

    knowledgeApi.queryKnowledge({ keyword: 'test' })
    expect(request.get).toHaveBeenCalledWith('/knowledge/query', { params: { keyword: 'test' } })

    knowledgeApi.getCrossKnowledge()
    expect(request.get).toHaveBeenCalledWith('/knowledge/cross')
  })

  it('graph api calls correct endpoints', () => {
    graphApi.getGraphData({ keyword: 'k' })
    expect(request.get).toHaveBeenCalledWith('/graph/data', { params: { keyword: 'k' } })

    graphApi.expandNode(5)
    expect(request.get).toHaveBeenCalledWith('/graph/expand', { params: { nodeId: 5 } })

    graphApi.saveGraphConfig({ name: 'c' })
    expect(request.post).toHaveBeenCalledWith('/graph/config/save', { name: 'c' })
  })

  it('qa api calls correct endpoints', () => {
    qaApi.kbqa('问题')
    expect(request.post).toHaveBeenCalledWith('/qa/kbqa', { question: '问题' })

    qaApi.graphRag('问题')
    expect(request.post).toHaveBeenCalledWith('/qa/graphrag', { question: '问题' })

    qaApi.getTemplates()
    expect(request.get).toHaveBeenCalledWith('/qa/template/list')
  })

  it('exercise api calls correct endpoints', () => {
    exerciseApi.addExercise({ content: 'q' })
    expect(request.post).toHaveBeenCalledWith('/exercise/add', { content: 'q' })

    exerciseApi.getExercises()
    expect(request.get).toHaveBeenCalledWith('/exercise/list')

    exerciseApi.saveRecord({ userId: 1 })
    expect(request.post).toHaveBeenCalledWith('/exercise/record/save', { userId: 1 })

    exerciseApi.getNotes(1, '跨学科')
    expect(request.get).toHaveBeenCalledWith('/exercise/note/list', { params: { userId: 1, category: '跨学科' } })
  })
})
