const functions = require('firebase-functions');
const admin = require('firebase-admin');
const crypto = require('crypto');
admin.initializeApp();

function getAesKey() {
  const cfg = (functions.config && functions.config().crypto && functions.config().crypto.key) || null;
  return cfg || process.env.CHAT_AES_KEY || 'MySuperSecretKey'; // 16 bytes
}

function decryptAESBase64_ECB_PKCS5(encryptedBase64, keyStr) {
  const key = Buffer.from(keyStr, 'utf8');
  const decipher = crypto.createDecipheriv('aes-128-ecb', key, null);
  decipher.setAutoPadding(true);
  const cipherBytes = Buffer.from(encryptedBase64, 'base64');
  const decrypted = Buffer.concat([decipher.update(cipherBytes), decipher.final()]);
  return decrypted.toString('utf8');
}

function tryDecryptContent(content) {
  if (!content || typeof content !== 'string') return content;
  const key = getAesKey();
  // Intento 1: descifrar una vez
  try {
    let plain = decryptAESBase64_ECB_PKCS5(content, key);
    // Intento 2: compatibilidad por si viene doble-cifrado
    try {
      plain = decryptAESBase64_ECB_PKCS5(plain, key);
    } catch (_) {
      // una pasada fue suficiente
    }
    return plain;
  } catch (e) {
    // Fallback: devolver tal cual si no es Base64 o no está cifrado con nuestra clave
    return content;
  }
}

exports.sendMessageNotification = functions.firestore
  .document('chats/{chatId}/messages/{messageId}')
  .onCreate(async (snapshot, context) => {
    try {
      const messageData = snapshot.data();
      const chatId = context.params.chatId;

      if (!messageData || !messageData.senderId || !chatId) {
        console.log('Datos del mensaje insuficientes para enviar notificación');
        return null;
      }

      const chatSnapshot = await admin.firestore()
        .collection('chats')
        .doc(chatId)
        .get();

      if (!chatSnapshot.exists) {
        console.log(`Chat ${chatId} no encontrado`);
        return null;
      }

      const chatData = chatSnapshot.data();
      if (!chatData.participantIds || chatData.participantIds.length === 0) {
        console.log(`Chat ${chatId} no tiene participantes`);
        return null;
      }

      const recipientIds = chatData.participantIds.filter(
        id => id !== messageData.senderId
      );

      if (recipientIds.length === 0) {
        console.log('No hay destinatarios para enviar notificación');
        return null;
      }

      // Obtener tokens FCM válidos de los destinatarios (soportar >10 con 'in' por lotes)
      const chunk = (arr, size) => arr.reduce((acc, _, i) => (i % size ? acc : [...acc, arr.slice(i, i + size)]), []);
      const recipientChunks = chunk(recipientIds, 10);

      const tokens = [];
      const tokenToUserId = {};

      for (const ids of recipientChunks) {
        const usersSnap = await admin.firestore()
          .collection('users')
          .where(admin.firestore.FieldPath.documentId(), 'in', ids)
          .get();

        usersSnap.forEach(doc => {
          const data = doc.data() || {};
          const token = data.fcmToken;
          if (token && typeof token === 'string' && token.trim() !== '') {
            tokens.push(token);
            tokenToUserId[token] = doc.id;
          }
        });
      }

      if (tokens.length === 0) {
        console.log('Destinatarios sin token FCM; no se enviará notificación');
        return null;
      }

      const senderName = messageData.senderName || 'Alguien';
      const isImage = messageData.messageType === 1; // 1 == imagen según tu modelo
      const notificationTitle = `Mensaje de ${senderName}`;

      let notificationBody;
      if (isImage) {
        notificationBody = '📷 Imagen';
      } else {
        const rawContent = (messageData.content || '').toString();
        const plain = tryDecryptContent(rawContent);
        // recortar a 100 chars para la notificación
        notificationBody = plain.length > 100 ? plain.slice(0, 100) + '…' : plain;
      }

      // Mensaje FCM v1 con configuración Android para canal y prioridad
      const baseMessage = {
        notification: {
          title: notificationTitle,
          body: notificationBody,
        },
        data: {
          chatId: chatId,
          messageId: context.params.messageId,
          senderId: messageData.senderId,
        },
        android: {
          priority: 'high',
          notification: {
            channelId: 'chat_messages',
            clickAction: 'OPEN_CHAT_ACTIVITY',
            sound: 'default',
          },
        },
        // Opcional para iOS si en el futuro usan iPhone
        apns: {
          payload: {
            aps: {
              sound: 'default',
              category: 'OPEN_CHAT_ACTIVITY',
            },
          },
        },
      };

      // Enviar a múltiples tokens con v1
      const multicast = {
        tokens,
        ...baseMessage,
      };

      const response = await admin.messaging().sendEachForMulticast(multicast);

      // Limpiar tokens inválidos y registrar errores
      const cleanupPromises = [];
      response.responses.forEach((resp, idx) => {
        if (!resp.success) {
          const token = tokens[idx];
          const err = resp.error;
          console.error(`Error enviando a token ${token}:`, err);

          if (err && err.code === 'messaging/registration-token-not-registered') {
            const userId = tokenToUserId[token];
            if (userId) {
              cleanupPromises.push(
                admin.firestore().collection('users').doc(userId).update({ fcmToken: '' })
                  .then(() => console.log(`Token inválido eliminado para usuario ${userId}`))
                  .catch(e => console.error('Error limpiando token inválido', e))
              );
            }
          }
        }
      });

      await Promise.all(cleanupPromises);
      console.log(`Notificaciones intentadas: ${tokens.length}, éxito: ${response.successCount}, fallo: ${response.failureCount}`);
      return null;

    } catch (error) {
      console.error('Error en la función sendMessageNotification:', error);
      return null;
    }
  });
