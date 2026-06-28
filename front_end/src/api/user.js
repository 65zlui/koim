import client from './client'

export const register = (username, password, nickname) =>
  client.post('/user/register', { username, password, nickname })

export const login = (username, password) =>
  client.post('/user/login', { username, password })

export const getProfile = () => client.get('/user/profile')

export const updateProfile = (data) => client.post('/user/profile', data)

export const changePassword = (oldPassword, newPassword) =>
  client.post('/user/password', { oldPassword, newPassword })

export const searchUsers = (q) => client.get(`/user/search?q=${encodeURIComponent(q)}`)
