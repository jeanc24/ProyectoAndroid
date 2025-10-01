const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

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

      const sendNotificationPromises = recipientIds.map(async (userId) => {
        try {
          const userSnapshot = await admin.firestore()
            .collection('users')
            .doc(userId)
            .get();

          if (!userSnapshot.exists) {
            console.log(`Usuario ${userId} no encontrado`);
            return null;
          }

          const userData = userSnapshot.data();
          const fcmToken = userData.fcmToken;

          if (!fcmToken || fcmToken === '') {
            console.log(`Usuario ${userId} no tiene token FCM`);
            return null;
          }

          const senderName = messageData.senderName || 'Alguien';
          let notificationTitle = `Mensaje de ${senderName}`;
          let notificationBody = '';

          if (messageData.messageType === 1) {
            notificationBody = '📷 Imagen';
          } else {
            notificationBody = messageData.content.length > 100
              ? messageData.content.substring(0, 97) + '...'
              : messageData.content;
          }

          const payload = {
            notification: {
              title: notificationTitle,
              body: notificationBody,
              icon: 'default',
              sound: 'default',
              clickAction: 'OPEN_CHAT_ACTIVITY'
            },
            data: {
              chatId: chatId,
              messageId: context.params.messageId,
              senderId: messageData.senderId
            }
          };

          return admin.messaging().sendToDevice(fcmToken, payload);
        } catch (err) {
          console.error(`Error enviando notificación a ${userId}:`, err);
          return null;
        }
      });

      await Promise.all(sendNotificationPromises);
      return null;

    } catch (error) {
      console.error('Error en la función sendMessageNotification:', error);
      return null;
    }
  });
