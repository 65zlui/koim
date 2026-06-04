import client from './client'

export const sendMessage = (msgId, toUid, groupId, type, content) =>
  client.post('/message/send', { msgId, toUid, groupId, type, content })

export const syncMessages = (lastSeq) =>
  client.post('/message/sync', { lastSeq })

export const confirmMessages = (msgIds) =>
  client.post('/message/confirm', { msgIds })
