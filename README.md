# ProyectoAndroid

Este proyecto es una aplicación de chat desarrollada en Java para Android. Utiliza Firebase Firestore para almacenar mensajes y usuarios, Firebase Storage para las imágenes, y FCM para notificaciones push.

A continuación se describen las **clases principales** y su función en la arquitectura de la app.


##  Clases Importantes

### **Actividades (Activities)**
 **MainActivity**  
  Pantalla principal con la lista de chats del usuario. Permite navegar a otras actividades (crear chat, editar perfil, ver chat).

**ChatActivity**  
  Pantalla de conversación entre usuarios. Muestra mensajes (texto/imágenes), permite enviar nuevos mensajes y adjuntar imágenes. Usa `MessageAdapter` para mostrar los mensajes.

**EditProfileActivity**  
  Permite editar nombre y foto de perfil. Cambios se guardan en Firestore y Firebase Storage.

**CrearChatActivity**  
  Interfaz para buscar usuarios y crear chats individuales o grupales. Filtra usuarios y permite seleccionar varios para crear grupos.

**ImageViewerActivity**  
  Muestra una imagen de chat en pantalla completa. Usa **PhotoView** para zoom/desplazamiento y **Picasso** para cargar la imagen desde URL.

**FCMDebugActivity**  
  Herramienta interna para probar el registro de token FCM y enviar notificaciones de prueba.



### **Adaptadores (Adapters)**
 **ChatAdapter**  
  Muestra la lista de chats en `MainActivity`. Incluye nombre, foto, último mensaje descifrado, hora y estado de lectura.

 **MessageAdapter**  
  Muestra mensajes en un chat. Descifra texto, muestra imágenes, gestiona encabezados de fecha y tipo de mensaje.

 **UserAdapter**  
  Lista de usuarios para crear chats/grupos. Permite seleccionar usuarios y muestra su foto y nombre.



### **Modelos de Datos**
 **User**  
  Usuario registrado: UID, nombre, email, foto, estado en línea y token FCM.

 **Chat**  
  Conversación: IDs de participantes, nombre, foto, último mensaje (cifrado si es texto), tipo y estado de lectura.

 **Message**  
  Mensaje en chat: contenido cifrado (texto), tipo (texto/imágen), remitente, fecha, estado de lectura y URL de imagen.



### **Utilidades**
 **CryptoUtils**  
  Clase para cifrado/descifrado de mensajes de texto usando AES y Base64.

 **ImageLoader**  
  Inicializa y gestiona la carga de imágenes con Picasso.



### **Firebase & Notificaciones**
 **FirestoreDataSource**  
  Encapsula operaciones en Firestore para usuarios, chats y mensajes.

 **FirebaseStorageDataSource**  
  Gestiona subida y obtención de URLs para imágenes en Firebase Storage.

 **NotificationManager**  
  Administra el token FCM y la lógica para enviar notificaciones push.



### **Librerías externas**
 **Picasso:** Carga eficiente de imágenes desde URLs.
 **PhotoView:** Permite zoom/desplazamiento en imágenes.
 **Firebase (Firestore, Storage, Auth, FCM):** Backend para almacenamiento, autenticación y notificaciones.



## Estructura típica de carpetas

 `com.example.proyectoandroid`  
  Actividades principales y adaptadores generales.
 `com.example.proyectoandroid.chat`  
  Clases específicas para chats y mensajes.
 `com.example.proyectoandroid.data.model`  
  Modelos de datos: User, Chat, Message.
 `com.example.proyectoandroid.utils`  
  Utilidades como CryptoUtils, ImageLoader.
 `com.example.proyectoandroid.data.remote`  
  Clases para interacción con Firestore y Storage.



##  Notas de seguridad

Los mensajes de texto se almacenan **cifrados** en Firestore, solo se descifran en el cliente.
Las imágenes se guardan en Firebase Storage y se acceden vía URL segura.



##  Visualización de imágenes en los chats

1. El adaptador de mensajes (`MessageAdapter`) carga miniaturas con **Picasso**.
2. Al hacer click, se abre `ImageViewerActivity` con la URL.
3. En la Activity, **PhotoView** permite zoom y desplazamiento, mientras Picasso carga la imagen.
