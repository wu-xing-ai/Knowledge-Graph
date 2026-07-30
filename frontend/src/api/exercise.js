import request from './request'

export function addExercise(data) {
  return request.post('/exercise/add', data)
}

export function updateExercise(data) {
  return request.put('/exercise/update', data)
}

export function deleteExercise(id) {
  return request.delete(`/exercise/${id}`)
}

export function getExercises() {
  return request.get('/exercise/list')
}

export function getExercise(id) {
  return request.get(`/exercise/${id}`)
}

export function saveRecord(data) {
  return request.post('/exercise/record/save', data)
}

export function getRecords(userId) {
  return request.get('/exercise/record/list', { params: { userId } })
}

export function addNote(data) {
  return request.post('/exercise/note/add', data)
}

export function updateNote(data) {
  return request.put('/exercise/note/update', data)
}

export function deleteNote(id) {
  return request.delete(`/exercise/note/${id}`)
}

export function getNotes(userId, category) {
  return request.get('/exercise/note/list', { params: { userId, category } })
}
