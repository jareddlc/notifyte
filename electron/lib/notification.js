var cache = require('memory-cache');
var config = require('./config');
var log = require('./logger');

var api = module.exports = {};
cache.put(config.cache.notifications, {});

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

api.cachePut = function cachePut(notification) {
  if(notification === null) {
    return log.info('Cannot insert null notification into cache');
  }

  var notifications = api.cacheGet();
  var key = notification.packageName + ':' + notification.name;
  if(notifications[key]) {
    log.info('Notification key already exists, adding');
    notifications[key].push(notification);
  }
  else {
    log.info('Notification key does not exist, creating');
    notifications[key] = [];
    notifications[key].push(notification);
  }

  cache.put(config.cache.notifications, notifications);
};

api.cacheGet = function cacheGet() {
  return cache.get(config.cache.notifications);
};
