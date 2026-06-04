import client from './client'

export const fetchConversations = () => client.get('/conversation/list')
export const markRead = (peerType, peerId) =>
  client.post('/conversation/read', { peerType, peerId })
