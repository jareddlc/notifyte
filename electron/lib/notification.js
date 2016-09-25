var config = require('./config');
var log = require('./logger');

var api = module.exports = {};

api.init = function init(data, callback) {
  if(!data || !data.cache) {
    throw new Error('Error: cannot initialize without cache');
  }
  if(!data.ble) {
    throw new Error('Error: cannot initialize without ble');
  }
  api.ble = data.ble;
  api.cache = data.cache;

  if(callback) {
    callback(null);
  }
};

api.isNotification = function isNotification(json) {
  if(!json.appName) {
    return false;
  }
  if(!json.packageName) {
    return false;
  }

  return true;
};

api.parse = function parse(msg) {
  var notification = null;

  try {
    notification = JSON.parse(msg);
    if(api.isNotification(notification)) {
      return notification;
    }
    else {
      log.info('Notification: not valid notification');
      return notification;
    }
  }
  catch(e) {
    log.error('Notification: could not parse notification');
    return notification;
  }
};

api.put = function put(notification) {
  if(notification === null) {
    return log.info('Notification: cannot insert null notification into cache');
  }

  var notifications = api.get();

  if(notification.key && notifications[notification.key]) {
    notifications[notification.key].push(notification);
    api.cache.put(config.cache.notifications, notifications);
  }
  else {
    var key = new Buffer(notification.packageName + ':' + notification.name).toString('base64');
    notification.key = key;
    if(notifications[key]) {
      log.info('Notification: key already exists, adding');
      notifications[key].push(notification);
    }
    else {
      log.info('Notification: key does not exist, creating');
      notifications[key] = [];
      notifications[key].push(notification);
    }

    api.cache.put(config.cache.notifications, notifications);
  }
};

api.get = function get() {
  return api.cache.get(config.cache.notifications);
};

api.sendNotification = function sendNotification(notification, callback) {
  log.info('Notification: sendNotification');
  if(api.isNotification(notification)) {
    api.put(notification);
    api.ble.sendNotification(notification);
  }
  else {
    if(callback) {
      callback(new Error('Error: trying to send a non notification'));
    }
  }
};

api.deleteNotification = function deleteNotification(key, callback) {
  log.info('Notification: deleteNotification', key.key);
  var notifications = api.get();
  if(!key || !key.key) {
    return callback(new Error('Error: no notification provided'));
  }

  if(notifications[key.key]) {
    delete notifications[key.key];
    callback(null);
  }
  else {
    log.error('Notification: could not delete notification', key.key);
    callback(new Error('Error: deleting notification not found'));
  }
};
