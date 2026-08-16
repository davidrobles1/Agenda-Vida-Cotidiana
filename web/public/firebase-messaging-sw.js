importScripts('https://www.gstatic.com/firebasejs/10.14.1/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.14.1/firebase-messaging-compat.js');

// WEB-005: background message handling needs its own service-worker context,
// which can't read Vite's import.meta.env — this config is the same public
// Firebase Web client config as web/.env (not a secret, see .env.example).
firebase.initializeApp({
  apiKey: 'AIzaSyCMep5W1WbCj1Z-en_HPRwjaYNGnvtOTlg',
  authDomain: 'vida-cotidiana-6da30.firebaseapp.com',
  projectId: 'vida-cotidiana-6da30',
  storageBucket: 'vida-cotidiana-6da30.firebasestorage.app',
  messagingSenderId: '810187433686',
  appId: '1:810187433686:web:8e54e76b5f7624d59a0b77',
});

firebase.messaging();
