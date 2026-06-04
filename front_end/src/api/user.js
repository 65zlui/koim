import client from './client'

export const register = (username, password, nickname) =>
  client.post('/user/register', { username, password, nickname })

export const login = (username, password) =>
  client.post('/user/login', { username, password })
