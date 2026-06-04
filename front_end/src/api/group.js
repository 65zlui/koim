import client from './client'

export const createGroup = (name, memberUids = []) =>
  client.post('/group/create', { name, memberUids })

export const joinGroup = (groupId) =>
  client.post('/group/join', { groupId })

export const groupInfo = (groupId) => client.get(`/group/${groupId}`)
