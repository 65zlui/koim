import client from './client'

export const createGroup = (name, memberUids = []) =>
  client.post('/group/create', { name, memberUids })

export const joinGroup = (groupId) =>
  client.post('/group/join', { groupId })

export const groupInfo = (groupId) => client.get(`/group/${groupId}`)

export const groupMembers = (groupId) => client.get(`/group/${groupId}/members`)

export const updateGroup = (groupId, name) =>
  client.post('/group/update', { groupId, name })

export const leaveGroup = (groupId) =>
  client.post('/group/leave', { groupId })

export const kickMember = (groupId, targetUid) =>
  client.post('/group/kick', { groupId, targetUid })
