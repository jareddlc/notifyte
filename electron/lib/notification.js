var cache = require('memory-cache');
var config = require('./config');
var log = require('./logger');

var api = module.exports = {};

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
