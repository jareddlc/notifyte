var package = require('../package.json');

var config = {};

config.name = 'Notifyte';
config.port = process.env.PORT || 7777;
config.service = {
  name: package.name,
  port: config.port,
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
  title: 'Notifyte',
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

config.ble = {
  limit: 512,
  mtu: 20
};
config.serviceUuid = ['fff0'];

config.characteristic = {
  uuid: 'fff1',
  properties: ['read', 'write', 'notify'], //'read', 'write', 'writeWithoutResponse', 'notify', 'indicate'
};

config.descriptor = {
  uuid: 'fff2',
  value: 'Notifyte descriptor value'
};

config.cache = {
  ble: {
    bluetooth: 'bluetooth',
    state: 'state',
    advertising: 'advertising',
    client: 'client',
    subscribed: 'subscribed'
  },
  notifications: 'notifications',
  sockets: 'sockets'
};

module.exports = config;
