var package = require('../package.json');

var config = {};

config.name = 'Notifyte';
config.port = process.env.PORT || 7777;
config.service = {
  name: package.name,
  version: package.version,
  description: package.description
};

config.logging = {
  enabled: process.env.LOG || true,
  error: true,
  info: true,
  debug: true
};

config.window = {
  title: config.name,
  width: 800,
  height: 600,
  minWidth: 600,
  minHeight: 400,
  center: true,
  movable: true,
  minimizable: true,
  maximizable: true,
  resizable: true,
  closable: true,
  fullscreen: false,
  fullscreenable: true
};

//config.serviceUuid = ['31419fef-b24e-4ea8-a280-86572b6c0a7d'];

config.ble = {
  limit: 512,
  mtu: 20
};
config.serviceUuid = ['fff0'];


config.characteristic = {
  uuid: 'fff1',
  //uuid: 'ff27961f-4e7d-4fde-ad0a-91e7411635bc',
  //uuid: '0000XXXX-0000-1000-8000-00805f9b34fb',
  properties: ['read', 'write', 'notify'], //'read', 'write', 'writeWithoutResponse', 'notify', 'indicate'
};

config.descriptor = {
  //uuid: '917e4d97-3827-4e6e-b776-4f554d468476',
  uuid: 'fff2',
  value: 'Notifyte descriptor value'
};

config.cache = {
  ble: {
    state: 'state',
    advertising: 'advertising',
    client: 'client'
  },
  notifications: 'notifications'
};

module.exports = config;
