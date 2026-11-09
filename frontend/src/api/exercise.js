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

export function addFavorite(data) {
  return request.post('/exercise/favorite/add', data)
}

export function deleteFavorite(id) {
  return request.delete(`/exercise/favorite/${id}`)
}

export function getFavorites(userId) {
  return request.get('/exercise/favorite/list', { params: { userId } })
}

export function getPracticeQuestions(params) {
  return request.get('/exercise/practice/list', { params })
}

export function submitPractice(data) {
  return request.post('/exercise/practice/submit', data)
}

export function getMistakes(userId) {
  return request.get('/exercise/mistake/list', { params: { userId } })
}

export function removeMistake(id) {
  return request.delete(`/exercise/mistake/${id}`)
}

export function getExerciseStats() {
  return request.get('/exercise/stats')
}
